'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { taskSchema, type GradeValues, type TaskValues } from '@/lib/schemas';
import { formatDate, formatDateTime, titleCase } from '@/lib/format';
import type { Activity, Assessment, Certificate, Enrollment, PageResponse, ParticipantRow, Task } from '@/lib/types';
import { RequireAuth } from '@/components/RequireAuth';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';

function TaskSubmission({ task, onDone }: { task: Task; onDone: () => void }) {
  const [text, setText] = useState('');
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);

  const submit = useMutation({
    mutationFn: () =>
      api<Assessment>(`/api/v1/tasks/${task.taskId}/submit`, {
        method: 'POST',
        body: {
          submissionText: task.taskType === 'ASSIGNMENT' ? text : null,
          answers: task.taskType === 'QUIZ' ? Object.values(answers).map((value) => Number(value) || 0) : null,
        },
      }),
    onSuccess: () => {
      setError(null);
      onDone();
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : 'Could not submit this task.'),
  });

  const questions = task.questions ?? [];

  return (
    <div className="mt-3 rounded-xl border border-slate-200 p-3 dark:border-slate-700">
      {task.instructions && <p className="text-sm text-slate-700 dark:text-slate-300">{task.instructions}</p>}
      {task.taskType === 'QUIZ' && questions.length > 0 && (
        <div className="mt-3 space-y-2">
          {questions.map((question, index) => {
            const key = String(question.id ?? question.question ?? index);
            const label = String(question.question ?? question.text ?? `Question ${index + 1}`);
            return (
              <Field key={key} label={label} htmlFor={`q-${task.taskId}-${index}`}>
                <Input
                  id={`q-${task.taskId}-${index}`}
                  value={answers[key] ?? ''}
                  onChange={(event) => setAnswers((current) => ({ ...current, [key]: event.target.value }))}
                />
              </Field>
            );
          })}
        </div>
      )}
      {task.taskType === 'ASSIGNMENT' && (
        <Field label="Your submission" htmlFor={`submission-${task.taskId}`} className="mt-3">
          <Textarea
            id={`submission-${task.taskId}`}
            rows={3}
            value={text}
            onChange={(event) => setText(event.target.value)}
            placeholder="Write your answer…"
          />
        </Field>
      )}
      {error && <p className="mt-2 text-xs text-rose-600">{error}</p>}
      <div className="mt-3">
        <Button
          size="sm"
          loading={submit.isPending}
          disabled={task.taskType === 'ASSIGNMENT' ? !text.trim() : questions.length > 0 && Object.keys(answers).length === 0}
          onClick={() => submit.mutate()}
        >
          Submit task
        </Button>
      </div>
    </div>
  );
}

function ParticipantAssessments() {
  const queryClient = useQueryClient();
  const [openTask, setOpenTask] = useState<number | null>(null);

  const assessments = useQuery({
    queryKey: ['assessments', 'mine'],
    queryFn: () => api<Assessment[]>('/api/v1/assessments/me'),
  });

  const certificates = useQuery({
    queryKey: ['certificates', 'mine'],
    queryFn: () => api<Certificate[]>('/api/v1/certificates/me'),
  });

  const tasks = useQuery({
    queryKey: ['tasks', 'for-me'],
    queryFn: async () => {
      const enrollments = await api<Enrollment[]>('/api/v1/enrollments/me');
      const active = enrollments.filter((item) => item.status === 'ENROLLED' || item.status === 'COMPLETED');
      const lists = await Promise.all(
        active.map(async (enrollment) => {
          const items = await api<Task[]>(`/api/v1/activities/${enrollment.activityId}/tasks`);
          return items
            .filter((task) => task.published)
            .map((task) => ({ task, activityTitle: enrollment.activityTitle }));
        }),
      );
      return lists.flat();
    },
  });

  const submittedTaskIds = new Set((assessments.data ?? []).map((item) => item.taskId));
  const available = (tasks.data ?? []).filter((item) => !submittedTaskIds.has(item.task.taskId));

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['assessments', 'mine'] });
    void queryClient.invalidateQueries({ queryKey: ['tasks', 'for-me'] });
  }

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <section className="space-y-4 lg:col-span-2">
        <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">Tasks to submit</h2>
        {tasks.isLoading ? (
          <SpinnerRow label="Loading tasks…" />
        ) : available.length === 0 ? (
          <EmptyState title="No open tasks" description="Tasks appear here once you are enrolled and a facilitator publishes them." />
        ) : (
          <div className="space-y-3">
            {available.map(({ task, activityTitle }) => (
              <Card key={task.taskId} className="!p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">{task.title}</p>
                    <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                      {activityTitle} · {titleCase(task.taskType)} · {task.maxScore} marks
                      {task.dueDate ? ` · due ${formatDate(task.dueDate)}` : ''}
                    </p>
                  </div>
                  <Button size="sm" variant="secondary" onClick={() => setOpenTask((current) => (current === task.taskId ? null : task.taskId))}>
                    {openTask === task.taskId ? 'Close' : 'Open'}
                  </Button>
                </div>
                {openTask === task.taskId && <TaskSubmission task={task} onDone={refresh} />}
              </Card>
            ))}
          </div>
        )}

        <h2 className="pt-2 text-base font-semibold text-slate-900 dark:text-slate-100">My results</h2>
        {assessments.isLoading ? (
          <SpinnerRow label="Loading results…" />
        ) : assessments.isError ? (
          <ErrorState
            message={assessments.error instanceof ApiError ? assessments.error.message : 'Could not load your results.'}
            onRetry={() => assessments.refetch()}
          />
        ) : (assessments.data ?? []).length === 0 ? (
          <EmptyState title="No submissions yet" description="Once you submit a task, your score and feedback appear here." />
        ) : (
          <div className="space-y-3">
            {(assessments.data ?? []).map((assessment) => (
              <Card key={assessment.assessmentId} className="!p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">{assessment.taskTitle}</p>
                    <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                      {assessment.status === 'GRADED'
                        ? `${assessment.score}/${assessment.maxScore} · ${Math.round(assessment.percentage)}%`
                        : 'Awaiting grading'}
                      {assessment.gradedByName ? ` · by ${assessment.gradedByName}` : ''}
                    </p>
                  </div>
                  <Badge tone={assessment.status}>{titleCase(assessment.status)}</Badge>
                </div>
                {assessment.feedback && (
                  <p className="mt-2 whitespace-pre-line text-sm text-slate-700 dark:text-slate-300">{assessment.feedback}</p>
                )}
              </Card>
            ))}
          </div>
        )}
      </section>

      <aside className="space-y-4">
        <Card>
          <CardTitle>My certificates</CardTitle>
          <CardDescription>Verifiable by anyone with the certificate number.</CardDescription>
          {(certificates.data ?? []).length === 0 ? (
            <p className="mt-3 text-sm text-slate-600 dark:text-slate-300">
              No certificates yet. Pass the assessment and attendance thresholds for an activity.
            </p>
          ) : (
            <ul className="mt-3 space-y-2">
              {(certificates.data ?? []).map((certificate) => (
                <li key={certificate.certificateId} className="flex items-center justify-between gap-2">
                  <div>
                    <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{certificate.activityTitle}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {certificate.certificateNumber} · {formatDate(certificate.issueDate)}
                    </p>
                  </div>
                  <LinkButton href={`/certificates/${certificate.certificateNumber}`} size="sm" variant="secondary">
                    Verify
                  </LinkButton>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </aside>
    </div>
  );
}

function StaffAssessments() {
  const queryClient = useQueryClient();
  const [activityId, setActivityId] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [scores, setScores] = useState<Record<number, { score: string; feedback: string }>>({});

  const activities = useQuery({
    queryKey: ['facilitator-activities'],
    queryFn: () => api<PageResponse<Activity>>('/api/v1/facilitator/activities?size=100'),
  });

  const selectedId = activityId || String(activities.data?.items[0]?.activityId ?? '');

  const tasks = useQuery({
    queryKey: ['activity', selectedId, 'tasks'],
    queryFn: () => api<Task[]>(`/api/v1/activities/${selectedId}/tasks`),
    enabled: !!selectedId,
  });

  const submissions = useQuery({
    queryKey: ['activity', selectedId, 'submissions'],
    queryFn: async () => {
      const list = await api<Task[]>(`/api/v1/activities/${selectedId}/tasks`);
      const lists = await Promise.all(
        list.map((task) => api<Assessment[]>(`/api/v1/tasks/${task.taskId}/submissions`)),
      );
      return lists.flat();
    },
    enabled: !!selectedId,
  });

  const roster = useQuery({
    queryKey: ['activity', selectedId, 'roster'],
    queryFn: () => api<ParticipantRow[]>(`/api/v1/activities/${selectedId}/participants`),
    enabled: !!selectedId,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<TaskValues>({
    resolver: zodResolver(taskSchema),
    defaultValues: { title: '', taskType: 'ASSIGNMENT', instructions: '', maxScore: 100, published: true },
  });

  const createTask = useMutation({
    mutationFn: (values: TaskValues) =>
      api<Task>(`/api/v1/activities/${selectedId}/tasks`, {
        method: 'POST',
        body: {
          title: values.title,
          taskType: values.taskType,
          instructions: values.instructions || null,
          maxScore: values.maxScore,
          dueDate: values.dueDate ? new Date(values.dueDate).toISOString() : null,
          published: values.published ?? true,
        },
      }),
    onSuccess: () => {
      reset();
      setNotice('Task created.');
      void queryClient.invalidateQueries({ queryKey: ['activity', selectedId, 'tasks'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not create the task.'),
  });

  const grade = useMutation({
    mutationFn: ({ assessmentId, values }: { assessmentId: number; values: GradeValues }) =>
      api<Assessment>(`/api/v1/assessments/${assessmentId}/grade`, { method: 'PUT', body: values }),
    onSuccess: () => {
      setNotice('Assessment graded.');
      void queryClient.invalidateQueries({ queryKey: ['activity', selectedId, 'submissions'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not grade the assessment.'),
  });

  const issue = useMutation({
    mutationFn: (youthId: number) =>
      api<Certificate>(`/api/v1/activities/${selectedId}/certificates/${youthId}`, { method: 'POST' }),
    onSuccess: (certificate) => {
      setNotice(`Certificate ${certificate.certificateNumber} issued.`);
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not issue the certificate.'),
  });

  return (
    <div className="space-y-4">
      {notice && <Alert tone={notice.toLowerCase().includes('could not') ? 'error' : 'success'}>{notice}</Alert>}

      <Card>
        <CardTitle>Choose an activity</CardTitle>
        <div className="mt-3 max-w-md">
          <Field label="Activity" htmlFor="staff-activity">
            <Select id="staff-activity" value={selectedId} onChange={(event) => setActivityId(event.target.value)}>
              {(activities.data?.items ?? []).map((activity) => (
                <option key={activity.activityId} value={activity.activityId}>
                  {activity.title} ({titleCase(activity.status)})
                </option>
              ))}
            </Select>
          </Field>
        </div>
        {activities.isLoading && <SpinnerRow label="Loading activities…" />}
        {!activities.isLoading && (activities.data?.items ?? []).length === 0 && (
          <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">No activities assigned to you yet.</p>
        )}
      </Card>

      {selectedId && (
        <div className="grid gap-6 lg:grid-cols-3">
          <section className="space-y-4 lg:col-span-2">
            <Card>
              <CardTitle>Submissions ({submissions.data?.length ?? 0})</CardTitle>
              <CardDescription>Grade each submission out of the task maximum score.</CardDescription>
              {submissions.isLoading ? (
                <SpinnerRow label="Loading submissions…" />
              ) : (submissions.data ?? []).length === 0 ? (
                <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No submissions yet.</p>
              ) : (
                <ul className="mt-3 space-y-3">
                  {(submissions.data ?? []).map((submission) => (
                    <li key={submission.assessmentId} className="rounded-lg border border-slate-200 p-3 dark:border-slate-700">
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                            {submission.youthName} · {submission.taskTitle}
                          </p>
                          <p className="text-xs text-slate-500 dark:text-slate-400">
                            Submitted {formatDateTime(submission.assessmentDate)} · max {submission.maxScore}
                          </p>
                        </div>
                        <Badge tone={submission.status}>{titleCase(submission.status)}</Badge>
                      </div>
                      {submission.submissionText && (
                        <p className="mt-2 whitespace-pre-line rounded bg-slate-50 p-2 text-sm text-slate-700 dark:bg-slate-800/60 dark:text-slate-300">
                          {submission.submissionText}
                        </p>
                      )}
                      {submission.status === 'GRADED' ? (
                        <p className="mt-2 text-sm text-slate-700 dark:text-slate-300">
                          Score {submission.score}/{submission.maxScore} ({Math.round(submission.percentage)}%)
                          {submission.feedback ? ` · ${submission.feedback}` : ''}
                        </p>
                      ) : (
                        <form
                          className="mt-3 grid gap-2 sm:grid-cols-4"
                          onSubmit={(event) => {
                            event.preventDefault();
                            const entry = scores[submission.assessmentId] ?? { score: '', feedback: '' };
                            grade.mutate({
                              assessmentId: submission.assessmentId,
                              values: { score: Number(entry.score), feedback: entry.feedback || undefined },
                            });
                          }}
                        >
                          <Input
                            aria-label={`Score for ${submission.youthName}`}
                            type="number"
                            min={0}
                            max={submission.maxScore}
                            placeholder="Score"
                            value={scores[submission.assessmentId]?.score ?? ''}
                            onChange={(event) =>
                              setScores((current) => ({
                                ...current,
                                [submission.assessmentId]: {
                                  score: event.target.value,
                                  feedback: current[submission.assessmentId]?.feedback ?? '',
                                },
                              }))
                            }
                          />
                          <Input
                            aria-label={`Feedback for ${submission.youthName}`}
                            placeholder="Feedback"
                            className="sm:col-span-2"
                            value={scores[submission.assessmentId]?.feedback ?? ''}
                            onChange={(event) =>
                              setScores((current) => ({
                                ...current,
                                [submission.assessmentId]: {
                                  score: current[submission.assessmentId]?.score ?? '',
                                  feedback: event.target.value,
                                },
                              }))
                            }
                          />
                          <Button
                            type="submit"
                            size="sm"
                            loading={grade.isPending}
                            disabled={scores[submission.assessmentId]?.score === undefined || scores[submission.assessmentId]?.score === ''}
                          >
                            Grade
                          </Button>
                        </form>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card>
              <CardTitle>Create a task</CardTitle>
              <form className="mt-3 space-y-3" onSubmit={handleSubmit((values) => createTask.mutate(values))} noValidate>
                <Field label="Title" htmlFor="task-title" error={errors.title?.message} required>
                  <Input id="task-title" invalid={!!errors.title} {...register('title')} />
                </Field>
                <div className="grid gap-3 sm:grid-cols-3">
                  <Field label="Type" htmlFor="task-type" error={errors.taskType?.message} required>
                    <Select id="task-type" {...register('taskType')}>
                      <option value="ASSIGNMENT">Assignment</option>
                      <option value="QUIZ">Quiz</option>
                    </Select>
                  </Field>
                  <Field label="Max score" htmlFor="task-max" error={errors.maxScore?.message}>
                    <Input id="task-max" type="number" min={1} {...register('maxScore')} />
                  </Field>
                  <Field label="Due date" htmlFor="task-due" error={errors.dueDate?.message}>
                    <Input id="task-due" type="datetime-local" {...register('dueDate')} />
                  </Field>
                </div>
                <Field label="Instructions" htmlFor="task-instructions" error={errors.instructions?.message}>
                  <Textarea id="task-instructions" rows={2} {...register('instructions')} />
                </Field>
                <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
                  <input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-brand-600" {...register('published')} />
                  Publish immediately
                </label>
                <Button type="submit" size="sm" loading={isSubmitting || createTask.isPending}>
                  Create task
                </Button>
              </form>
            </Card>
          </section>

          <aside className="space-y-4">
            <Card>
              <CardTitle>Issue certificates</CardTitle>
              <CardDescription>Participants must pass the assessment and attendance thresholds first.</CardDescription>
              {(roster.data ?? []).length === 0 ? (
                <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No enrolled participants.</p>
              ) : (
                <ul className="mt-3 space-y-2">
                  {(roster.data ?? []).map((participant) => (
                    <li key={participant.youthId} className="flex items-center justify-between gap-2">
                      <div>
                        <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{participant.name}</p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">{titleCase(participant.status)}</p>
                      </div>
                      <Button size="sm" variant="secondary" loading={issue.isPending} onClick={() => issue.mutate(participant.youthId)}>
                        Issue
                      </Button>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card>
              <CardTitle>Tasks</CardTitle>
              <ul className="mt-3 space-y-2 text-sm">
                {(tasks.data ?? []).map((task) => (
                  <li key={task.taskId} className="flex items-center justify-between gap-2">
                    <span className="text-slate-700 dark:text-slate-200">{task.title}</span>
                    <Badge tone={task.published ? 'PUBLISHED' : 'DRAFT'}>{task.published ? 'Published' : 'Draft'}</Badge>
                  </li>
                ))}
              </ul>
            </Card>
          </aside>
        </div>
      )}
    </div>
  );
}

function AssessmentsContent() {
  const { user } = useAuth();

  return (
    <>
      <PageHeader
        title="Assessments & certificates"
        description={
          user?.role === 'FACILITATOR' || user?.role === 'ADMIN'
            ? 'Create tasks, grade submissions and issue certificates for your activities.'
            : 'Submit tasks, see your scores and feedback, and download your certificates.'
        }
        actions={
          <LinkButton href="/dashboard" size="sm" variant="secondary">
            Dashboard
          </LinkButton>
        }
      />
      {user?.role === 'PARTICIPANT' ? <ParticipantAssessments /> : user?.role === 'FACILITATOR' || user?.role === 'ADMIN' ? <StaffAssessments /> : (
        <EmptyState title="No assessments for this role" description="Assessments are available to participants, facilitators and administrators." />
      )}
    </>
  );
}

export default function AssessmentsPage() {
  return (
    <RequireAuth>
      <AssessmentsContent />
    </RequireAuth>
  );
}

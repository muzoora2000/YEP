'use client';

import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { teamSchema, type TeamValues } from '@/lib/schemas';
import { formatDate, formatDateTime, titleCase } from '@/lib/format';
import type {
  Activity,
  Enrollment,
  Material,
  ParticipantRow,
  Post,
  Progress,
  Session,
  Team,
  Thread,
} from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, Breadcrumbs, EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';

function ThreadPanel({ thread }: { thread: Thread }) {
  const queryClient = useQueryClient();
  const [content, setContent] = useState('');
  const [error, setError] = useState<string | null>(null);

  const posts = useQuery({
    queryKey: ['thread', thread.threadId, 'posts'],
    queryFn: () => api<Post[]>(`/api/v1/threads/${thread.threadId}/posts`),
  });

  const addPost = useMutation({
    mutationFn: () => api<Post>(`/api/v1/threads/${thread.threadId}/posts`, { method: 'POST', body: { content } }),
    onSuccess: () => {
      setContent('');
      setError(null);
      void queryClient.invalidateQueries({ queryKey: ['thread', thread.threadId, 'posts'] });
      void queryClient.invalidateQueries({ queryKey: ['threads', thread.activityId] });
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : 'Could not post your reply.'),
  });

  return (
    <div className="mt-3 rounded-xl border border-slate-200 p-3 dark:border-slate-700">
      {posts.isLoading ? (
        <SpinnerRow label="Loading discussion…" />
      ) : (posts.data ?? []).length === 0 ? (
        <p className="text-sm text-slate-500 dark:text-slate-400">No posts yet - start the conversation.</p>
      ) : (
        <ul className="space-y-2">
          {(posts.data ?? []).map((post) => (
            <li key={post.postId} className="rounded-lg bg-slate-50 p-2.5 text-sm dark:bg-slate-800/60">
              <p className="text-xs font-semibold text-slate-700 dark:text-slate-200">
                {post.authorName} · {formatDateTime(post.postedAt)}
              </p>
              <p className="mt-1 whitespace-pre-line text-slate-700 dark:text-slate-300">
                {post.hidden ? 'This post was hidden by a facilitator.' : post.content}
              </p>
            </li>
          ))}
        </ul>
      )}

      {error && <p className="mt-2 text-xs text-rose-600">{error}</p>}

      <form
        className="mt-3 flex gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          if (content.trim()) addPost.mutate();
        }}
      >
        <Input
          aria-label={`Reply to ${thread.topic}`}
          placeholder="Write a reply…"
          value={content}
          onChange={(event) => setContent(event.target.value)}
        />
        <Button type="submit" size="sm" loading={addPost.isPending} disabled={!content.trim()}>
          Reply
        </Button>
      </form>
    </div>
  );
}

export default function ActivityDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const { user, isStaff } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);
  const [openThread, setOpenThread] = useState<number | null>(null);
  const [sessionForm, setSessionForm] = useState({ title: '', scheduledStart: '', durationMinutes: '60', meetingLink: '' });
  const [threadTopic, setThreadTopic] = useState('');

  const activityQuery = useQuery({
    queryKey: ['activity', id],
    queryFn: () => api<Activity>(`/api/v1/activities/${id}`),
  });

  const sessions = useQuery({
    queryKey: ['activity', id, 'sessions'],
    queryFn: () => api<Session[]>(`/api/v1/activities/${id}/sessions`),
  });

  const materials = useQuery({
    queryKey: ['activity', id, 'materials'],
    queryFn: () => api<Material[]>(`/api/v1/activities/${id}/materials`),
  });

  const threads = useQuery({
    queryKey: ['activity', id, 'threads'],
    queryFn: () => api<Thread[]>(`/api/v1/activities/${id}/threads`),
  });

  const teams = useQuery({
    queryKey: ['activity', id, 'teams'],
    queryFn: () => api<Team[]>(`/api/v1/teams/activity/${id}`),
    enabled: activityQuery.data?.level === 'TWO',
  });

  const myEnrollments = useQuery({
    queryKey: ['enrollments', 'mine'],
    queryFn: () => api<Enrollment[]>('/api/v1/enrollments/me'),
    enabled: user?.role === 'PARTICIPANT',
  });

  const roster = useQuery({
    queryKey: ['activity', id, 'roster'],
    queryFn: () => api<ParticipantRow[]>(`/api/v1/activities/${id}/participants`),
    enabled: isStaff,
  });

  const progress = useQuery({
    queryKey: ['activity', id, 'progress'],
    queryFn: () => api<Progress>(`/api/v1/activities/${id}/progress`),
    enabled: !!myEnrollments.data?.some((item) => item.activityId === Number(id) && item.status !== 'CANCELLED'),
  });

  const enrollment = myEnrollments.data?.find((item) => item.activityId === Number(id));

  const enroll = useMutation({
    mutationFn: () => api(`/api/v1/activities/${id}/enroll`, { method: 'POST' }),
    onSuccess: () => {
      setNotice('You are enrolled. Check your notifications for updates.');
      void queryClient.invalidateQueries({ queryKey: ['activity', id] });
      void queryClient.invalidateQueries({ queryKey: ['enrollments', 'mine'] });
      void queryClient.invalidateQueries({ queryKey: ['progress'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not enrol.'),
  });

  const cancelEnrollment = useMutation({
    mutationFn: () => api(`/api/v1/enrollments/${enrollment?.enrollmentId}`, { method: 'DELETE' }),
    onSuccess: () => {
      setNotice('Your enrolment was cancelled.');
      void queryClient.invalidateQueries({ queryKey: ['activity', id] });
      void queryClient.invalidateQueries({ queryKey: ['enrollments', 'mine'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not cancel.'),
  });

  const changeStatus = useMutation({
    mutationFn: (status: string) =>
      api(`/api/v1/activities/${id}/status`, { method: 'PUT', body: { status } }),
    onSuccess: () => {
      setNotice('Activity status updated.');
      void queryClient.invalidateQueries({ queryKey: ['activity', id] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not update the status.'),
  });

  const createSession = useMutation({
    mutationFn: () =>
      api<Session>(`/api/v1/activities/${id}/sessions`, {
        method: 'POST',
        body: {
          title: sessionForm.title,
          scheduledStart: new Date(sessionForm.scheduledStart).toISOString(),
          durationMinutes: Number(sessionForm.durationMinutes),
          meetingLink: sessionForm.meetingLink,
        },
      }),
    onSuccess: () => {
      setSessionForm({ title: '', scheduledStart: '', durationMinutes: '60', meetingLink: '' });
      setNotice('Session scheduled.');
      void queryClient.invalidateQueries({ queryKey: ['activity', id, 'sessions'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not schedule the session.'),
  });

  const createThread = useMutation({
    mutationFn: () =>
      api<Thread>(`/api/v1/activities/${id}/threads`, { method: 'POST', body: { topic: threadTopic } }),
    onSuccess: () => {
      setThreadTopic('');
      setNotice('Discussion thread created.');
      void queryClient.invalidateQueries({ queryKey: ['activity', id, 'threads'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not create the thread.'),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<TeamValues>({
    resolver: zodResolver(teamSchema),
    defaultValues: { teamName: '', activityId: Number(id) },
  });

  const createTeam = useMutation({
    mutationFn: (values: TeamValues) =>
      api<Team>('/api/v1/teams', {
        method: 'POST',
        body: { activityId: values.activityId, teamName: values.teamName, mentorId: values.mentorId },
      }),
    onSuccess: (team) => {
      reset({ teamName: '', activityId: Number(id) });
      void queryClient.invalidateQueries({ queryKey: ['teams'] });
      void queryClient.invalidateQueries({ queryKey: ['activity', id, 'teams'] });
      router.push(`/teams/${team.teamId}`);
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not create the team.'),
  });

  if (activityQuery.isLoading) return <SpinnerRow label="Loading activity…" />;
  if (activityQuery.isError || !activityQuery.data) {
    return (
      <ErrorState
        message={activityQuery.error instanceof ApiError ? activityQuery.error.message : 'This activity could not be found.'}
        onRetry={() => activityQuery.refetch()}
      />
    );
  }

  const activity = activityQuery.data;
  const isLevelTwo = activity.level === 'TWO';

  return (
    <>
      <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Activities', href: '/exchanges' }, { label: activity.title }]} />

      <PageHeader
        title={activity.title}
        description={`Facilitated by ${activity.facilitatorName}`}
        actions={
          <>
            <Badge tone={activity.status}>{titleCase(activity.status)}</Badge>
            {user?.role === 'PARTICIPANT' && !enrollment && activity.status === 'PUBLISHED' && (
              <Button size="sm" loading={enroll.isPending} onClick={() => enroll.mutate()}>
                Enrol now
              </Button>
            )}
            {user?.role === 'PARTICIPANT' && enrollment && enrollment.status !== 'CANCELLED' && (
              <Button
                size="sm"
                variant="secondary"
                loading={cancelEnrollment.isPending}
                onClick={() => cancelEnrollment.mutate()}
              >
                Cancel enrolment
              </Button>
            )}
          </>
        }
      />

      {notice && (
        <div className="mb-4">
          <Alert tone={notice.toLowerCase().includes('could not') ? 'error' : 'success'}>{notice}</Alert>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="space-y-4 lg:col-span-2">
          <Card>
            <CardTitle>About this {isLevelTwo ? 'programme' : 'session'}</CardTitle>
            <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700 dark:text-slate-200">
              {activity.description ?? 'Details will be published by the facilitator soon.'}
            </p>
            <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-600 dark:text-slate-300">
              <Badge>{isLevelTwo ? 'Level Two' : 'Level One'}</Badge>
              {activity.category && <Badge>{titleCase(activity.category)}</Badge>}
              <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">Starts {formatDate(activity.startDate)}</span>
              {activity.endDate && (
                <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">Ends {formatDate(activity.endDate)}</span>
              )}
              <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">
                {activity.enrolledCount} enrolled{activity.capacity ? ` / ${activity.capacity}` : ''}
              </span>
              {activity.waitlistedCount > 0 && (
                <span className="rounded-full bg-amber-100 px-2.5 py-1 text-amber-800 dark:bg-amber-500/20 dark:text-amber-300">
                  {activity.waitlistedCount} waitlisted
                </span>
              )}
            </div>
            {activity.levelFeatures.length > 0 && (
              <ul className="mt-4 flex flex-wrap gap-2">
                {activity.levelFeatures.map((feature) => (
                  <li key={feature} className="rounded-full bg-brand-50 px-3 py-1 text-xs font-medium text-brand-800 dark:bg-brand-500/15 dark:text-brand-200">
                    {feature}
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card>
            <CardTitle>Sessions</CardTitle>
            <CardDescription>Live sessions and check-in windows for this activity.</CardDescription>
            {(sessions.data ?? []).length === 0 ? (
              <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No sessions scheduled yet.</p>
            ) : (
              <ul className="mt-3 divide-y divide-slate-100 dark:divide-slate-700">
                {(sessions.data ?? []).map((session) => (
                  <li key={session.sessionId} className="flex flex-wrap items-center justify-between gap-3 py-2.5">
                    <div>
                      <p className="font-medium text-slate-900 dark:text-slate-100">{session.title}</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        {formatDateTime(session.scheduledStart)} · {session.durationMinutes} min
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge tone={session.status}>{titleCase(session.status)}</Badge>
                      <a
                        href={session.meetingLink}
                        target="_blank"
                        rel="noreferrer noopener"
                        className="text-xs font-semibold text-brand-700 hover:underline dark:text-brand-300"
                      >
                        Join
                      </a>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card>
            <CardTitle>Materials</CardTitle>
            <CardDescription>Slides, videos, links and reading for this activity.</CardDescription>
            {(materials.data ?? []).length === 0 ? (
              <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No materials published yet.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {(materials.data ?? []).map((material) => (
                  <li key={material.materialId} className="rounded-lg border border-slate-200 p-3 dark:border-slate-700">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="font-medium text-slate-900 dark:text-slate-100">{material.title}</p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          {titleCase(material.materialType)} · {material.uploadedByName}
                        </p>
                        {material.description && (
                          <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{material.description}</p>
                        )}
                      </div>
                      <a
                        href={material.url}
                        target="_blank"
                        rel="noreferrer noopener"
                        className="shrink-0 text-xs font-semibold text-brand-700 hover:underline dark:text-brand-300"
                      >
                        Open
                      </a>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card>
            <CardTitle>Discussion</CardTitle>
            <CardDescription>Ask questions and share updates with the cohort.</CardDescription>
            {(threads.data ?? []).length === 0 ? (
              <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No discussion threads yet.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {(threads.data ?? []).map((thread) => (
                  <li key={thread.threadId} className="rounded-lg border border-slate-200 p-3 dark:border-slate-700">
                    <button
                      type="button"
                      className="flex w-full items-center justify-between gap-3 text-left"
                      onClick={() => setOpenThread((current) => (current === thread.threadId ? null : thread.threadId))}
                      aria-expanded={openThread === thread.threadId}
                    >
                      <span className="font-medium text-slate-900 dark:text-slate-100">{thread.topic}</span>
                      <span className="text-xs text-slate-500 dark:text-slate-400">{thread.postCount} posts</span>
                    </button>
                    {openThread === thread.threadId && <ThreadPanel thread={thread} />}
                  </li>
                ))}
              </ul>
            )}
            {isStaff && (
              <form
                className="mt-4 flex gap-2 border-t border-slate-100 pt-4 dark:border-slate-800"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (threadTopic.trim()) createThread.mutate();
                }}
              >
                <Input
                  aria-label="New discussion topic"
                  placeholder="New discussion topic"
                  value={threadTopic}
                  onChange={(event) => setThreadTopic(event.target.value)}
                />
                <Button type="submit" size="sm" loading={createThread.isPending} disabled={!threadTopic.trim()}>
                  Create
                </Button>
              </form>
            )}
          </Card>

          {isLevelTwo && (
            <Card>
              <CardTitle>Teams in this programme</CardTitle>
              <CardDescription>
                {(teams.data ?? []).length} team(s) · teams need {activity.minTeamSize}-{activity.maxTeamSize} members.
              </CardDescription>
              {(teams.data ?? []).length > 0 && (
                <ul className="mt-3 divide-y divide-slate-100 dark:divide-slate-700">
                  {(teams.data ?? []).map((team) => (
                    <li key={team.teamId} className="flex items-center justify-between gap-3 py-2.5">
                      <div>
                        <Link
                          href={`/teams/${team.teamId}`}
                          className="font-medium text-slate-900 hover:text-brand-700 hover:underline dark:text-slate-100 dark:hover:text-brand-300"
                        >
                          {team.teamName}
                        </Link>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          {team.memberCount} members{team.mentorName ? ` · mentor ${team.mentorName}` : ''}
                        </p>
                      </div>
                      <Badge tone={team.status}>{titleCase(team.status)}</Badge>
                    </li>
                  ))}
                </ul>
              )}

              {user?.role === 'PARTICIPANT' && enrollment?.status === 'ENROLLED' && (
                <form
                  className="mt-4 space-y-3 border-t border-slate-100 pt-4 dark:border-slate-800"
                  onSubmit={handleSubmit((values) => createTeam.mutate(values))}
                  noValidate
                >
                  <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Create a team</h3>
                  <Field label="Team name" htmlFor="team-name" error={errors.teamName?.message} required>
                    <Input id="team-name" invalid={!!errors.teamName} {...register('teamName')} />
                  </Field>
                  <input type="hidden" {...register('activityId')} />
                  <Button type="submit" size="sm" loading={isSubmitting || createTeam.isPending}>
                    Create team
                  </Button>
                </form>
              )}
            </Card>
          )}
        </section>

        <aside className="space-y-4">
          <Card>
            <CardTitle>At a glance</CardTitle>
            <dl className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Level</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{isLevelTwo ? 'Level Two' : 'Level One'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Pass mark</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{activity.passMark}%</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Attendance</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{activity.minAttendancePercent}%</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Partner</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{activity.partnerName ?? 'YEP'}</dd>
              </div>
              {enrollment && (
                <div className="flex justify-between gap-3">
                  <dt className="text-slate-500 dark:text-slate-400">Your status</dt>
                  <dd>
                    <Badge tone={enrollment.status}>{titleCase(enrollment.status)}</Badge>
                  </dd>
                </div>
              )}
            </dl>
          </Card>

          {progress.data && (
            <Card>
              <CardTitle>Your progress</CardTitle>
              <dl className="mt-3 space-y-2 text-sm">
                <div className="flex justify-between gap-3">
                  <dt className="text-slate-500 dark:text-slate-400">Attendance</dt>
                  <dd className="font-medium text-slate-800 dark:text-slate-200">
                    {progress.data.attendancePercent === null ? '—' : `${Math.round(progress.data.attendancePercent)}%`}
                  </dd>
                </div>
                <div className="flex justify-between gap-3">
                  <dt className="text-slate-500 dark:text-slate-400">Assessment average</dt>
                  <dd className="font-medium text-slate-800 dark:text-slate-200">
                    {Math.round(progress.data.assessmentAverage)}%
                  </dd>
                </div>
                <div className="flex justify-between gap-3">
                  <dt className="text-slate-500 dark:text-slate-400">Completed</dt>
                  <dd className="font-medium text-slate-800 dark:text-slate-200">{progress.data.completed ? 'Yes' : 'Not yet'}</dd>
                </div>
              </dl>
            </Card>
          )}

          {isStaff && (
            <>
              <Card>
                <CardTitle>Facilitator tools</CardTitle>
                <CardDescription>Update the lifecycle status of this activity.</CardDescription>
                <div className="mt-3 flex flex-wrap gap-2">
                  {['DRAFT', 'PUBLISHED', 'CLOSED', 'COMPLETED'].map((status) => (
                    <Button
                      key={status}
                      size="sm"
                      variant={activity.status === status ? 'primary' : 'secondary'}
                      disabled={changeStatus.isPending || activity.status === status}
                      onClick={() => changeStatus.mutate(status)}
                    >
                      {titleCase(status)}
                    </Button>
                  ))}
                </div>
              </Card>

              <Card>
                <CardTitle>Schedule a session</CardTitle>
                <form
                  className="mt-3 space-y-3"
                  onSubmit={(event) => {
                    event.preventDefault();
                    if (sessionForm.title && sessionForm.scheduledStart && sessionForm.meetingLink) {
                      createSession.mutate();
                    }
                  }}
                >
                  <Field label="Title" htmlFor="session-title" required>
                    <Input
                      id="session-title"
                      value={sessionForm.title}
                      onChange={(event) => setSessionForm((current) => ({ ...current, title: event.target.value }))}
                    />
                  </Field>
                  <Field label="Starts" htmlFor="session-start" required>
                    <Input
                      id="session-start"
                      type="datetime-local"
                      value={sessionForm.scheduledStart}
                      onChange={(event) => setSessionForm((current) => ({ ...current, scheduledStart: event.target.value }))}
                    />
                  </Field>
                  <Field label="Duration (minutes)" htmlFor="session-duration" required>
                    <Input
                      id="session-duration"
                      type="number"
                      min={1}
                      value={sessionForm.durationMinutes}
                      onChange={(event) => setSessionForm((current) => ({ ...current, durationMinutes: event.target.value }))}
                    />
                  </Field>
                  <Field label="Meeting link" htmlFor="session-link" required>
                    <Input
                      id="session-link"
                      type="url"
                      value={sessionForm.meetingLink}
                      onChange={(event) => setSessionForm((current) => ({ ...current, meetingLink: event.target.value }))}
                    />
                  </Field>
                  <Button
                    type="submit"
                    size="sm"
                    loading={createSession.isPending}
                    disabled={!sessionForm.title || !sessionForm.scheduledStart || !sessionForm.meetingLink}
                  >
                    Schedule session
                  </Button>
                </form>
              </Card>

              <Card>
                <CardTitle>Roster ({roster.data?.length ?? 0})</CardTitle>
                <CardDescription>Change enrolment statuses from the roster.</CardDescription>
                {(roster.data ?? []).length === 0 ? (
                  <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No enrolments yet.</p>
                ) : (
                  <ul className="mt-3 max-h-72 space-y-2 overflow-y-auto text-sm">
                    {(roster.data ?? []).map((item) => (
                      <li key={item.youthId} className="flex items-center justify-between gap-2">
                        <span className="text-slate-700 dark:text-slate-200">{item.name}</span>
                        <Badge tone={item.status}>{titleCase(item.status)}</Badge>
                      </li>
                    ))}
                  </ul>
                )}
                <div className="mt-3">
                  <LinkButton href="/assessments" size="sm" variant="secondary">
                    Manage assessments
                  </LinkButton>
                </div>
              </Card>
            </>
          )}

          {!enrollment && user?.role === 'PARTICIPANT' && activity.status !== 'PUBLISHED' && (
            <EmptyState title="Enrolment is closed" description="Ask the facilitator about the next cohort." />
          )}
        </aside>
      </div>
    </>
  );
}

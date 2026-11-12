'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { milestoneSchema, reviewSchema, supportSchema, type MilestoneValues, type ReviewValues, type SupportValues } from '@/lib/schemas';
import { formatDate, formatDateTime, titleCase } from '@/lib/format';
import type { Milestone, Project, Support, Team } from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, Breadcrumbs, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';

const milestoneStatuses = ['PENDING', 'IN_PROGRESS', 'DONE'];

export default function ProjectDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const queryClient = useQueryClient();
  const { user, isStaff } = useAuth();
  const [notice, setNotice] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [milestone, setMilestone] = useState({ title: '', dueDate: '' });
  const [supportType, setSupportType] = useState('MENTORSHIP');
  const [supportMessage, setSupportMessage] = useState('');

  const projectQuery = useQuery({
    queryKey: ['project', id],
    queryFn: () => api<Project>(`/api/v1/projects/${id}`),
  });

  const teamQuery = useQuery({
    queryKey: ['team', projectQuery.data?.teamId],
    queryFn: () => api<Team>(`/api/v1/teams/${projectQuery.data?.teamId}`),
    enabled: !!projectQuery.data?.teamId,
  });

  const supports = useQuery({
    queryKey: ['project', id, 'supports'],
    queryFn: () => api<Support[]>(`/api/v1/projects/${id}/supports`),
    enabled: !!projectQuery.data,
  });

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['project', id] });
    void queryClient.invalidateQueries({ queryKey: ['project', id, 'supports'] });
    void queryClient.invalidateQueries({ queryKey: ['projects'] });
    void queryClient.invalidateQueries({ queryKey: ['team-project'] });
  };

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<MilestoneValues>({ resolver: zodResolver(milestoneSchema), defaultValues: { title: '', dueDate: '' } });

  const {
    register: registerReview,
    handleSubmit: handleReviewSubmit,
    reset: resetReview,
    formState: { errors: reviewErrors, isSubmitting: reviewSubmitting },
  } = useForm<ReviewValues>({
    resolver: zodResolver(reviewSchema),
    defaultValues: { decision: 'COMMENT', feedback: '' },
  });

  const {
    register: registerSupport,
    handleSubmit: handleSupportSubmit,
    reset: resetSupport,
    formState: { errors: supportErrors, isSubmitting: supportSubmitting },
  } = useForm<SupportValues>({
    resolver: zodResolver(supportSchema),
    defaultValues: { supportType: 'MENTORSHIP', message: '' },
  });

  const addMilestone = useMutation({
    mutationFn: (values: MilestoneValues) =>
      api<Milestone>(`/api/v1/projects/${id}/milestones`, {
        method: 'POST',
        body: { title: values.title, dueDate: values.dueDate || null },
      }),
    onSuccess: () => {
      reset();
      setNotice('Milestone added.');
      invalidate();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not add the milestone.'),
  });

  const updateMilestone = useMutation({
    mutationFn: ({ milestoneId, status }: { milestoneId: number; status: string }) =>
      api<Milestone>(`/api/v1/projects/milestones/${milestoneId}`, { method: 'PUT', body: { status } }),
    onSuccess: () => {
      setNotice('Milestone updated.');
      invalidate();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not update the milestone.'),
  });

  const submit = useMutation({
    mutationFn: () => api<Project>(`/api/v1/projects/${id}/submit`, { method: 'POST' }),
    onSuccess: () => {
      setNotice('Project submitted for review.');
      invalidate();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not submit the project.'),
  });

  const review = useMutation({
    mutationFn: (values: ReviewValues) =>
      api(`/api/v1/projects/${id}/reviews`, { method: 'POST', body: values }),
    onSuccess: () => {
      resetReview();
      setNotice('Review recorded.');
      invalidate();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not record the review.'),
  });

  const complete = useMutation({
    mutationFn: () => api<Project>(`/api/v1/projects/${id}/complete`, { method: 'POST' }),
    onSuccess: () => {
      setNotice('Project marked complete.');
      invalidate();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not complete the project.'),
  });

  const toggleSupport = useMutation({
    mutationFn: (openForSupport: boolean) =>
      api<Project>(`/api/v1/projects/${id}/support-visibility`, { method: 'PUT', body: { openForSupport } }),
    onSuccess: () => {
      setNotice('Support visibility updated.');
      invalidate();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not update support visibility.'),
  });

  const offerSupport = useMutation({
    mutationFn: (values: SupportValues) =>
      api<Support>(`/api/v1/projects/${id}/supports`, { method: 'POST', body: values }),
    onSuccess: () => {
      resetSupport();
      setNotice('Support offer sent.');
      invalidate();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not offer support.'),
  });

  const respondSupport = useMutation({
    mutationFn: ({ supportId, accept }: { supportId: number; accept: boolean }) =>
      api<Support>(`/api/v1/supports/${supportId}/respond`, { method: 'PUT', body: { accept } }),
    onSuccess: () => {
      setNotice('Support response recorded.');
      invalidate();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not respond to the support offer.'),
  });

  if (projectQuery.isLoading) return <SpinnerRow label="Loading project…" />;
  if (projectQuery.isError || !projectQuery.data) {
    return (
      <ErrorState
        message={projectQuery.error instanceof ApiError ? projectQuery.error.message : 'This project could not be found.'}
        onRetry={() => projectQuery.refetch()}
      />
    );
  }

  const project = projectQuery.data;
  const team = teamQuery.data;
  const isMember = !!team && team.members.some((member) => member.youthId === user?.userId);
  const isAssignedMentor = !!team && team.mentorId === user?.userId;
  const canManage = isStaff || isAssignedMentor;
  const canEdit = (isMember || canManage) && ['DRAFT', 'CHANGES_REQUESTED'].includes(project.status);
  const canReview = isAssignedMentor && ['SUBMITTED', 'UNDER_REVIEW'].includes(project.status);
  const canRespond = isMember || canManage;

  return (
    <>
      <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Projects', href: '/projects' }, { label: project.title }]} />

      <PageHeader
        title={project.title}
        description={`Team ${project.teamName} · ${titleCase(project.challengeArea)}`}
        actions={
          <>
            <Badge tone={project.status}>{titleCase(project.status)}</Badge>
            <LinkButton href={`/teams/${project.teamId}`} size="sm" variant="secondary">
              Go to team
            </LinkButton>
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
            <CardTitle>About the project</CardTitle>
            <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700 dark:text-slate-200">
              {project.description || 'No description yet.'}
            </p>
            <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-600 dark:text-slate-300">
              <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">{project.progressPercent}% milestones done</span>
              <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">
                Submitted {project.submittedAt ? formatDate(project.submittedAt) : 'not yet'}
              </span>
              {project.openForSupport && <Badge tone="OPEN">Open to partner support</Badge>}
              {project.overdue && <Badge tone="CANCELLED">Overdue</Badge>}
            </div>
            {project.aiUsed && (
              <div className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-900 dark:bg-amber-500/10 dark:text-amber-200">
                Responsible AI disclosure: {project.aiUsageDeclaration || 'AI tools were used while drafting this project.'}
              </div>
            )}
          </Card>

          <Card>
            <div className="flex items-start justify-between gap-3">
              <div>
                <CardTitle>Milestones</CardTitle>
                <CardDescription>At least two milestones are required before submitting for review.</CardDescription>
              </div>
              <Badge>{project.milestones.length}</Badge>
            </div>

            {project.milestones.length === 0 ? (
              <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No milestones yet.</p>
            ) : (
              <ul className="mt-3 divide-y divide-slate-100 dark:divide-slate-700">
                {project.milestones.map((item) => (
                  <li key={item.milestoneId} className="flex flex-wrap items-center justify-between gap-3 py-2.5">
                    <div>
                      <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{item.title}</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        Due {item.dueDate ? formatDate(item.dueDate) : 'not set'}
                        {item.overdue ? ' · overdue' : ''}
                      </p>
                    </div>
                    {canEdit ? (
                      <Select
                        aria-label={`Status for ${item.title}`}
                        value={item.status}
                        onChange={(event) => updateMilestone.mutate({ milestoneId: item.milestoneId, status: event.target.value })}
                        className="w-auto"
                      >
                        {milestoneStatuses.map((status) => (
                          <option key={status} value={status}>
                            {titleCase(status)}
                          </option>
                        ))}
                      </Select>
                    ) : (
                      <Badge tone={item.status}>{titleCase(item.status)}</Badge>
                    )}
                  </li>
                ))}
              </ul>
            )}

            {canEdit && (
              <form
                className="mt-4 grid gap-3 border-t border-slate-100 pt-4 sm:grid-cols-3 dark:border-slate-800"
                onSubmit={handleSubmit((values) => addMilestone.mutate(values))}
                noValidate
              >
                <Field label="Milestone" htmlFor="milestone-title" error={errors.title?.message} className="sm:col-span-2" required>
                  <Input id="milestone-title" invalid={!!errors.title} {...register('title')} />
                </Field>
                <Field label="Due date" htmlFor="milestone-due" error={errors.dueDate?.message}>
                  <Input id="milestone-due" type="date" {...register('dueDate')} />
                </Field>
                <div className="sm:col-span-3">
                  <Button type="submit" size="sm" loading={isSubmitting || addMilestone.isPending}>
                    Add milestone
                  </Button>
                </div>
              </form>
            )}

            {canEdit && (
              <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-4 dark:border-slate-800">
                <Button
                  size="sm"
                  loading={submit.isPending}
                  disabled={project.milestones.length < 2}
                  onClick={() => submit.mutate()}
                >
                  Submit for review
                </Button>
                {project.milestones.length < 2 && (
                  <p className="self-center text-xs text-slate-500 dark:text-slate-400">
                    Add {2 - project.milestones.length} more milestone(s) to submit.
                  </p>
                )}
              </div>
            )}
          </Card>

          <Card>
            <CardTitle>Reviews</CardTitle>
            {project.reviews.length === 0 ? (
              <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No reviews yet.</p>
            ) : (
              <ul className="mt-3 space-y-3">
                {project.reviews.map((item) => (
                  <li key={item.reviewId} className="rounded-lg bg-slate-50 p-3 dark:bg-slate-800/60">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">{item.mentorName}</p>
                      <Badge tone={item.decision}>{titleCase(item.decision)}</Badge>
                    </div>
                    <p className="mt-1 whitespace-pre-line text-sm text-slate-700 dark:text-slate-300">{item.feedback}</p>
                    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{formatDateTime(item.createdAt)}</p>
                  </li>
                ))}
              </ul>
            )}

            {canReview && (
              <form
                className="mt-4 space-y-3 border-t border-slate-100 pt-4 dark:border-slate-800"
                onSubmit={handleReviewSubmit((values) => review.mutate(values))}
                noValidate
              >
                <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Record a review</h3>
                <Field label="Decision" htmlFor="review-decision" error={reviewErrors.decision?.message} required>
                  <Select id="review-decision" {...registerReview('decision')}>
                    <option value="COMMENT">Comment</option>
                    <option value="CHANGES_REQUESTED">Request changes</option>
                    <option value="APPROVED">Approve</option>
                  </Select>
                </Field>
                <Field label="Feedback" htmlFor="review-feedback" error={reviewErrors.feedback?.message} required>
                  <Textarea id="review-feedback" rows={3} invalid={!!reviewErrors.feedback} {...registerReview('feedback')} />
                </Field>
                <Button type="submit" size="sm" loading={reviewSubmitting || review.isPending}>
                  Save review
                </Button>
              </form>
            )}
          </Card>

          <Card>
            <div className="flex items-start justify-between gap-3">
              <div>
                <CardTitle>Partner support</CardTitle>
                <CardDescription>Offers of funding, mentorship, resources or visibility for this project.</CardDescription>
              </div>
              {canManage && (
                <Button
                  size="sm"
                  variant="secondary"
                  loading={toggleSupport.isPending}
                  onClick={() => toggleSupport.mutate(!project.openForSupport)}
                >
                  {project.openForSupport ? 'Close to support' : 'Open to support'}
                </Button>
              )}
            </div>

            {(supports.data ?? []).length === 0 ? (
              <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No support offers yet.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {(supports.data ?? []).map((support) => (
                  <li key={support.supportId} className="rounded-lg border border-slate-200 p-3 dark:border-slate-700">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                          {support.organisationName} · {titleCase(support.supportType)}
                        </p>
                        {support.message && <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{support.message}</p>}
                      </div>
                      <Badge tone={support.status}>{titleCase(support.status)}</Badge>
                    </div>
                    {canRespond && support.status === 'OFFERED' && (
                      <div className="mt-2 flex gap-2">
                        <Button size="sm" onClick={() => respondSupport.mutate({ supportId: support.supportId, accept: true })}>
                          Accept
                        </Button>
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => respondSupport.mutate({ supportId: support.supportId, accept: false })}
                        >
                          Decline
                        </Button>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}

            {user?.role === 'PARTNER' && project.openForSupport && ['APPROVED', 'COMPLETED'].includes(project.status) && (
              <form
                className="mt-4 space-y-3 border-t border-slate-100 pt-4 dark:border-slate-800"
                onSubmit={handleSupportSubmit((values) => offerSupport.mutate(values))}
                noValidate
              >
                <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Offer support</h3>
                <Field label="Support type" htmlFor="support-type" error={supportErrors.supportType?.message} required>
                  <Select id="support-type" {...registerSupport('supportType')}>
                    {['FUNDING', 'MENTORSHIP', 'RESOURCES', 'VISIBILITY'].map((type) => (
                      <option key={type} value={type}>
                        {titleCase(type)}
                      </option>
                    ))}
                  </Select>
                </Field>
                <Field label="Message" htmlFor="support-message" error={supportErrors.message?.message}>
                  <Textarea id="support-message" rows={2} {...registerSupport('message')} />
                </Field>
                <Button type="submit" size="sm" loading={supportSubmitting || offerSupport.isPending}>
                  Send offer
                </Button>
              </form>
            )}
          </Card>
        </section>

        <aside className="space-y-4">
          <Card>
            <CardTitle>At a glance</CardTitle>
            <dl className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Status</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{titleCase(project.status)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Team</dt>
                <dd className="text-right font-medium text-slate-800 dark:text-slate-200">
                  <Link className="text-brand-700 hover:underline dark:text-brand-300" href={`/teams/${project.teamId}`}>
                    {project.teamName}
                  </Link>
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Activity</dt>
                <dd className="text-right font-medium text-slate-800 dark:text-slate-200">{project.activityTitle}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">AI used</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{project.aiUsed ? 'Yes' : 'No'}</dd>
              </div>
            </dl>
          </Card>

          {canManage && (
            <Card>
              <CardTitle>Mentor tools</CardTitle>
              <div className="mt-3 space-y-2">
                <Button
                  size="sm"
                  loading={complete.isPending}
                  disabled={project.status !== 'APPROVED'}
                  onClick={() => complete.mutate()}
                >
                  Mark complete
                </Button>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Only an approved project can be marked complete.
                </p>
              </div>
            </Card>
          )}
        </aside>
      </div>
    </>
  );
}

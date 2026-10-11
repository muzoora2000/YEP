'use client';

import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { projectSchema, type ProjectValues } from '@/lib/schemas';
import { formatDate, titleCase } from '@/lib/format';
import type { Project, Team } from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, Breadcrumbs, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Checkbox, Field, Input, Textarea } from '@/components/ui/Form';

export default function TeamDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user, isStaff } = useAuth();
  const [notice, setNotice] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);

  const teamQuery = useQuery({
    queryKey: ['team', id],
    queryFn: () => api<Team>(`/api/v1/teams/${id}`),
  });

  const projectQuery = useQuery({
    queryKey: ['team-project', id],
    queryFn: async () => {
      try {
        return await api<Project>(`/api/v1/teams/${id}/project`);
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
      }
    },
  });

  const join = useMutation({
    mutationFn: () => api<Team>(`/api/v1/teams/${id}/join`, { method: 'POST' }),
    onSuccess: () => {
      setNotice('You joined this team.');
      void queryClient.invalidateQueries({ queryKey: ['team', id] });
      void queryClient.invalidateQueries({ queryKey: ['teams'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not join this team.'),
  });

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ProjectValues>({
    resolver: zodResolver(projectSchema),
    defaultValues: {
      title: projectQuery.data?.title ?? '',
      problemStatement: projectQuery.data?.problemStatement ?? '',
      proposedSolution: projectQuery.data?.proposedSolution ?? '',
      aiAssisted: projectQuery.data?.aiAssisted ?? false,
      aiUsageNote: projectQuery.data?.aiUsageNote ?? '',
    },
    values: {
      title: projectQuery.data?.title ?? '',
      problemStatement: projectQuery.data?.problemStatement ?? '',
      proposedSolution: projectQuery.data?.proposedSolution ?? '',
      aiAssisted: projectQuery.data?.aiAssisted ?? false,
      aiUsageNote: projectQuery.data?.aiUsageNote ?? '',
    },
  });

  const saveProject = useMutation({
    mutationFn: (values: ProjectValues) => {
      const body = {
        title: values.title,
        problemStatement: values.problemStatement || null,
        proposedSolution: values.proposedSolution || null,
        aiAssisted: values.aiAssisted ?? false,
        aiUsageNote: values.aiUsageNote || null,
      };
      return projectQuery.data
        ? api<Project>(`/api/v1/projects/${projectQuery.data.id}`, { method: 'PUT', body })
        : api<Project>(`/api/v1/projects?teamId=${id}`, { method: 'POST', body });
    },
    onSuccess: (project) => {
      setNotice('Project saved.');
      setEditing(false);
      void queryClient.invalidateQueries({ queryKey: ['team-project', id] });
      void queryClient.invalidateQueries({ queryKey: ['team', id] });
      if (!projectQuery.data) router.push(`/projects/${project.id}`);
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not save the project.'),
  });

  const submitProject = useMutation({
    mutationFn: () =>
      api<Project>(`/api/v1/projects/${projectQuery.data?.id}/submit`, { method: 'POST' }),
    onSuccess: () => {
      setNotice('Project submitted for review.');
      void queryClient.invalidateQueries({ queryKey: ['team-project', id] });
      void queryClient.invalidateQueries({ queryKey: ['team', id] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not submit the project.'),
  });

  if (teamQuery.isLoading) return <SpinnerRow label="Loading team…" />;
  if (teamQuery.isError || !teamQuery.data) {
    return (
      <ErrorState
        message={teamQuery.error instanceof ApiError ? teamQuery.error.message : 'This team could not be found.'}
        onRetry={() => teamQuery.refetch()}
      />
    );
  }

  const team = teamQuery.data;
  const project = projectQuery.data ?? null;
  const isMember = (team.members ?? []).some((member) => member.participantId === user?.id);
  const canEditProject = !!user && (user.role === 'PARTICIPANT' || user.role === 'ADMIN' || user.role === 'FACILITATOR');
  const canReview = isStaff;

  return (
    <>
      <Breadcrumbs
        items={[
          { label: 'Home', href: '/' },
          { label: 'Teams', href: '/teams' },
          { label: team.name },
        ]}
      />

      <PageHeader
        title={team.name}
        description={team.description || team.exchangeTitle || 'Level Two team'}
        actions={
          <>
            {team.levelTwoExchangeId && (
              <LinkButton href={`/exchanges/${team.levelTwoExchangeId}`} size="sm" variant="secondary">
                {team.exchangeTitle ?? 'Open exchange'}
              </LinkButton>
            )}
            {user?.role === 'PARTICIPANT' && !isMember && (
              <Button size="sm" loading={join.isPending} onClick={() => join.mutate()}>
                Join this team
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
            <CardTitle>Members ({team.memberCount})</CardTitle>
            <ul className="mt-3 divide-y divide-slate-100 dark:divide-slate-700">
              {(team.members ?? []).map((member) => (
                <li key={`${member.participantId}-${member.memberRole}`} className="flex items-center justify-between gap-3 py-2.5">
                  <div>
                    <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{member.fullName ?? 'Participant'}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">Joined {formatDate(member.joinedAt)}</p>
                  </div>
                  <Badge tone={member.memberRole === 'LEAD' ? 'LEVEL_TWO' : 'DRAFT'}>
                    {titleCase(member.memberRole)}
                  </Badge>
                </li>
              ))}
            </ul>
          </Card>

          <Card>
            <div className="flex items-start justify-between gap-3">
              <div>
                <CardTitle>Impact project</CardTitle>
                <CardDescription>
                  The team&apos;s solution to a real problem, reviewed by a human facilitator.
                </CardDescription>
              </div>
              {project && <Badge tone={project.status}>{titleCase(project.status)}</Badge>}
            </div>

            {projectQuery.isLoading ? (
              <SpinnerRow label="Loading project…" />
            ) : !project ? (
              canEditProject ? (
                <p className="mt-3 text-sm text-slate-600 dark:text-slate-300">
                  No project yet. Fill in the first draft below - save it as often as you like before submitting.
                </p>
              ) : (
                <p className="mt-3 text-sm text-slate-600 dark:text-slate-300">This team has not started a project yet.</p>
              )
            ) : (
              <div className="mt-3 space-y-3">
                <div>
                  <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{project.title}</h3>
                  <p className="mt-1 whitespace-pre-line text-sm text-slate-700 dark:text-slate-200">
                    <strong className="text-slate-900 dark:text-slate-100">Problem: </strong>
                    {project.problemStatement || 'Not documented yet.'}
                  </p>
                  <p className="mt-2 whitespace-pre-line text-sm text-slate-700 dark:text-slate-200">
                    <strong className="text-slate-900 dark:text-slate-100">Proposed solution: </strong>
                    {project.proposedSolution || 'Not documented yet.'}
                  </p>
                  {project.aiAssisted && (
                    <p className="mt-2 rounded-lg bg-amber-50 dark:bg-amber-500/10 px-3 py-2 text-xs text-amber-900 dark:text-amber-200">
                      AI was used in drafting this project: {project.aiUsageNote || 'no note given'}.
                      A human reviewer still checked it.
                    </p>
                  )}
                </div>
                <div className="flex flex-wrap gap-2">
                  <LinkButton href={`/projects/${project.id}`} size="sm" variant="secondary">
                    Open project
                  </LinkButton>
                  {canEditProject && ['DRAFT', 'REJECTED'].includes(project.status) && (
                    <>
                      <Button size="sm" variant="secondary" onClick={() => setEditing((value) => !value)}>
                        {editing ? 'Close editor' : 'Edit draft'}
                      </Button>
                      <Button size="sm" loading={submitProject.isPending} onClick={() => submitProject.mutate()}>
                        Submit for review
                      </Button>
                    </>
                  )}
                  {canReview && project.status === 'UNDER_REVIEW' && (
                    <LinkButton href={`/projects/${project.id}`} size="sm">
                      Review it
                    </LinkButton>
                  )}
                </div>
                {project.reviewFeedback && (
                  <p className="rounded-lg bg-slate-50 dark:bg-slate-800/60 px-3 py-2 text-sm text-slate-700 dark:text-slate-200">
                    <strong>Reviewer feedback: </strong>
                    {project.reviewFeedback}
                  </p>
                )}
              </div>
            )}

            {(canEditProject && (!project || editing)) && (
              <form
                className="mt-5 space-y-4 border-t border-slate-100 dark:border-slate-800 pt-4"
                onSubmit={handleSubmit((values) => saveProject.mutate(values))}
                noValidate
              >
                <Field label="Project title" htmlFor="project-title" error={errors.title?.message} required>
                  <Input id="project-title" invalid={!!errors.title} {...register('title')} />
                </Field>

                <Field
                  label="Problem statement"
                  htmlFor="problem"
                  error={errors.problemStatement?.message}
                  hint="What real problem does your community face?"
                >
                  <Textarea id="problem" rows={4} {...register('problemStatement')} />
                </Field>

                <Field
                  label="Proposed solution"
                  htmlFor="solution"
                  error={errors.proposedSolution?.message}
                  hint="What will your team build or change?"
                >
                  <Textarea id="solution" rows={4} {...register('proposedSolution')} />
                </Field>

                <Checkbox
                  label="We used AI tools while drafting this project"
                  {...register('aiAssisted')}
                />

                <Field
                  label="How was AI used?"
                  htmlFor="ai-note"
                  error={errors.aiUsageNote?.message}
                  hint="Disclose any AI assistance - it is required for responsible AI review"
                >
                  <Textarea id="ai-note" rows={2} {...register('aiUsageNote')} />
                </Field>

                <Button type="submit" loading={isSubmitting || saveProject.isPending}>
                  {project ? 'Save changes' : 'Create project'}
                </Button>
              </form>
            )}
          </Card>
        </section>

        <aside className="space-y-4">
          <Card>
            <CardTitle>Team details</CardTitle>
            <dl className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Created</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{formatDate(team.createdAt)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Exchange</dt>
                <dd className="text-right font-medium text-slate-800 dark:text-slate-200">
                  {team.levelTwoExchangeId ? (
                    <Link className="text-brand-700 dark:text-brand-300 hover:underline" href={`/exchanges/${team.levelTwoExchangeId}`}>
                      {team.exchangeTitle ?? 'Level Two programme'}
                    </Link>
                  ) : (
                    '—'
                  )}
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Mentor</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{team.mentorName ?? 'Not assigned yet'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Members</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{team.memberCount}</dd>
              </div>
            </dl>
          </Card>

          <Card>
            <CardTitle>Next steps</CardTitle>
            <ul className="mt-3 space-y-2 text-sm text-slate-700 dark:text-slate-200">
              <li>1. Agree on a problem worth solving.</li>
              <li>2. Draft and submit your impact project.</li>
              <li>3. Iterate on reviewer feedback.</li>
              <li>4. Complete the project to unlock your certificate.</li>
            </ul>
            {!isMember && user?.role === 'PARTICIPANT' && (
              <div className="mt-4">
                <Button size="sm" loading={join.isPending} onClick={() => join.mutate()}>
                  Join this team
                </Button>
              </div>
            )}
          </Card>
        </aside>
      </div>
    </>
  );
}

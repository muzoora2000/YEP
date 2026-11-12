'use client';

import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { projectSchema, type ProjectValues } from '@/lib/schemas';
import { titleCase } from '@/lib/format';
import type { Project, Team } from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, Breadcrumbs, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';

export default function TeamDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user, isStaff } = useAuth();
  const [notice, setNotice] = useState<string | null>(null);
  const [youthId, setYouthId] = useState('');
  const [mentorId, setMentorId] = useState('');

  const teamQuery = useQuery({
    queryKey: ['team', id],
    queryFn: () => api<Team>(`/api/v1/teams/${id}`),
  });

  const projectQuery = useQuery({
    queryKey: ['team-project', id],
    queryFn: async () => {
      try {
        return await api<Project>(`/api/v1/projects/team/${id}`);
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
      }
    },
  });

  const invalidateTeam = () => {
    void queryClient.invalidateQueries({ queryKey: ['team', id] });
    void queryClient.invalidateQueries({ queryKey: ['teams'] });
  };

  const join = useMutation({
    mutationFn: () => api<Team>(`/api/v1/teams/${id}/members`, { method: 'POST', body: {} }),
    onSuccess: () => {
      setNotice('You joined this team.');
      invalidateTeam();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not join this team.'),
  });

  const addMember = useMutation({
    mutationFn: () =>
      api<Team>(`/api/v1/teams/${id}/members`, { method: 'POST', body: { youthId: Number(youthId) } }),
    onSuccess: () => {
      setYouthId('');
      setNotice('Member added.');
      invalidateTeam();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not add that member.'),
  });

  const removeMember = useMutation({
    mutationFn: (memberId: number) => api<Team>(`/api/v1/teams/${id}/members/${memberId}`, { method: 'DELETE' }),
    onSuccess: () => {
      setNotice('Member removed.');
      invalidateTeam();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not remove that member.'),
  });

  const assignMentor = useMutation({
    mutationFn: () => api<Team>(`/api/v1/teams/${id}/mentor`, { method: 'PUT', body: { mentorId: Number(mentorId) } }),
    onSuccess: () => {
      setMentorId('');
      setNotice('Mentor assigned.');
      invalidateTeam();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not assign the mentor.'),
  });

  const lock = useMutation({
    mutationFn: () => api<Team>(`/api/v1/teams/${id}/lock`, { method: 'PUT' }),
    onSuccess: () => {
      setNotice('Team locked.');
      invalidateTeam();
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not lock the team.'),
  });

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ProjectValues>({
    resolver: zodResolver(projectSchema),
    defaultValues: {
      teamId: Number(id),
      title: '',
      description: '',
      challengeArea: 'EMPLOYMENT',
      aiUsed: false,
      aiUsageDeclaration: '',
    },
  });

  const createProject = useMutation({
    mutationFn: (values: ProjectValues) =>
      api<Project>('/api/v1/projects', {
        method: 'POST',
        body: {
          teamId: values.teamId,
          title: values.title,
          description: values.description || null,
          challengeArea: values.challengeArea,
          aiUsed: values.aiUsed ?? false,
          aiUsageDeclaration: values.aiUsageDeclaration || null,
        },
      }),
    onSuccess: (project) => {
      void queryClient.invalidateQueries({ queryKey: ['team-project', id] });
      invalidateTeam();
      router.push(`/projects/${project.projectId}`);
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not create the project.'),
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
  const isMember = team.members.some((member) => member.youthId === user?.userId);
  const isMentor = team.mentorId === user?.userId;
  const canManage = isStaff || isMentor;
  const canWork = isMember || canManage;

  return (
    <>
      <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Teams', href: '/teams' }, { label: team.teamName }]} />

      <PageHeader
        title={team.teamName}
        description={`${team.activityTitle} · ${team.memberCount}/${team.maxTeamSize} members`}
        actions={
          <>
            <LinkButton href={`/exchanges/${team.activityId}`} size="sm" variant="secondary">
              Open activity
            </LinkButton>
            {user?.role === 'PARTICIPANT' && !isMember && team.status !== 'LOCKED' && (
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
            <div className="flex items-start justify-between gap-3">
              <div>
                <CardTitle>Members ({team.memberCount})</CardTitle>
                <CardDescription>Team size must reach {team.minTeamSize} before it can be locked.</CardDescription>
              </div>
              <Badge tone={team.status}>{titleCase(team.status)}</Badge>
            </div>
            <ul className="mt-3 divide-y divide-slate-100 dark:divide-slate-700">
              {team.members.map((member) => (
                <li key={member.memberId} className="flex items-center justify-between gap-3 py-2.5">
                  <div>
                    <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{member.name}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">{member.email}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge tone={member.role === 'LEADER' ? 'APPROVED' : 'DRAFT'}>{titleCase(member.role)}</Badge>
                    {canManage && team.status !== 'LOCKED' && member.youthId !== user?.userId && (
                      <Button
                        size="sm"
                        variant="ghost"
                        loading={removeMember.isPending}
                        onClick={() => removeMember.mutate(member.youthId)}
                      >
                        Remove
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ul>

            {canManage && team.status !== 'LOCKED' && (
              <form
                className="mt-4 flex flex-wrap items-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-800"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (youthId) addMember.mutate();
                }}
              >
                <Field label="Add participant by user ID" htmlFor="youth-id">
                  <Input
                    id="youth-id"
                    type="number"
                    min={1}
                    value={youthId}
                    onChange={(event) => setYouthId(event.target.value)}
                    placeholder="e.g. 12"
                  />
                </Field>
                <Button type="submit" size="sm" loading={addMember.isPending} disabled={!youthId}>
                  Add member
                </Button>
              </form>
            )}
          </Card>

          <Card>
            <div className="flex items-start justify-between gap-3">
              <div>
                <CardTitle>Impact project</CardTitle>
                <CardDescription>The team&apos;s solution to a real community problem.</CardDescription>
              </div>
              {project && <Badge tone={project.status}>{titleCase(project.status)}</Badge>}
            </div>

            {projectQuery.isLoading ? (
              <SpinnerRow label="Loading project…" />
            ) : project ? (
              <div className="mt-3 space-y-3">
                <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{project.title}</h3>
                <p className="text-sm text-slate-700 dark:text-slate-200">{project.description || 'No description yet.'}</p>
                <div className="flex flex-wrap gap-2">
                  <LinkButton href={`/projects/${project.projectId}`} size="sm" variant="secondary">
                    Open project
                  </LinkButton>
                  {project.milestones.length < 2 && canWork && (
                    <span className="text-xs text-slate-500 dark:text-slate-400">Add at least two milestones before submitting.</span>
                  )}
                </div>
              </div>
            ) : canWork ? (
              <form className="mt-4 space-y-4" onSubmit={handleSubmit((values) => createProject.mutate(values))} noValidate>
                <p className="text-sm text-slate-600 dark:text-slate-300">
                  Start the project draft. You can add milestones and submit it for review from the project page.
                </p>
                <Field label="Project title" htmlFor="project-title" error={errors.title?.message} required>
                  <Input id="project-title" invalid={!!errors.title} {...register('title')} />
                </Field>
                <Field label="Description" htmlFor="project-description" error={errors.description?.message}>
                  <Textarea id="project-description" rows={3} {...register('description')} />
                </Field>
                <Field label="Challenge area" htmlFor="challenge-area" error={errors.challengeArea?.message} required>
                  <Select id="challenge-area" {...register('challengeArea')}>
                    {['EMPLOYMENT', 'EDUCATION', 'AGRICULTURE', 'HEALTH', 'CLIMATE_CHANGE', 'DIGITAL_INCLUSION'].map((area) => (
                      <option key={area} value={area}>
                        {titleCase(area)}
                      </option>
                    ))}
                  </Select>
                </Field>
                <label className="flex items-start gap-3 text-sm text-slate-700 dark:text-slate-300">
                  <input
                    type="checkbox"
                    className="mt-0.5 h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-600 dark:border-slate-600 dark:bg-slate-800"
                    {...register('aiUsed')}
                  />
                  <span>We used AI tools while drafting this project</span>
                </label>
                <Field label="How was AI used?" htmlFor="ai-declaration" error={errors.aiUsageDeclaration?.message}>
                  <Textarea id="ai-declaration" rows={2} {...register('aiUsageDeclaration')} />
                </Field>
                <input type="hidden" {...register('teamId')} />
                <Button type="submit" loading={isSubmitting || createProject.isPending}>
                  Create project
                </Button>
              </form>
            ) : (
              <p className="mt-3 text-sm text-slate-600 dark:text-slate-300">This team has not started a project yet.</p>
            )}
          </Card>
        </section>

        <aside className="space-y-4">
          <Card>
            <CardTitle>Team details</CardTitle>
            <dl className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Activity</dt>
                <dd className="text-right font-medium text-slate-800 dark:text-slate-200">
                  <Link className="text-brand-700 hover:underline dark:text-brand-300" href={`/exchanges/${team.activityId}`}>
                    {team.activityTitle}
                  </Link>
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Created by</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{team.createdByName}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Mentor</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{team.mentorName ?? 'Not assigned'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Size</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">
                  {team.memberCount} / {team.maxTeamSize}
                </dd>
              </div>
            </dl>
          </Card>

          {canManage && (
            <Card>
              <CardTitle>Manage team</CardTitle>
              <div className="mt-3 space-y-3">
                <form
                  className="flex items-end gap-2"
                  onSubmit={(event) => {
                    event.preventDefault();
                    if (mentorId) assignMentor.mutate();
                  }}
                >
                  <Field label="Mentor user ID" htmlFor="mentor-id">
                    <Input
                      id="mentor-id"
                      type="number"
                      min={1}
                      value={mentorId}
                      onChange={(event) => setMentorId(event.target.value)}
                      placeholder="e.g. 7"
                    />
                  </Field>
                  <Button type="submit" size="sm" loading={assignMentor.isPending} disabled={!mentorId}>
                    Assign
                  </Button>
                </form>
                <Button
                  size="sm"
                  variant="secondary"
                  loading={lock.isPending}
                  disabled={team.status === 'LOCKED' || team.memberCount < team.minTeamSize}
                  onClick={() => lock.mutate()}
                >
                  {team.status === 'LOCKED' ? 'Team locked' : 'Lock team'}
                </Button>
                {team.memberCount < team.minTeamSize && (
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Add {team.minTeamSize - team.memberCount} more member(s) before locking.
                  </p>
                )}
              </div>
            </Card>
          )}

          <Card>
            <CardTitle>Next steps</CardTitle>
            <ul className="mt-3 space-y-2 text-sm text-slate-700 dark:text-slate-200">
              <li>1. Agree on a problem worth solving.</li>
              <li>2. Create the project and add at least two milestones.</li>
              <li>3. Submit for review and respond to feedback.</li>
              <li>4. Complete the project to unlock certificates.</li>
            </ul>
          </Card>
        </aside>
      </div>
    </>
  );
}

'use client';

import { useParams, useRouter } from 'next/navigation';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { teamSchema, type TeamValues } from '@/lib/schemas';
import { formatDate, titleCase } from '@/lib/format';
import type { Enrollment, Exchange, Team } from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, Breadcrumbs, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Textarea } from '@/components/ui/Form';

export default function ExchangeDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const { user, isStaff } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);

  const exchangeQuery = useQuery({
    queryKey: ['exchange', id],
    queryFn: () => api<Exchange>(`/api/v1/exchanges/${id}`),
  });

  const enrollmentsQuery = useQuery({
    queryKey: ['exchange', id, 'enrollments'],
    queryFn: () => api<Enrollment[]>(`/api/v1/exchanges/${id}/enrollments`),
    enabled: isStaff,
  });

  const teamsQuery = useQuery({
    queryKey: ['teams', 'exchange', id],
    queryFn: () => api<Team[]>(`/api/v1/teams?exchangeId=${id}`),
    enabled: exchangeQuery.data?.level === 'LEVEL_TWO',
  });

  const enroll = useMutation({
    mutationFn: () => api(`/api/v1/exchanges/${id}/enroll`, { method: 'POST' }),
    onSuccess: () => {
      setNotice('You are enrolled. Good luck!');
      void queryClient.invalidateQueries({ queryKey: ['exchange', id] });
      void queryClient.invalidateQueries({ queryKey: ['enrollments', 'mine'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not enrol.'),
  });

  const changeStatus = useMutation({
    mutationFn: (status: string) =>
      api(`/api/v1/exchanges/${id}/status`, { method: 'PATCH', body: { status } }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['exchange', id] }),
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not update the status.'),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<TeamValues>({
    resolver: zodResolver(teamSchema),
    defaultValues: { name: '', description: '', levelTwoExchangeId: Number(id) },
  });

  const createTeam = useMutation({
    mutationFn: (values: TeamValues) => api<Team>('/api/v1/teams', { method: 'POST', body: values }),
    onSuccess: (team) => {
      reset();
      void queryClient.invalidateQueries({ queryKey: ['teams'] });
      router.push(`/teams/${team.id}`);
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not create the team.'),
  });

  if (exchangeQuery.isLoading) return <SpinnerRow label="Loading exchange…" />;
  if (exchangeQuery.isError || !exchangeQuery.data) {
    return (
      <ErrorState
        message={
          exchangeQuery.error instanceof ApiError
            ? exchangeQuery.error.message
            : 'This exchange could not be found.'
        }
        onRetry={() => exchangeQuery.refetch()}
      />
    );
  }

  const exchange = exchangeQuery.data;
  const isLevelTwo = exchange.level === 'LEVEL_TWO';

  return (
    <>
      <Breadcrumbs
        items={[
          { label: 'Home', href: '/' },
          { label: 'Exchanges', href: '/exchanges' },
          { label: exchange.title },
        ]}
      />

      <PageHeader
        title={exchange.title}
        description={exchange.summaryLabel}
        actions={
          <>
            <Badge tone={exchange.status}>{titleCase(exchange.status)}</Badge>
            {user?.role === 'PARTICIPANT' && exchange.canEnroll && (
              <Button size="sm" loading={enroll.isPending} onClick={() => enroll.mutate()}>
                Enrol now
              </Button>
            )}
            {isStaff && (
              <LinkButton href="/admin" size="sm" variant="secondary">
                Manage
              </LinkButton>
            )}
          </>
        }
      />

      {notice && <div className="mb-4"><Alert tone={notice.includes('enrolled') ? 'success' : 'error'}>{notice}</Alert></div>}

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="lg:col-span-2 space-y-4">
          <Card>
            <CardTitle>About this {isLevelTwo ? 'programme' : 'session'}</CardTitle>
            <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700">
              {exchange.description ?? 'Details will be published by the facilitator soon.'}
            </p>
            <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-600">
              <Badge>{isLevelTwo ? 'Level Two' : 'Level One'}</Badge>
              {exchange.topic && <Badge tone="LEVEL_ONE">{titleCase(exchange.topic)}</Badge>}
              {exchange.focusArea && <Badge tone="LEVEL_TWO">{titleCase(exchange.focusArea)}</Badge>}
              <span className="rounded-full bg-slate-100 px-2.5 py-1">Starts {formatDate(exchange.startDate)}</span>
              {exchange.endDate && (
                <span className="rounded-full bg-slate-100 px-2.5 py-1">Ends {formatDate(exchange.endDate)}</span>
              )}
              <span className="rounded-full bg-slate-100 px-2.5 py-1">{exchange.enrolledCount} enrolled</span>
              {exchange.maxParticipants && (
                <span className="rounded-full bg-slate-100 px-2.5 py-1">Cap {exchange.maxParticipants}</span>
              )}
            </div>
            {exchange.sessionLink && (
              <div className="mt-4 rounded-lg bg-brand-50 px-3 py-2 text-sm text-brand-900">
                Session link:{' '}
                <a href={exchange.sessionLink} target="_blank" rel="noreferrer noopener" className="font-semibold underline">
                  {exchange.sessionLink}
                </a>
              </div>
            )}
          </Card>

          {isLevelTwo && (
            <Card>
              <CardTitle>Teams in this programme</CardTitle>
              <CardDescription>
                {teamsQuery.isLoading
                  ? 'Loading teams…'
                  : `${teamsQuery.data?.length ?? 0} team(s) · maximum ${exchange.maxTeams ?? '—'}`}
              </CardDescription>

              {(teamsQuery.data ?? []).length > 0 && (
                <ul className="mt-3 divide-y divide-slate-100">
                  {(teamsQuery.data ?? []).map((team) => (
                    <li key={team.id} className="flex items-center justify-between gap-3 py-2.5">
                      <div>
                        <a href={`/teams/${team.id}`} className="font-medium text-slate-900 hover:text-brand-700 hover:underline">
                          {team.name}
                        </a>
                        <p className="text-xs text-slate-500">
                          {team.memberCount} members{team.mentorName ? ` · mentor ${team.mentorName}` : ''}
                        </p>
                      </div>
                      <Badge tone={team.impactProjectStatus ?? 'DRAFT'}>
                        {team.impactProjectStatus ? titleCase(team.impactProjectStatus) : 'No project'}
                      </Badge>
                    </li>
                  ))}
                </ul>
              )}

              {user?.role === 'PARTICIPANT' && (
                <form
                  className="mt-4 space-y-3 border-t border-slate-100 pt-4"
                  onSubmit={handleSubmit((values) => createTeam.mutate(values))}
                  noValidate
                >
                  <h3 className="text-sm font-semibold text-slate-900">Create a team</h3>
                  <Field label="Team name" htmlFor="team-name" error={errors.name?.message} required>
                    <Input id="team-name" invalid={!!errors.name} {...register('name')} />
                  </Field>
                  <Field label="What will your team work on?" htmlFor="team-description" error={errors.description?.message}>
                    <Textarea id="team-description" {...register('description')} />
                  </Field>
                  <input type="hidden" {...register('levelTwoExchangeId')} />
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
                <dt className="text-slate-500">Level</dt>
                <dd className="font-medium text-slate-800">{isLevelTwo ? 'Level Two' : 'Level One'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500">Status</dt>
                <dd className="font-medium text-slate-800">{titleCase(exchange.status)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500">Starts</dt>
                <dd className="font-medium text-slate-800">{formatDate(exchange.startDate)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500">Participants</dt>
                <dd className="font-medium text-slate-800">{exchange.enrolledCount}</dd>
              </div>
              {isLevelTwo && (
                <div className="flex justify-between gap-3">
                  <dt className="text-slate-500">Teams</dt>
                  <dd className="font-medium text-slate-800">{exchange.teamCount}</dd>
                </div>
              )}
            </dl>
            {!isLevelTwo && user?.role === 'PARTICIPANT' && !exchange.canEnroll && (
              <p className="mt-3 text-xs text-slate-500">
                Enrolment is closed for this session - ask the facilitator for the next cohort.
              </p>
            )}
          </Card>

          {isStaff && (
            <Card>
              <CardTitle>Facilitator tools</CardTitle>
              <CardDescription>Update the lifecycle status of this exchange.</CardDescription>
              <div className="mt-3 flex flex-wrap gap-2">
                {['OPEN', 'ACTIVE', 'COMPLETED', 'CANCELLED'].map((status) => (
                  <Button
                    key={status}
                    size="sm"
                    variant={exchange.status === status ? 'primary' : 'secondary'}
                    disabled={changeStatus.isPending || exchange.status === status}
                    onClick={() => changeStatus.mutate(status)}
                  >
                    {titleCase(status)}
                  </Button>
                ))}
              </div>
            </Card>
          )}

          {isStaff && enrollmentsQuery.data && (
            <Card>
              <CardTitle>Enrolments ({enrollmentsQuery.data.length})</CardTitle>
              <ul className="mt-3 max-h-72 space-y-2 overflow-y-auto text-sm">
                {enrollmentsQuery.data.map((enrollment) => (
                  <li key={enrollment.id} className="flex items-center justify-between gap-2">
                    <span className="text-slate-700">{enrollment.participantName ?? 'Participant'}</span>
                    <Badge tone={enrollment.completionStatus}>{titleCase(enrollment.completionStatus)}</Badge>
                  </li>
                ))}
              </ul>
            </Card>
          )}
        </aside>
      </div>
    </>
  );
}

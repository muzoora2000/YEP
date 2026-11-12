'use client';

import Link from 'next/link';
import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api, withQuery } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatDate, titleCase } from '@/lib/format';
import type { Activity, Enrollment, PageResponse } from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select } from '@/components/ui/Form';

function ActivityCard({ activity, enrollment }: { activity: Activity; enrollment?: Enrollment }) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const enroll = useMutation({
    mutationFn: () => api(`/api/v1/activities/${activity.activityId}/enroll`, { method: 'POST' }),
    onSuccess: () => {
      setError(null);
      void queryClient.invalidateQueries({ queryKey: ['activities'] });
      void queryClient.invalidateQueries({ queryKey: ['enrollments', 'mine'] });
      void queryClient.invalidateQueries({ queryKey: ['activity', String(activity.activityId)] });
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : 'Could not enrol right now.'),
  });

  const canEnroll = user?.role === 'PARTICIPANT' && !enrollment && activity.status === 'PUBLISHED';

  return (
    <Card as="article" className="flex flex-col">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-brand-700 dark:text-brand-300">
            {activity.level === 'ONE' ? 'Level One session' : 'Level Two programme'}
          </p>
          <CardTitle className="mt-1">
            <Link
              href={`/exchanges/${activity.activityId}`}
              className="hover:text-brand-700 hover:underline dark:hover:text-brand-300"
            >
              {activity.title}
            </Link>
          </CardTitle>
        </div>
        <Badge tone={activity.status}>{titleCase(activity.status)}</Badge>
      </div>

      <CardDescription>
        {activity.description
          ? activity.description.length > 150
            ? `${activity.description.slice(0, 150)}…`
            : activity.description
          : 'Details will be published by the facilitator soon.'}
      </CardDescription>

      <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-600 dark:text-slate-300">
        {activity.category && <Badge>{titleCase(activity.category)}</Badge>}
        <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">
          Starts {formatDate(activity.startDate)}
        </span>
        <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">
          {activity.enrolledCount} enrolled
          {activity.capacity ? ` / ${activity.capacity}` : ''}
        </span>
        {activity.level === 'TWO' && (
          <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">
            Teams of {activity.minTeamSize}-{activity.maxTeamSize}
          </span>
        )}
      </div>

      {enrollment && (
        <p className="mt-3">
          <Badge tone={enrollment.status}>Your status: {titleCase(enrollment.status)}</Badge>
        </p>
      )}

      {error && (
        <p role="alert" className="mt-3 rounded-lg bg-rose-50 px-3 py-2 text-xs text-rose-700 dark:bg-rose-500/10 dark:text-rose-200">
          {error}
        </p>
      )}

      <div className="mt-4 flex flex-wrap gap-2 pt-2">
        <LinkButton href={`/exchanges/${activity.activityId}`} size="sm" variant="secondary">
          View details
        </LinkButton>
        {canEnroll && (
          <Button size="sm" loading={enroll.isPending} onClick={() => enroll.mutate()}>
            Enrol now
          </Button>
        )}
      </div>
    </Card>
  );
}

export default function ExchangesPage() {
  const { user } = useAuth();
  const [query, setQuery] = useState('');
  const [level, setLevel] = useState('');
  const [page, setPage] = useState(0);

  const listQuery = useMemo(
    () => withQuery('/api/v1/activities', { q: query, page, size: 9 }),
    [query, page],
  );

  const activities = useQuery({
    queryKey: ['activities', listQuery],
    queryFn: () => api<PageResponse<Activity>>(listQuery),
    placeholderData: (previous) => previous,
  });

  const enrollments = useQuery({
    queryKey: ['enrollments', 'mine'],
    queryFn: () => api<Enrollment[]>('/api/v1/enrollments/me'),
    enabled: user?.role === 'PARTICIPANT',
  });

  const byActivity = new Map((enrollments.data ?? []).map((item) => [item.activityId, item]));
  const visible = (activities.data?.items ?? []).filter((item) => !level || item.level === level);

  return (
    <>
      <PageHeader
        title="Virtual exchanges"
        description="Level One sessions are open to every participant; Level Two programmes host the teams and impact projects."
        actions={
          <LinkButton href="/dashboard" size="sm" variant="secondary">
            Back to dashboard
          </LinkButton>
        }
      />

      <form
        className="mb-6 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 sm:grid-cols-3 dark:border-slate-800 dark:bg-slate-900"
        onSubmit={(event) => event.preventDefault()}
        role="search"
        aria-label="Filter activities"
      >
        <Field label="Search" htmlFor="filter-q" className="sm:col-span-2">
          <Input
            id="filter-q"
            type="search"
            placeholder="Title or description"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setPage(0);
            }}
          />
        </Field>
        <Field label="Level" htmlFor="filter-level">
          <Select
            id="filter-level"
            value={level}
            onChange={(event) => {
              setLevel(event.target.value);
              setPage(0);
            }}
          >
            <option value="">All levels</option>
            <option value="ONE">Level One</option>
            <option value="TWO">Level Two</option>
          </Select>
        </Field>
      </form>

      {activities.isLoading ? (
        <SpinnerRow label="Loading activities…" />
      ) : activities.isError ? (
        <ErrorState
          message={activities.error instanceof ApiError ? activities.error.message : 'Could not load activities.'}
          onRetry={() => activities.refetch()}
        />
      ) : visible.length === 0 ? (
        <EmptyState title="No activities match these filters" description="Try clearing the search or choosing another level." />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {visible.map((activity) => (
              <ActivityCard key={activity.activityId} activity={activity} enrollment={byActivity.get(activity.activityId)} />
            ))}
          </div>

          <nav className="mt-6 flex items-center justify-between gap-3" aria-label="Pagination">
            <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>
              Previous
            </Button>
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Page {(activities.data?.page ?? 0) + 1} of {Math.max(activities.data?.totalPages ?? 1, 1)} ·{' '}
              {activities.data?.totalItems ?? 0} results
            </p>
            <Button
              variant="secondary"
              size="sm"
              disabled={page + 1 >= Math.max(activities.data?.totalPages ?? 1, 1)}
              onClick={() => setPage((value) => value + 1)}
            >
              Next
            </Button>
          </nav>
        </>
      )}
    </>
  );
}

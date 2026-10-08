'use client';

import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { Suspense, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api, withQuery } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatDate, titleCase } from '@/lib/format';
import type { Exchange, PageResponse } from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select } from '@/components/ui/Form';

const levels = ['', 'LEVEL_ONE', 'LEVEL_TWO'];
const topics = ['', 'DIGITAL_LITERACY', 'AI_LITERACY', 'CRITICAL_THINKING', 'MEDIA_LITERACY', 'INTERCULTURAL_COMM'];
const focusAreas = ['', 'EMPLOYMENT', 'EDUCATION', 'AGRICULTURE', 'HEALTH', 'CLIMATE', 'DIGITAL_INCLUSION'];
const statuses = ['', 'OPEN', 'ACTIVE', 'COMPLETED', 'DRAFT', 'CANCELLED'];

function ExchangeCard({ exchange }: { exchange: Exchange }) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const enroll = useMutation({
    mutationFn: () => api(`/api/v1/exchanges/${exchange.id}/enroll`, { method: 'POST' }),
    onSuccess: () => {
      setError(null);
      void queryClient.invalidateQueries({ queryKey: ['exchanges'] });
      void queryClient.invalidateQueries({ queryKey: ['exchange', exchange.id] });
      void queryClient.invalidateQueries({ queryKey: ['enrollments', 'mine'] });
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : 'Could not enrol right now.'),
  });

  return (
    <Card as="article" className="flex flex-col">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-brand-700">
            {exchange.level === 'LEVEL_ONE' ? 'Level One session' : 'Level Two programme'}
          </p>
          <CardTitle className="mt-1">
            <Link href={`/exchanges/${exchange.id}`} className="hover:text-brand-700 hover:underline">
              {exchange.title}
            </Link>
          </CardTitle>
        </div>
        <Badge tone={exchange.status}>{titleCase(exchange.status)}</Badge>
      </div>

      <CardDescription>
        {exchange.description
          ? exchange.description.length > 150
            ? `${exchange.description.slice(0, 150)}…`
            : exchange.description
          : exchange.summaryLabel}
      </CardDescription>

      <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-600">
        <Badge tone={exchange.level}>
          {exchange.topic ? titleCase(exchange.topic) : exchange.focusArea ? titleCase(exchange.focusArea) : 'Exchange'}
        </Badge>
        <span className="rounded-full bg-slate-100 px-2.5 py-1">Starts {formatDate(exchange.startDate)}</span>
        <span className="rounded-full bg-slate-100 px-2.5 py-1">
          {exchange.enrolledCount} enrolled{exchange.teamCount > 0 ? ` · ${exchange.teamCount} teams` : ''}
        </span>
      </div>

      {error && (
        <p role="alert" className="mt-3 rounded-lg bg-rose-50 px-3 py-2 text-xs text-rose-700">
          {error}
        </p>
      )}

      <div className="mt-4 flex flex-wrap gap-2 pt-2">
        <LinkButton href={`/exchanges/${exchange.id}`} size="sm" variant="secondary">
          View details
        </LinkButton>
        {user?.role === 'PARTICIPANT' && exchange.canEnroll && (
          <Button size="sm" loading={enroll.isPending} onClick={() => enroll.mutate()}>
            Enrol now
          </Button>
        )}
      </div>
    </Card>
  );
}

function ExchangesContent() {
  const params = useSearchParams();
  const [filters, setFilters] = useState({
    level: params.get('level') ?? '',
    topic: params.get('topic') ?? '',
    focusArea: params.get('focusArea') ?? '',
    status: params.get('status') ?? '',
    q: params.get('q') ?? '',
    from: '',
    to: '',
  });
  const [page, setPage] = useState(0);

  const query = withQuery('/api/v1/exchanges', { ...filters, page, size: 9 });
  const exchanges = useQuery({
    queryKey: ['exchanges', query],
    queryFn: () => api<PageResponse<Exchange>>(query),
    placeholderData: (previous) => previous,
  });

  function update(key: keyof typeof filters, value: string) {
    setFilters((current) => ({ ...current, [key]: value }));
    setPage(0);
  }

  return (
    <>
      <PageHeader
        title="Virtual exchanges"
        description="Filter by level, topic, focus area, status or date. Level One sessions are open to everyone; Level Two programmes host the teams."
        actions={<LinkButton href="/dashboard" size="sm" variant="secondary">Back to dashboard</LinkButton>}
      />

      <form
        className="mb-6 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 sm:grid-cols-2 lg:grid-cols-4"
        onSubmit={(event) => event.preventDefault()}
        role="search"
        aria-label="Filter exchanges"
      >
        <Field label="Search" htmlFor="filter-q">
          <Input
            id="filter-q"
            type="search"
            placeholder="Title or description"
            value={filters.q}
            onChange={(event) => update('q', event.target.value)}
          />
        </Field>

        <Field label="Level" htmlFor="filter-level">
          <Select id="filter-level" value={filters.level} onChange={(event) => update('level', event.target.value)}>
            {levels.map((value) => (
              <option key={value || 'all'} value={value}>
                {value ? titleCase(value) : 'All levels'}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Level One topic" htmlFor="filter-topic">
          <Select id="filter-topic" value={filters.topic} onChange={(event) => update('topic', event.target.value)}>
            {topics.map((value) => (
              <option key={value || 'all'} value={value}>
                {value ? titleCase(value) : 'Any topic'}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Level Two focus" htmlFor="filter-focus">
          <Select
            id="filter-focus"
            value={filters.focusArea}
            onChange={(event) => update('focusArea', event.target.value)}
          >
            {focusAreas.map((value) => (
              <option key={value || 'all'} value={value}>
                {value ? titleCase(value) : 'Any focus area'}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Status" htmlFor="filter-status">
          <Select id="filter-status" value={filters.status} onChange={(event) => update('status', event.target.value)}>
            {statuses.map((value) => (
              <option key={value || 'all'} value={value}>
                {value ? titleCase(value) : 'Any status'}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="From" htmlFor="filter-from">
          <Input
            id="filter-from"
            type="date"
            value={filters.from}
            onChange={(event) => update('from', event.target.value)}
          />
        </Field>

        <Field label="To" htmlFor="filter-to">
          <Input
            id="filter-to"
            type="date"
            value={filters.to}
            onChange={(event) => update('to', event.target.value)}
          />
        </Field>

        <div className="flex items-end">
          <Button
            variant="secondary"
            size="sm"
            className="w-full"
            onClick={() => {
              setFilters({ level: '', topic: '', focusArea: '', status: '', q: '', from: '', to: '' });
              setPage(0);
            }}
          >
            Clear filters
          </Button>
        </div>
      </form>

      {exchanges.isLoading ? (
        <SpinnerRow label="Loading exchanges…" />
      ) : exchanges.isError ? (
        <ErrorState
          message={exchanges.error instanceof ApiError ? exchanges.error.message : 'Could not load exchanges.'}
          onRetry={() => exchanges.refetch()}
        />
      ) : (exchanges.data?.content.length ?? 0) === 0 ? (
        <EmptyState
          title="No exchanges match these filters"
          description="Try widening the date range or clearing the topic filter."
        />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {(exchanges.data?.content ?? []).map((exchange) => (
              <ExchangeCard key={exchange.id} exchange={exchange} />
            ))}
          </div>

          <nav className="mt-6 flex items-center justify-between gap-3" aria-label="Pagination">
            <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>
              Previous
            </Button>
            <p className="text-sm text-slate-600">
              Page {(exchanges.data?.page ?? 0) + 1} of {Math.max(exchanges.data?.totalPages ?? 1, 1)} ·{' '}
              {exchanges.data?.totalElements ?? 0} results
            </p>
            <Button
              variant="secondary"
              size="sm"
              disabled={exchanges.data?.last ?? true}
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

export default function ExchangesPage() {
  return (
    <Suspense fallback={<SpinnerRow label="Loading exchanges…" />}>
      <ExchangesContent />
    </Suspense>
  );
}

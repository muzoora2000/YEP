'use client';

import Link from 'next/link';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatDate, titleCase } from '@/lib/format';
import type { Team } from '@/lib/types';
import { useQuery } from '@tanstack/react-query';

import { Badge } from '@/components/ui/Badge';
import { LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';

export default function TeamsPage() {
  const { user } = useAuth();

  const teams = useQuery({
    queryKey: ['teams', 'mine'],
    queryFn: () => api<Team[]>('/api/v1/teams'),
  });

  return (
    <>
      <PageHeader
        title="My teams"
        description="Teams you belong to inside Level Two programmes. Create a team from the exchange page."
        actions={<LinkButton href="/exchanges?level=LEVEL_TWO" size="sm" variant="secondary">Level Two programmes</LinkButton>}
      />

      {teams.isLoading ? (
        <SpinnerRow label="Loading teams…" />
      ) : teams.isError ? (
        <ErrorState
          message={teams.error instanceof ApiError ? teams.error.message : 'Could not load teams.'}
          onRetry={() => teams.refetch()}
        />
      ) : (teams.data ?? []).length === 0 ? (
        <EmptyState
          title="You are not on a team yet"
          description="Open a Level Two programme, create a team and start an impact project."
          action={<LinkButton href="/exchanges?level=LEVEL_TWO" size="sm">Find a programme</LinkButton>}
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {(teams.data ?? []).map((team) => (
            <Card key={team.id} as="article">
              <CardTitle>
                <Link href={`/teams/${team.id}`} className="hover:text-brand-700 dark:hover:text-brand-300 hover:underline">
                  {team.name}
                </Link>
              </CardTitle>
              <CardDescription>
                {team.description || team.exchangeTitle || 'Level Two team'}
              </CardDescription>
              <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-600 dark:text-slate-300">
                <span className="rounded-full bg-slate-100 dark:bg-slate-800 px-2.5 py-1">
                  {team.memberCount} member{team.memberCount === 1 ? '' : 's'}
                </span>
                <span className="rounded-full bg-slate-100 dark:bg-slate-800 px-2.5 py-1">Since {formatDate(team.createdAt)}</span>
                {team.mentorName && <Badge tone="MENTOR">Mentor {team.mentorName}</Badge>}
              </div>
              <div className="mt-4 flex items-center justify-between gap-2">
                <Badge tone={team.impactProjectStatus ?? 'DRAFT'}>
                  {team.impactProjectStatus ? titleCase(team.impactProjectStatus) : 'No project yet'}
                </Badge>
                <Link href={`/teams/${team.id}`} className="text-xs font-semibold text-brand-700 dark:text-brand-300 hover:underline">
                  Open →
                </Link>
              </div>
            </Card>
          ))}
        </div>
      )}
    </>
  );
}

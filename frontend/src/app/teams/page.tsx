'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { titleCase } from '@/lib/format';
import type { Team } from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';

export default function TeamsPage() {
  const { user } = useAuth();
  const isMentor = user?.role === 'MENTOR';

  const teams = useQuery({
    queryKey: ['teams', isMentor ? 'mentored' : 'mine'],
    queryFn: () => api<Team[]>(isMentor ? '/api/v1/teams/mentored' : '/api/v1/teams/my'),
    enabled: !!user && (isMentor || user.role === 'PARTICIPANT'),
  });

  return (
    <>
      <PageHeader
        title={isMentor ? 'Mentored teams' : 'My teams'}
        description={
          isMentor
            ? 'Teams you guide through their impact project.'
            : 'Teams you belong to inside Level Two activities. Create or join a team from the activity page.'
        }
        actions={
          <LinkButton href="/exchanges" size="sm" variant="secondary">
            Browse Level Two activities
          </LinkButton>
        }
      />

      {!user || (user.role !== 'PARTICIPANT' && user.role !== 'MENTOR') ? (
        <EmptyState
          title="Teams are listed per activity"
          description="Open a Level Two activity to see its teams, or use the admin and facilitator tools to manage them."
          action={
            <LinkButton href="/exchanges" size="sm">
              Browse activities
            </LinkButton>
          }
        />
      ) : teams.isLoading ? (
        <SpinnerRow label="Loading teams…" />
      ) : teams.isError ? (
        <ErrorState
          message={teams.error instanceof ApiError ? teams.error.message : 'Could not load teams.'}
          onRetry={() => teams.refetch()}
        />
      ) : (teams.data ?? []).length === 0 ? (
        <EmptyState
          title={isMentor ? 'No teams assigned yet' : 'You are not on a team yet'}
          description={
            isMentor
              ? 'A facilitator can assign you to a team from the team page.'
              : 'Open a Level Two activity, enrol, then create or join a team and start an impact project.'
          }
          action={
            <LinkButton href="/exchanges" size="sm">
              Find a programme
            </LinkButton>
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {(teams.data ?? []).map((team) => (
            <Card key={team.teamId} as="article">
              <CardTitle>
                <Link href={`/teams/${team.teamId}`} className="hover:text-brand-700 hover:underline dark:hover:text-brand-300">
                  {team.teamName}
                </Link>
              </CardTitle>
              <CardDescription>{team.activityTitle}</CardDescription>
              <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-600 dark:text-slate-300">
                <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">
                  {team.memberCount}/{team.maxTeamSize} members
                </span>
                {team.mentorName && <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">Mentor {team.mentorName}</span>}
              </div>
              <div className="mt-4 flex items-center justify-between gap-2">
                <Badge tone={team.status}>{titleCase(team.status)}</Badge>
                <Link href={`/teams/${team.teamId}`} className="text-xs font-semibold text-brand-700 hover:underline dark:text-brand-300">
                  Open
                </Link>
              </div>
            </Card>
          ))}
        </div>
      )}
    </>
  );
}

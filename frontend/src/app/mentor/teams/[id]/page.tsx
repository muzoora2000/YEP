'use client';

import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { ArrowLeft, Users } from 'lucide-react';
import { api } from '@/lib/api';
import { PageHeader, SpinnerRow, EmptyState, ErrorState } from '@/components/ui/Feedback';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { RequireAuth } from '@/components/RequireAuth';

interface TeamMemberResponse { memberId: number; youthId: number; name: string; role: string; }
interface TeamResponse {
  teamId: number; teamName: string; activityId: number; activityTitle: string;
  status: string; memberCount: number; projectId: number | null; projectTitle: string | null;
  members: TeamMemberResponse[];
}

function TeamDetail({ teamId }: { teamId: number }) {
  const { data: team, isLoading, error } = useQuery<TeamResponse>({
    queryKey: ['team', teamId],
    queryFn: () => api<TeamResponse>(`/api/v1/teams/${teamId}`),
  });

  if (isLoading) return <SpinnerRow />;
  if (error || !team) return <ErrorState message="Could not load team." />;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <LinkButton href="/mentor" size="sm" variant="secondary">
          <ArrowLeft className="h-4 w-4" /> Back
        </LinkButton>
        <PageHeader title={team.teamName} description={team.activityTitle} />
      </div>

      <div className="flex flex-wrap gap-2">
        <Badge tone={team.status}>{team.status}</Badge>
        <span className="text-sm text-slate-500">{team.memberCount} member{team.memberCount !== 1 ? 's' : ''}</span>
      </div>

      <Card className="p-6">
        <h2 className="mb-4 flex items-center gap-2 text-base font-semibold">
          <Users className="h-5 w-5" /> Members
        </h2>
        {(!team.members || team.members.length === 0)
          ? <EmptyState title="No members" description="This team has no members yet." />
          : (
            <ul className="divide-y divide-slate-100 dark:divide-slate-700">
              {team.members.map(m => (
                <li key={m.memberId} className="flex items-center justify-between py-3">
                  <span className="font-medium">{m.name}</span>
                  <Badge tone={m.role}>{m.role}</Badge>
                </li>
              ))}
            </ul>
          )
        }
      </Card>

      {team.projectId
        ? <div className="text-center">
            <LinkButton href={`/mentor/projects/${team.projectId}`}>Review Impact Project</LinkButton>
          </div>
        : <EmptyState title="No project yet" description="This team has not started an impact project." />
      }
    </div>
  );
}

export default function MentorTeamPage({ params }: { params: { id: string } }) {
  return (
    <RequireAuth roles={['MENTOR', 'ADMIN']}>
      <TeamDetail teamId={Number(params.id)} />
    </RequireAuth>
  );
}

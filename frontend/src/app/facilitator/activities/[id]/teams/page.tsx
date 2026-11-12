'use client';

import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, Users } from 'lucide-react';
import { api } from '@/lib/api';
import { PageHeader, SpinnerRow, EmptyState, ErrorState } from '@/components/ui/Feedback';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { LinkButton } from '@/components/ui/Button';
import { RequireAuth } from '@/components/RequireAuth';
import { ActivityNav } from '@/components/ActivityNav';

interface TeamSummary {
  teamId: number; teamName: string; status: string; memberCount: number; projectId: number | null;
}

function TeamsView({ activityId }: { activityId: number }) {
  const { data: teams, isLoading, error } = useQuery<TeamSummary[]>({
    queryKey: ['activity-teams', activityId],
    queryFn: () => api<TeamSummary[]>(`/api/v1/teams/activity/${activityId}`),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Teams" description={`Activity #${activityId} — Level 2`}
        actions={
          <LinkButton href="/facilitator" size="sm" variant="secondary"><ArrowLeft className="h-4 w-4" /> Back</LinkButton>
        }
      />

      <ActivityNav activityId={activityId} active="teams" />

      {isLoading && <SpinnerRow />}
      {error && <ErrorState message="Could not load teams." />}
      {!isLoading && teams?.length === 0 && (
        <EmptyState
          title="No teams yet"
          description="Teams are formed by participants enrolling in this Level 2 activity." />
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {teams?.map(t => (
          <Card key={t.teamId} className="space-y-3 p-4">
            <div className="flex items-start justify-between gap-2">
              <div className="flex items-center gap-2">
                <Users className="h-4 w-4 text-slate-400" />
                <p className="font-semibold">{t.teamName}</p>
              </div>
              <Badge tone={t.status}>{t.status}</Badge>
            </div>
            <p className="text-sm text-slate-500">{t.memberCount} member{t.memberCount !== 1 ? 's' : ''}</p>
            {t.projectId && (
              <LinkButton href={`/projects/${t.projectId}`} size="sm" variant="secondary">View project</LinkButton>
            )}
          </Card>
        ))}
      </div>
    </div>
  );
}

export default function ActivityTeamsPage({ params }: { params: { id: string } }) {
  return (
    <RequireAuth roles={['FACILITATOR', 'ADMIN']}>
      <TeamsView activityId={Number(params.id)} />
    </RequireAuth>
  );
}

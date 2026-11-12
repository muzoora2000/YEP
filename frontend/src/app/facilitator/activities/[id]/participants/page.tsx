'use client';

import { useQuery } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { api } from '@/lib/api';
import { PageHeader, SpinnerRow, EmptyState, ErrorState } from '@/components/ui/Feedback';
import { Badge } from '@/components/ui/Badge';
import { LinkButton } from '@/components/ui/Button';
import { RequireAuth } from '@/components/RequireAuth';
import { ActivityNav } from '@/components/ActivityNav';

interface ParticipantRow {
  youthId: number; name: string; country: string | null; status: string;
  attendancePercent: number | null; resultsPercent: number | null; lastActivityAt: string | null;
}

function pct(value: number | null): string {
  return value === null ? '—' : `${Math.round(value)}%`;
}

function ParticipantsList({ activityId }: { activityId: number }) {
  const { data: participants, isLoading, error } = useQuery<ParticipantRow[]>({
    queryKey: ['roster', activityId],
    queryFn: () => api<ParticipantRow[]>(`/api/v1/activities/${activityId}/participants`),
  });

  const counts = participants?.reduce((acc, e) => {
    acc[e.status] = (acc[e.status] ?? 0) + 1;
    return acc;
  }, {} as Record<string, number>) ?? {};

  return (
    <div className="space-y-6">
      <PageHeader title="Participants" description={`Activity #${activityId}`}
        actions={
          <LinkButton href="/facilitator" size="sm" variant="secondary"><ArrowLeft className="h-4 w-4" /> Back</LinkButton>
        }
      />

      <ActivityNav activityId={activityId} active="participants" />

      {/* Summary badges */}
      <div className="flex flex-wrap gap-2">
        {Object.entries(counts).map(([status, count]) => (
          <span key={status} className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-sm dark:bg-slate-800">
            <Badge tone={status}>{status}</Badge>
            <span className="font-semibold">{count}</span>
          </span>
        ))}
      </div>

      {isLoading && <SpinnerRow />}
      {error && <ErrorState message="Could not load participants." />}
      {!isLoading && participants?.length === 0 && <EmptyState title="No enrollments" description="No participants enrolled yet." />}

      {participants && participants.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-700">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-50 dark:bg-slate-800">
              <tr>
                {['Name', 'Country', 'Status', 'Attendance', 'Results', 'Last activity'].map(h => (
                  <th key={h} className="px-4 py-2 text-left font-medium text-slate-600 dark:text-slate-300">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
              {participants.map(e => (
                <tr key={e.youthId} className="bg-white dark:bg-slate-900">
                  <td className="px-4 py-3 font-medium">{e.name}</td>
                  <td className="px-4 py-3 text-slate-500">{e.country ?? '—'}</td>
                  <td className="px-4 py-3"><Badge tone={e.status}>{e.status}</Badge></td>
                  <td className="px-4 py-3 text-slate-500">{pct(e.attendancePercent)}</td>
                  <td className="px-4 py-3 text-slate-500">{pct(e.resultsPercent)}</td>
                  <td className="px-4 py-3 text-slate-500">
                    {e.lastActivityAt ? new Date(e.lastActivityAt).toLocaleDateString() : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default function ParticipantsPage({ params }: { params: { id: string } }) {
  return (
    <RequireAuth roles={['FACILITATOR', 'ADMIN']}>
      <ParticipantsList activityId={Number(params.id)} />
    </RequireAuth>
  );
}

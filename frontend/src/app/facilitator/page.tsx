'use client';

import { useQuery } from '@tanstack/react-query';
import { BookOpen, Plus, Users, FileText, Video, CheckSquare } from 'lucide-react';
import { api } from '@/lib/api';
import { PageHeader, SpinnerRow, EmptyState, ErrorState } from '@/components/ui/Feedback';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { RequireAuth } from '@/components/RequireAuth';

interface ActivitySummary {
  activityId: number; title: string; level: string; status: string;
  startDate: string; endDate: string | null; enrolledCount: number;
}
interface PagedResponse { items: ActivitySummary[]; totalItems: number; }

function FacilitatorDashboard() {
  const { data: paged, isLoading, error } = useQuery<PagedResponse>({
    queryKey: ['facilitator-activities'],
    queryFn: () => api<PagedResponse>('/api/v1/facilitator/activities?size=50&sort=activityId'),
  });
  const activities = paged?.items ?? [];
  const published = activities.filter(a => a.status === 'PUBLISHED').length;
  const draft = activities.filter(a => a.status === 'DRAFT').length;

  return (
    <div className="space-y-8">
      <PageHeader
        title="Facilitator Dashboard"
        description="Manage your exchanges, materials, sessions and assessments."
        actions={
          <LinkButton href="/facilitator/activities/new">
            <Plus className="h-4 w-4" /> New activity
          </LinkButton>
        }
      />

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard label="Total" value={activities.length} />
        <StatCard label="Published" value={published} accent />
        <StatCard label="Drafts" value={draft} />
        <StatCard label="Total enrolled" value={activities.reduce((a, x) => a + (x.enrolledCount ?? 0), 0)} />
      </div>

      {/* Activity list */}
      <section>
        <h2 className="mb-3 text-lg font-semibold">Your activities</h2>
        {isLoading && <SpinnerRow />}
        {error && <ErrorState message="Could not load activities." />}
        {!isLoading && activities.length === 0 && (
          <EmptyState title="No activities yet" description="Create your first activity to get started."
            action={<LinkButton href="/facilitator/activities/new">Create activity</LinkButton>} />
        )}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {activities.map(a => (
            <Card key={a.activityId} className="space-y-3 p-4">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold leading-snug">{a.title}</p>
                  <p className="mt-0.5 text-xs text-slate-500">
                    Level {a.level === 'ONE' ? '1' : '2'} · {a.startDate}
                    {a.endDate ? ` → ${a.endDate}` : ''}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">{a.enrolledCount ?? 0} enrolled</p>
                </div>
                <Badge tone={a.status}>{a.status}</Badge>
              </div>
              <div className="flex flex-wrap gap-1.5">
                <LinkButton href={`/facilitator/activities/${a.activityId}/materials`} size="sm" variant="secondary">
                  <FileText className="h-3 w-3" /><span className="sr-only">Materials</span>
                </LinkButton>
                <LinkButton href={`/facilitator/activities/${a.activityId}/sessions`} size="sm" variant="secondary">
                  <Video className="h-3 w-3" /><span className="sr-only">Sessions</span>
                </LinkButton>
                <LinkButton href={`/facilitator/activities/${a.activityId}/assessments`} size="sm" variant="secondary">
                  <CheckSquare className="h-3 w-3" /><span className="sr-only">Assessments</span>
                </LinkButton>
                <LinkButton href={`/facilitator/activities/${a.activityId}/participants`} size="sm" variant="secondary">
                  <Users className="h-3 w-3" /><span className="sr-only">Participants</span>
                </LinkButton>
                {a.level === 'TWO' && (
                  <LinkButton href={`/facilitator/activities/${a.activityId}/teams`} size="sm" variant="secondary">
                    <BookOpen className="h-3 w-3" /><span className="sr-only">Teams</span>
                  </LinkButton>
                )}
              </div>
            </Card>
          ))}
        </div>
      </section>
    </div>
  );
}

function StatCard({ label, value, accent }: { label: string; value: number; accent?: boolean }) {
  return (
    <Card className="p-4">
      <p className={`text-2xl font-bold ${accent ? 'text-brand-600 dark:text-brand-400' : 'text-slate-900 dark:text-slate-50'}`}>
        {value}
      </p>
      <p className="text-xs text-slate-500">{label}</p>
    </Card>
  );
}

export default function FacilitatorPage() {
  return (
    <RequireAuth roles={['FACILITATOR', 'ADMIN']}>
      <FacilitatorDashboard />
    </RequireAuth>
  );
}

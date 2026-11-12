'use client';

import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { Users, Folder, Clock } from 'lucide-react';
import { api } from '@/lib/api';
import { PageHeader, SpinnerRow, EmptyState, ErrorState } from '@/components/ui/Feedback';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { RequireAuth } from '@/components/RequireAuth';

interface TeamResponse {
  teamId: number; teamName: string; activityId: number; activityTitle: string;
  status: string; memberCount: number; projectId: number | null; projectTitle: string | null;
}
interface ProjectResponse {
  projectId: number; title: string; challengeArea: string; status: string;
  teamId: number; teamName: string;
}

function MentorDashboard() {
  const { data: teams, isLoading: loadingTeams, error: teamsError } = useQuery<TeamResponse[]>({
    queryKey: ['mentor-teams'],
    queryFn: () => api<TeamResponse[]>('/api/v1/teams/mentored'),
  });

  const { data: projects, isLoading: loadingProjects, error: projectsError } = useQuery<ProjectResponse[]>({
    queryKey: ['mentor-projects'],
    queryFn: () => api<ProjectResponse[]>('/api/v1/projects/mentored'),
  });

  const pendingReviews = projects?.filter(p =>
    p.status === 'SUBMITTED' || p.status === 'UNDER_REVIEW'
  ) ?? [];

  function statusTone(s: string) {
    if (s === 'APPROVED' || s === 'COMPLETED') return 'green';
    if (s === 'CHANGES_REQUESTED') return 'red';
    if (s === 'SUBMITTED' || s === 'UNDER_REVIEW') return 'yellow';
    return 'default';
  }

  return (
    <div className="space-y-8">
      <PageHeader title="Mentor Dashboard" description="Guide your teams and review their impact projects." />

      {/* Stats strip */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
        <StatCard icon={<Users className="h-5 w-5" />} label="Teams assigned" value={teams?.length ?? 0} />
        <StatCard icon={<Folder className="h-5 w-5" />} label="Projects" value={projects?.length ?? 0} />
        <StatCard icon={<Clock className="h-5 w-5 text-yellow-600" />} label="Awaiting review" value={pendingReviews.length} />
      </div>

      {/* Teams */}
      <section>
        <h2 className="mb-3 text-lg font-semibold">My teams</h2>
        {loadingTeams && <SpinnerRow />}
        {teamsError && <ErrorState message="Could not load teams." />}
        {!loadingTeams && teams?.length === 0 && (
          <EmptyState title="No teams yet" description="You have not been assigned to any teams yet." />
        )}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {teams?.map(team => (
            <Card key={team.teamId} className="p-4 hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold text-slate-900 dark:text-slate-50">{team.teamName}</p>
                  <p className="mt-0.5 text-sm text-slate-500">{team.activityTitle}</p>
                  <p className="mt-1 text-sm text-slate-500">{team.memberCount} member{team.memberCount !== 1 ? 's' : ''}</p>
                </div>
                <Badge tone={team.status}>{team.status}</Badge>
              </div>
              <div className="mt-4 flex gap-2">
                <LinkButton href={`/mentor/teams/${team.teamId}`} size="sm" variant="secondary">View team</LinkButton>
                {team.projectId && (
                  <LinkButton href={`/mentor/projects/${team.projectId}`} size="sm" variant="secondary">Review project</LinkButton>
                )}
              </div>
            </Card>
          ))}
        </div>
      </section>

      {/* Projects pending review */}
      {pendingReviews.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-semibold text-yellow-700 dark:text-yellow-400">
            Projects awaiting your review ({pendingReviews.length})
          </h2>
          <div className="grid gap-4 sm:grid-cols-2">
            {pendingReviews.map(p => (
              <Card key={p.projectId} className="p-4">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-semibold">{p.title}</p>
                    <p className="text-sm text-slate-500">{p.challengeArea} · {p.teamName}</p>
                  </div>
                  <Badge tone={p.status}>{p.status.replace('_', ' ')}</Badge>
                </div>
                <LinkButton href={`/mentor/projects/${p.projectId}`} size="sm" className="mt-3">Review now</LinkButton>
              </Card>
            ))}
          </div>
        </section>
      )}

      {/* All projects table */}
      <section>
        <h2 className="mb-3 text-lg font-semibold">All projects</h2>
        {loadingProjects && <SpinnerRow />}
        {projectsError && <ErrorState message="Could not load projects." />}
        {!loadingProjects && projects?.length === 0 && (
          <EmptyState title="No projects yet" description="Your teams have not started impact projects yet." />
        )}
        {projects && projects.length > 0 && (
          <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-700">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50 dark:bg-slate-800">
                <tr>
                  {['Project', 'Challenge area', 'Team', 'Status', ''].map(h => (
                    <th key={h} className="px-4 py-2 text-left font-medium text-slate-600 dark:text-slate-300">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                {projects.map(p => (
                  <tr key={p.projectId} className="bg-white dark:bg-slate-900">
                    <td className="px-4 py-3 font-medium">{p.title}</td>
                    <td className="px-4 py-3 text-slate-500">{p.challengeArea}</td>
                    <td className="px-4 py-3">{p.teamName}</td>
                    <td className="px-4 py-3"><Badge tone={p.status}>{p.status.replace('_', ' ')}</Badge></td>
                    <td className="px-4 py-3">
                      <LinkButton href={`/mentor/projects/${p.projectId}`} size="sm" variant="secondary">Review</LinkButton>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function StatCard({ icon, label, value }: { icon: React.ReactNode; label: string; value: number }) {
  return (
    <Card className="flex items-center gap-3 p-4">
      <span className="rounded-lg bg-brand-100 p-2 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300">{icon}</span>
      <div>
        <p className="text-2xl font-bold text-slate-900 dark:text-slate-50">{value}</p>
        <p className="text-xs text-slate-500">{label}</p>
      </div>
    </Card>
  );
}

export default function MentorPage() {
  return (
    <RequireAuth roles={['MENTOR', 'ADMIN']}>
      <MentorDashboard />
    </RequireAuth>
  );
}

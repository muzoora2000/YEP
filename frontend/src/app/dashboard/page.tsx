'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';

import { RequireAuth } from '@/components/RequireAuth';
import { LinkButton } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatDate, titleCase } from '@/lib/format';
import type { Progress, Team, UnreadCount } from '@/lib/types';

function Stat({ label, value, href }: { label: string; value: string | number; href?: string }) {
  const body = (
    <>
      <p className="text-2xl font-bold text-slate-900 dark:text-slate-100">{value}</p>
      <p className="mt-1 text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</p>
    </>
  );
  return (
    <Card className="!p-4">
      {href ? (
        <Link href={href} className="-m-2 block rounded-lg p-2 hover:bg-slate-50 dark:hover:bg-slate-800">
          {body}
        </Link>
      ) : (
        body
      )}
    </Card>
  );
}

function DashboardContent() {
  const { user, isStaff } = useAuth();

  const progress = useQuery({
    queryKey: ['progress', 'mine'],
    queryFn: () => api<Progress[]>('/api/v1/progress/me'),
  });

  const teams = useQuery({
    queryKey: ['teams', 'mine'],
    queryFn: () => api<Team[]>('/api/v1/teams/my'),
    enabled: user?.role === 'PARTICIPANT',
  });

  const mentoredTeams = useQuery({
    queryKey: ['teams', 'mentored'],
    queryFn: () => api<Team[]>('/api/v1/teams/mentored'),
    enabled: user?.role === 'MENTOR',
  });

  const unread = useQuery({
    queryKey: ['unread-count'],
    queryFn: () => api<UnreadCount>('/api/v1/notifications/unread-count'),
  });

  if (progress.isLoading || teams.isLoading) {
    return <SpinnerRow label="Loading your dashboard…" />;
  }

  if (progress.isError) {
    return <ErrorState message="Could not load your progress." onRetry={() => progress.refetch()} />;
  }

  const certificates = (progress.data ?? []).flatMap((item) => item.certificates);
  const enrolled = (progress.data ?? []).filter((item) => item.enrollmentStatus !== 'CANCELLED');
  const allTeams = [...(teams.data ?? []), ...(mentoredTeams.data ?? [])];

  return (
    <>
      <PageHeader
        title={`Welcome, ${user?.name?.split(' ')[0] ?? 'there'}`}
        description={
          isStaff
            ? 'Your facilitation, mentoring and assessment workspace.'
            : 'Your activities, teams and certificates in one place.'
        }
        actions={
          <>
            <LinkButton href="/exchanges" size="sm">
              Browse activities
            </LinkButton>
            <LinkButton href="/notifications" size="sm" variant="secondary">
              Notifications
              {unread.data && unread.data.unread > 0 && (
                <span className="rounded-full bg-rose-600 px-1.5 text-[10px] font-bold text-white">
                  {unread.data.unread}
                </span>
              )}
            </LinkButton>
          </>
        }
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Enrolments" value={enrolled.length} href="/exchanges" />
        <Stat label="My teams" value={teams.data?.length ?? 0} href="/teams" />
        <Stat label="Certificates" value={certificates.length} href="/assessments" />
        <Stat label="Unread notifications" value={unread.data?.unread ?? 0} href="/notifications" />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <section aria-labelledby="my-activities">
          <h2 id="my-activities" className="mb-3 text-base font-semibold text-slate-900 dark:text-slate-100">
            My activities
          </h2>
          {enrolled.length === 0 ? (
            <EmptyState
              title="No enrolments yet"
              description="Enrol in a Level One session or apply for a Level Two programme to get started."
              action={
                <LinkButton href="/exchanges" size="sm">
                  Find an activity
                </LinkButton>
              }
            />
          ) : (
            <ul className="space-y-3">
              {enrolled.slice(0, 5).map((item) => (
                <li key={item.activityId}>
                  <Card className="!p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <Link
                          href={`/exchanges/${item.activityId}`}
                          className="font-semibold text-slate-900 hover:text-brand-700 hover:underline dark:text-slate-100 dark:hover:text-brand-300"
                        >
                          {item.activityTitle}
                        </Link>
                        <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                          Attendance {item.attendancePercent === null ? '—' : `${Math.round(item.attendancePercent)}%`} · Assessment average{' '}
                          {Math.round(item.assessmentAverage)}%
                        </p>
                      </div>
                      <Badge tone={item.enrollmentStatus}>{titleCase(item.enrollmentStatus)}</Badge>
                    </div>
                  </Card>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section aria-labelledby="my-teams">
          <h2 id="my-teams" className="mb-3 text-base font-semibold text-slate-900 dark:text-slate-100">
            My teams
          </h2>
          {allTeams.length === 0 ? (
            <EmptyState
              title="You are not on a team yet"
              description="Inside a Level Two activity you can create or join a team and start an impact project."
              action={
                <LinkButton href="/teams" size="sm" variant="secondary">
                  See teams
                </LinkButton>
              }
            />
          ) : (
            <ul className="space-y-3">
              {allTeams.map((team) => (
                <li key={team.teamId}>
                  <Card className="!p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <Link
                          href={`/teams/${team.teamId}`}
                          className="font-semibold text-slate-900 hover:text-brand-700 hover:underline dark:text-slate-100 dark:hover:text-brand-300"
                        >
                          {team.teamName}
                        </Link>
                        <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                          {team.activityTitle} · {team.memberCount}/{team.maxTeamSize} members
                          {team.mentorName ? ` · mentor ${team.mentorName}` : ''}
                        </p>
                      </div>
                      <Badge tone={team.status}>{titleCase(team.status)}</Badge>
                    </div>
                  </Card>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section aria-labelledby="certificates" className="mt-6">
        <h2 id="certificates" className="mb-3 text-base font-semibold text-slate-900 dark:text-slate-100">
          My certificates
        </h2>
        {certificates.length === 0 ? (
          <EmptyState
            title="No certificates yet"
            description="Pass the assessment and attendance thresholds for an activity and your certificate is issued automatically."
          />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {certificates.map((certificate) => (
              <Card key={certificate.certificateId} className="!p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-900 dark:text-slate-100">{certificate.activityTitle}</p>
                    <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                      {certificate.achievement} · {formatDate(certificate.issueDate)}
                    </p>
                  </div>
                  <Link
                    href={`/certificates/${certificate.certificateNumber}`}
                    className="shrink-0 text-xs font-semibold text-brand-700 hover:underline dark:text-brand-300"
                  >
                    Verify
                  </Link>
                </div>
              </Card>
            ))}
          </div>
        )}
      </section>

      {isStaff && (
        <section aria-labelledby="staff" className="mt-6">
          <Card>
            <CardTitle id="staff">Staff workspace</CardTitle>
            <CardDescription>
              Manage activities, sessions, assessments and certificates, or review teams and projects.
            </CardDescription>
            <div className="mt-4 flex flex-wrap gap-2">
              <LinkButton href="/assessments" size="sm">
                Assessments
              </LinkButton>
              <LinkButton href="/projects" size="sm" variant="secondary">
                Projects
              </LinkButton>
              {user?.role === 'ADMIN' && (
                <LinkButton href="/admin" size="sm" variant="secondary">
                  Admin panel
                </LinkButton>
              )}
            </div>
          </Card>
        </section>
      )}
    </>
  );
}

export default function DashboardPage() {
  return (
    <RequireAuth>
      <DashboardContent />
    </RequireAuth>
  );
}

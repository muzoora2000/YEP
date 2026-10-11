'use client';

import Link from 'next/link';

import { RequireAuth } from '@/components/RequireAuth';
import { LinkButton } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { api, withQuery } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatDate, titleCase } from '@/lib/format';
import type { Certificate, Enrollment, PageResponse, Team, UnreadCount } from '@/lib/types';
import { useQuery } from '@tanstack/react-query';

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
        <Link href={href} className="block hover:bg-slate-50 dark:hover:bg-slate-800 rounded-lg -m-2 p-2">
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

  const enrollments = useQuery({
    queryKey: ['enrollments', 'mine'],
    queryFn: () => api<Enrollment[]>('/api/v1/enrollments/mine'),
  });

  const teams = useQuery({
    queryKey: ['teams', 'mine'],
    queryFn: () => api<Team[]>('/api/v1/teams'),
  });

  const certificates = useQuery({
    queryKey: ['certificates', 'mine'],
    queryFn: () => api<Certificate[]>('/api/v1/certificates/mine'),
  });

  const unread = useQuery({
    queryKey: ['unread-count'],
    queryFn: () => api<UnreadCount>('/api/v1/messages/unread-count'),
  });

  const staffProjects = useQuery({
    queryKey: ['projects', 'all'],
    queryFn: () => api<PageResponse<unknown>>('/api/v1/projects'),
    enabled: isStaff,
  });

  if (enrollments.isLoading || teams.isLoading) {
    return <SpinnerRow label="Loading your dashboard…" />;
  }

  if (enrollments.isError) {
    return <ErrorState message="Could not load your enrolments." onRetry={() => enrollments.refetch()} />;
  }

  const activeEnrolments = (enrollments.data ?? []).filter(
    (enrollment) => enrollment.completionStatus !== 'WITHDRAWN',
  );

  return (
    <>
      <PageHeader
        title={`WELCOME, ${user?.fullName?.split(' ')[0] ?? 'there'}`}
        description={
          isStaff
            ? 'Your facilitation, assessment and moderation workspace.'
            : 'Your exchanges, teams and certificates in one place.'
        }
        actions={
          <>
            <LinkButton href="/exchanges" size="sm">
              Browse exchanges
            </LinkButton>
            <LinkButton href="/messages" size="sm" variant="secondary">
              Messages
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
        <Stat label="Enrolments" value={activeEnrolments.length} href="/exchanges" />
        <Stat label="My teams" value={teams.data?.length ?? 0} href="/teams" />
        <Stat label="Certificates" value={certificates.data?.length ?? 0} href="/assessments" />
        <Stat label="Unread messages" value={unread.data?.unread ?? 0} href="/messages" />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <section aria-labelledby="my-exchanges">
          <h2 id="my-exchanges" className="mb-3 text-base font-semibold text-slate-900 dark:text-slate-100">
            My exchanges
          </h2>
          {activeEnrolments.length === 0 ? (
            <EmptyState
              title="No enrolments yet"
              description="Enrol in a Level One session or apply for a Level Two programme to get started."
              action={<LinkButton href="/exchanges" size="sm">Find an exchange</LinkButton>}
            />
          ) : (
            <ul className="space-y-3">
              {activeEnrolments.slice(0, 5).map((enrollment) => (
                <li key={enrollment.id}>
                  <Card className="!p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <Link
                          href={`/exchanges/${enrollment.exchangeId}`}
                          className="font-semibold text-slate-900 dark:text-slate-100 hover:text-brand-700 dark:hover:text-brand-300 hover:underline"
                        >
                          {enrollment.exchangeTitle}
                        </Link>
                        <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                          {titleCase(enrollment.level)} · enrolled {formatDate(enrollment.enrolledAt)}
                        </p>
                      </div>
                      <Badge tone={enrollment.completionStatus}>{titleCase(enrollment.completionStatus)}</Badge>
                    </div>
                  </Card>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section aria-labelledby="my-teams">
          <h2 id="my-teams" className="mb-3 text-base font-semibold text-slate-900 dark:text-slate-100">My teams</h2>
          {(teams.data ?? []).length === 0 ? (
            <EmptyState
              title="You are not on a team yet"
              description="Inside a Level Two exchange you can create or join a team and start an impact project."
              action={<LinkButton href="/teams" size="sm" variant="secondary">See teams</LinkButton>}
            />
          ) : (
            <ul className="space-y-3">
              {(teams.data ?? []).map((team) => (
                <li key={team.id}>
                  <Card className="!p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <Link href={`/teams/${team.id}`} className="font-semibold text-slate-900 dark:text-slate-100 hover:text-brand-700 dark:hover:text-brand-300 hover:underline">
                          {team.name}
                        </Link>
                        <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                          {team.exchangeTitle ?? 'Level Two'} · {team.memberCount} member
                          {team.memberCount === 1 ? '' : 's'}
                          {team.mentorName ? ` · mentor ${team.mentorName}` : ''}
                        </p>
                      </div>
                      <Badge tone={team.impactProjectStatus ?? 'DRAFT'}>
                        {team.impactProjectStatus ? titleCase(team.impactProjectStatus) : 'No project'}
                      </Badge>
                    </div>
                  </Card>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section aria-labelledby="certificates" className="mt-6">
        <h2 id="certificates" className="mb-3 text-base font-semibold text-slate-900 dark:text-slate-100">My certificates</h2>
        {(certificates.data ?? []).length === 0 ? (
          <EmptyState
            title="No certificates yet"
            description="Complete a Level One activity or an approved impact project and your certificate is issued automatically."
          />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {(certificates.data ?? []).map((certificate) => (
              <Card key={certificate.id} className="!p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-900 dark:text-slate-100">{certificate.title}</p>
                    <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                      {certificate.relatedActivityOrProject ?? 'YEP'} · {formatDate(certificate.issueDate)}
                    </p>
                  </div>
                  <Link
                    href={`/certificates/${certificate.verificationCode}`}
                    className="shrink-0 text-xs font-semibold text-brand-700 dark:text-brand-300 hover:underline"
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
              {(staffProjects.data?.totalElements ?? 0)} impact project
              {(staffProjects.data?.totalElements ?? 0) === 1 ? '' : 's'} in the pipeline
              {unread.data && unread.data.pendingApprovals > 0
                ? `, ${unread.data.pendingApprovals} AI draft(s) waiting for human approval.`
                : '.'}
            </CardDescription>
            <div className="mt-4 flex flex-wrap gap-2">
              <LinkButton href="/admin" size="sm">Open admin panel</LinkButton>
              <LinkButton href="/assessments" size="sm" variant="secondary">Assessments</LinkButton>
              <LinkButton href="/messages" size="sm" variant="secondary">Announcements</LinkButton>
            </div>
          </Card>
        </section>
      )}

      <p className="mt-8 text-xs text-slate-500 dark:text-slate-400">
        Quick links:{' '}
        <Link className="underline" href={withQuery('/exchanges', { level: 'LEVEL_ONE' })}>
          Level One sessions
        </Link>{' '}
        ·{' '}
        <Link className="underline" href={withQuery('/exchanges', { level: 'LEVEL_TWO' })}>
          Level Two programmes
        </Link>
      </p>
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

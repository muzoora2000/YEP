'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api, withQuery } from '@/lib/api';
import {
  activitySchema,
  adminUserSchema,
  type ActivityValues,
  type AdminUserValues,
} from '@/lib/schemas';
import { formatDate, formatDateTime, titleCase } from '@/lib/format';
import type {
  AccountStatus,
  Activity,
  ActivityStatus,
  AuditLog,
  Dashboard,
  PageResponse,
  PartnerSummary,
  Project,
  Role,
  Setting,
  User,
} from '@/lib/types';
import { RequireAuth } from '@/components/RequireAuth';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';

type Tab = 'OVERVIEW' | 'USERS' | 'ACTIVITIES' | 'PARTNERS' | 'PROJECTS' | 'AUDIT' | 'SETTINGS';

function OverviewPanel() {
  const dashboard = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: () => api<Dashboard>('/api/v1/admin/dashboard'),
  });

  if (dashboard.isLoading) return <SpinnerRow label="Loading platform overview…" />;
  if (dashboard.isError) {
    return (
      <ErrorState
        message={dashboard.error instanceof ApiError ? dashboard.error.message : 'Could not load the overview.'}
        onRetry={() => dashboard.refetch()}
      />
    );
  }

  const data = dashboard.data as Dashboard;
  const stats = [
    ['Total users', data.totalUsers],
    ['Participants', data.participants],
    ['Facilitators', data.facilitators],
    ['Mentors', data.mentors],
    ['Partners', data.partners],
    ['Verified partners', data.verifiedPartners],
    ['Pending accounts', data.pendingUsers],
    ['Activities', data.activities],
    ['Published activities', data.publishedActivities],
    ['Enrollments', data.enrollments],
    ['Teams', data.teams],
    ['Projects', data.projects],
    ['Approved projects', data.approvedProjects],
    ['Certificates', data.certificates],
    ['Opportunities', data.opportunities],
  ];

  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
      {stats.map(([label, value]) => (
        <Card key={label} className="!p-4">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</p>
          <p className="mt-1 text-2xl font-bold text-slate-900 dark:text-slate-100">{value}</p>
        </Card>
      ))}
    </div>
  );
}

function CreateUserForm({ onDone }: { onDone: (message: string) => void }) {
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<AdminUserValues>({
    resolver: zodResolver(adminUserSchema),
    defaultValues: { name: '', email: '', password: '', role: 'PARTICIPANT' },
  });

  const role = watch('role');

  const create = useMutation({
    mutationFn: (values: AdminUserValues) =>
      api<User>('/api/v1/admin/users', {
        method: 'POST',
        body: {
          name: values.name,
          email: values.email,
          password: values.password,
          role: values.role,
          phone: values.phone || null,
          age: values.age ?? null,
          country: values.country || null,
          organisationName: values.organisationName || null,
          organisationType: values.organisationType ?? null,
        },
      }),
    onSuccess: (user) => {
      reset();
      onDone(`Created ${user.name}.`);
      void queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      void queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] });
    },
    onError: (error) => onDone(error instanceof ApiError ? error.message : 'Could not create the account.'),
  });

  return (
    <Card>
      <CardTitle>Create an account</CardTitle>
      <CardDescription>New participants are active immediately; staff accounts still need approval.</CardDescription>
      <form className="mt-4 space-y-3" onSubmit={handleSubmit((values) => create.mutate(values))} noValidate>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Name" htmlFor="new-name" error={errors.name?.message} required>
            <Input id="new-name" invalid={!!errors.name} {...register('name')} />
          </Field>
          <Field label="Email" htmlFor="new-email" error={errors.email?.message} required>
            <Input id="new-email" type="email" invalid={!!errors.email} {...register('email')} />
          </Field>
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Password" htmlFor="new-password" error={errors.password?.message} required>
            <Input id="new-password" type="password" invalid={!!errors.password} {...register('password')} />
          </Field>
          <Field label="Role" htmlFor="new-role" error={errors.role?.message} required>
            <Select id="new-role" {...register('role')}>
              <option value="PARTICIPANT">Participant</option>
              <option value="FACILITATOR">Facilitator</option>
              <option value="MENTOR">Mentor</option>
              <option value="PARTNER">Partner</option>
              <option value="ADMIN">Administrator</option>
            </Select>
          </Field>
        </div>
        {role === 'PARTICIPANT' && (
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Age" htmlFor="new-age" error={errors.age?.message}>
              <Input id="new-age" type="number" min={18} max={35} {...register('age')} />
            </Field>
            <Field label="Country" htmlFor="new-country" error={errors.country?.message}>
              <Input id="new-country" {...register('country')} />
            </Field>
          </div>
        )}
        {role === 'PARTNER' && (
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Organisation name" htmlFor="new-org" error={errors.organisationName?.message}>
              <Input id="new-org" {...register('organisationName')} />
            </Field>
            <Field label="Organisation type" htmlFor="new-org-type" error={errors.organisationType?.message}>
              <Select id="new-org-type" {...register('organisationType')}>
                <option value="">Choose…</option>
                <option value="NGO">NGO</option>
                <option value="UNIVERSITY">University</option>
                <option value="EMPLOYER">Employer</option>
                <option value="DEVELOPMENT_AGENCY">Development agency</option>
                <option value="TECH_COMPANY">Tech company</option>
              </Select>
            </Field>
          </div>
        )}
        <Button type="submit" size="sm" loading={isSubmitting || create.isPending}>
          Create account
        </Button>
      </form>
    </Card>
  );
}

function UsersPanel() {
  const queryClient = useQueryClient();
  const [role, setRole] = useState<'' | Role>('');
  const [status, setStatus] = useState<'' | AccountStatus>('');
  const [search, setSearch] = useState('');
  const [q, setQ] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);

  const query = withQuery('/api/v1/admin/users', {
    role: role || undefined,
    status: status || undefined,
    q: q || undefined,
    page: 0,
    size: 30,
  });

  const users = useQuery({
    queryKey: ['admin-users', query],
    queryFn: () => api<PageResponse<User>>(query),
    placeholderData: (previous) => previous,
  });

  const updateStatus = useMutation({
    mutationFn: ({ id, next }: { id: number; next: AccountStatus }) =>
      api<User>(`/api/v1/admin/users/${id}/status`, { method: 'PUT', body: { status: next } }),
    onSuccess: (updated) => {
      setNotice(`${updated.name} is now ${titleCase(updated.status)}.`);
      void queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      void queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not update this account.'),
  });

  const approve = useMutation({
    mutationFn: ({ id, approved }: { id: number; approved: boolean }) =>
      api<User>(`/api/v1/admin/users/${id}/approve`, { method: 'PUT', body: { approved } }),
    onSuccess: (updated) => {
      setNotice(`${updated.name} was ${updated.status === 'ACTIVE' ? 'approved' : 'rejected'}.`);
      void queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      void queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not update this account.'),
  });

  const changeRole = useMutation({
    mutationFn: ({ id, next }: { id: number; next: Role }) =>
      api<User>(`/api/v1/admin/users/${id}/role`, { method: 'PUT', body: { role: next } }),
    onSuccess: (updated) => {
      setNotice(`${updated.name} is now a ${titleCase(updated.role)}.`);
      void queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      void queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not switch this role.'),
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <form
          className="grid flex-1 gap-3 rounded-2xl border border-slate-200 bg-white p-4 sm:grid-cols-4 dark:border-slate-800 dark:bg-slate-900"
          onSubmit={(event) => {
            event.preventDefault();
            setQ(search.trim());
          }}
          role="search"
        >
          <Field label="Search" htmlFor="user-search">
            <Input
              id="user-search"
              type="search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Name or email"
            />
          </Field>
          <Field label="Role" htmlFor="user-role">
            <Select id="user-role" value={role} onChange={(event) => setRole(event.target.value as Role | '')}>
              <option value="">All roles</option>
              <option value="PARTICIPANT">Participant</option>
              <option value="FACILITATOR">Facilitator</option>
              <option value="MENTOR">Mentor</option>
              <option value="PARTNER">Partner</option>
              <option value="ADMIN">Admin</option>
            </Select>
          </Field>
          <Field label="Status" htmlFor="user-status">
            <Select id="user-status" value={status} onChange={(event) => setStatus(event.target.value as AccountStatus | '')}>
              <option value="">All statuses</option>
              <option value="PENDING">Pending</option>
              <option value="ACTIVE">Active</option>
              <option value="SUSPENDED">Suspended</option>
            </Select>
          </Field>
          <div className="flex items-end">
            <Button type="submit" size="sm" className="w-full">
              Apply filters
            </Button>
          </div>
        </form>
        <Button size="sm" variant="secondary" onClick={() => setShowCreate((current) => !current)}>
          {showCreate ? 'Close form' : 'New account'}
        </Button>
      </div>

      {notice && <Alert tone={notice.toLowerCase().includes('could not') ? 'error' : 'success'}>{notice}</Alert>}
      {showCreate && <CreateUserForm onDone={setNotice} />}

      {users.isLoading ? (
        <SpinnerRow label="Loading users…" />
      ) : users.isError ? (
        <ErrorState
          message={users.error instanceof ApiError ? users.error.message : 'Could not load users.'}
          onRetry={() => users.refetch()}
        />
      ) : (users.data?.items ?? []).length === 0 ? (
        <EmptyState title="No users found" description="Try a different role, status or search term." />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
          <table className="w-full min-w-[900px] text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:bg-slate-800/60 dark:text-slate-400">
              <tr>
                <th scope="col" className="px-4 py-3">Name</th>
                <th scope="col" className="px-4 py-3">Email</th>
                <th scope="col" className="px-4 py-3">Role</th>
                <th scope="col" className="px-4 py-3">Joined</th>
                <th scope="col" className="px-4 py-3">Status</th>
                <th scope="col" className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
              {(users.data?.items ?? []).map((user) => (
                <tr key={user.userId}>
                  <td className="px-4 py-3 font-medium text-slate-900 dark:text-slate-100">{user.name}</td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{user.email}</td>
                  <td className="px-4 py-3"><Badge tone={user.role}>{titleCase(user.role)}</Badge></td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{formatDate(user.createdAt)}</td>
                  <td className="px-4 py-3">
                    <Badge tone={user.status === 'ACTIVE' ? 'COMPLETED' : user.status === 'PENDING' ? 'SUBMITTED' : 'REJECTED'}>
                      {titleCase(user.status)}
                    </Badge>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap justify-end gap-2">
                      {user.status === 'PENDING' && (
                        <>
                          <Button size="sm" loading={approve.isPending} onClick={() => approve.mutate({ id: user.userId, approved: true })}>
                            Approve
                          </Button>
                          <Button size="sm" variant="danger" loading={approve.isPending} onClick={() => approve.mutate({ id: user.userId, approved: false })}>
                            Reject
                          </Button>
                        </>
                      )}
                      {user.status !== 'PENDING' && (
                        <Button
                          size="sm"
                          variant="secondary"
                          loading={updateStatus.isPending}
                          onClick={() =>
                            updateStatus.mutate({
                              id: user.userId,
                              next: user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE',
                            })
                          }
                        >
                          {user.status === 'ACTIVE' ? 'Suspend' : 'Reactivate'}
                        </Button>
                      )}
                      {(user.role === 'FACILITATOR' || user.role === 'MENTOR') && (
                        <Button
                          size="sm"
                          variant="ghost"
                          loading={changeRole.isPending}
                          onClick={() =>
                            changeRole.mutate({
                              id: user.userId,
                              next: user.role === 'FACILITATOR' ? 'MENTOR' : 'FACILITATOR',
                            })
                          }
                        >
                          Switch to {user.role === 'FACILITATOR' ? 'mentor' : 'facilitator'}
                        </Button>
                      )}
                    </div>
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

function ActivitiesPanel() {
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);

  const activities = useQuery({
    queryKey: ['admin-activities'],
    queryFn: () => api<PageResponse<Activity>>('/api/v1/facilitator/activities?size=100'),
  });

  const facilitators = useQuery({
    queryKey: ['admin-facilitators'],
    queryFn: () => api<PageResponse<User>>('/api/v1/admin/users?role=FACILITATOR&size=100'),
  });

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ActivityValues>({
    resolver: zodResolver(activitySchema),
    defaultValues: { level: 'ONE', title: '', startDate: '' },
  });

  const level = watch('level');

  const create = useMutation({
    mutationFn: (values: ActivityValues) =>
      api<Activity>('/api/v1/activities', {
        method: 'POST',
        body: {
          level: values.level,
          title: values.title,
          description: values.description || null,
          startDate: values.startDate,
          endDate: values.endDate || null,
          category: values.category || null,
          capacity: values.capacity ?? null,
          facilitatorId: values.facilitatorId ?? null,
          partnerId: values.partnerId ?? null,
          passMark: values.passMark ?? null,
          minAttendancePercent: values.minAttendancePercent ?? null,
          minTeamSize: values.minTeamSize ?? null,
          maxTeamSize: values.maxTeamSize ?? null,
        },
      }),
    onSuccess: (activity) => {
      setNotice(`Created "${activity.title}".`);
      reset();
      void queryClient.invalidateQueries({ queryKey: ['admin-activities'] });
      void queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not create the activity.'),
  });

  const changeStatus = useMutation({
    mutationFn: ({ id, next }: { id: number; next: ActivityStatus }) =>
      api<Activity>(`/api/v1/activities/${id}/status`, { method: 'PUT', body: { status: next } }),
    onSuccess: (activity) => {
      setNotice(`"${activity.title}" is now ${titleCase(activity.status)}.`);
      void queryClient.invalidateQueries({ queryKey: ['admin-activities'] });
      void queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not change the activity status.'),
  });

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <Card>
        <CardTitle>Create an activity</CardTitle>
        <CardDescription>Activities start as drafts. Publish them when the dates and facilitator are ready.</CardDescription>
        <form className="mt-4 space-y-3" onSubmit={handleSubmit((values) => create.mutate(values))} noValidate>
          {notice && <Alert tone={notice.toLowerCase().includes('could not') ? 'error' : 'success'}>{notice}</Alert>}
          <Field label="Title" htmlFor="activity-title" error={errors.title?.message} required>
            <Input id="activity-title" invalid={!!errors.title} {...register('title')} />
          </Field>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Level" htmlFor="activity-level" required>
              <Select id="activity-level" {...register('level')}>
                <option value="ONE">Level One - group sessions</option>
                <option value="TWO">Level Two - team projects</option>
              </Select>
            </Field>
            <Field label="Facilitator" htmlFor="activity-facilitator" error={errors.facilitatorId?.message} required>
              <Select id="activity-facilitator" {...register('facilitatorId')}>
                <option value="">Choose…</option>
                {(facilitators.data?.items ?? []).map((user) => (
                  <option key={user.userId} value={user.userId}>{user.name}</option>
                ))}
              </Select>
            </Field>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Start date" htmlFor="activity-start" error={errors.startDate?.message} required>
              <Input id="activity-start" type="date" invalid={!!errors.startDate} {...register('startDate')} />
            </Field>
            <Field label="End date" htmlFor="activity-end" error={errors.endDate?.message}>
              <Input id="activity-end" type="date" {...register('endDate')} />
            </Field>
          </div>
          <Field label="Description" htmlFor="activity-description" error={errors.description?.message}>
            <Textarea id="activity-description" rows={3} {...register('description')} />
          </Field>
          <div className="grid gap-3 sm:grid-cols-3">
            <Field label="Capacity" htmlFor="activity-capacity" error={errors.capacity?.message}>
              <Input id="activity-capacity" type="number" min={1} {...register('capacity')} />
            </Field>
            <Field label="Pass mark" htmlFor="activity-pass" error={errors.passMark?.message}>
              <Input id="activity-pass" type="number" min={0} max={100} {...register('passMark')} />
            </Field>
            <Field label="Attendance %" htmlFor="activity-attendance" error={errors.minAttendancePercent?.message}>
              <Input id="activity-attendance" type="number" min={0} max={100} {...register('minAttendancePercent')} />
            </Field>
          </div>
          {level === 'TWO' && (
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="Minimum team size" htmlFor="activity-min-team" error={errors.minTeamSize?.message}>
                <Input id="activity-min-team" type="number" min={1} {...register('minTeamSize')} />
              </Field>
              <Field label="Maximum team size" htmlFor="activity-max-team" error={errors.maxTeamSize?.message}>
                <Input id="activity-max-team" type="number" min={1} {...register('maxTeamSize')} />
              </Field>
            </div>
          )}
          <Button type="submit" loading={isSubmitting || create.isPending}>
            Create activity
          </Button>
        </form>
      </Card>

      <Card>
        <CardTitle>All activities</CardTitle>
        <CardDescription>Change status to publish, close or complete an activity.</CardDescription>
        {activities.isLoading ? (
          <SpinnerRow label="Loading activities…" />
        ) : activities.isError ? (
          <ErrorState message="Could not load activities." onRetry={() => activities.refetch()} />
        ) : (activities.data?.items ?? []).length === 0 ? (
          <EmptyState title="No activities yet" description="Create the first activity using the form." />
        ) : (
          <ul className="mt-3 divide-y divide-slate-100 dark:divide-slate-700">
            {(activities.data?.items ?? []).map((activity) => (
              <li key={activity.activityId} className="py-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <Link
                      href={`/exchanges/${activity.activityId}`}
                      className="text-sm font-semibold text-slate-900 hover:text-brand-700 hover:underline dark:text-slate-100 dark:hover:text-brand-300"
                    >
                      {activity.title}
                    </Link>
                    <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                      Level {activity.level === 'ONE' ? 'One' : 'Two'} · {activity.facilitatorName} · starts {formatDate(activity.startDate)}
                    </p>
                  </div>
                  <Badge tone={activity.status}>{titleCase(activity.status)}</Badge>
                </div>
                <div className="mt-2 flex items-center gap-2">
                  <Select
                    aria-label={`Status for ${activity.title}`}
                    className="max-w-[10rem]"
                    value={activity.status}
                    onChange={(event) => changeStatus.mutate({ id: activity.activityId, next: event.target.value as ActivityStatus })}
                  >
                    <option value="DRAFT">Draft</option>
                    <option value="PUBLISHED">Published</option>
                    <option value="CLOSED">Closed</option>
                    <option value="COMPLETED">Completed</option>
                  </Select>
                  <LinkButton href={`/exchanges/${activity.activityId}`} size="sm" variant="ghost">
                    Open
                  </LinkButton>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}

function PartnersPanel() {
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);

  const partners = useQuery({
    queryKey: ['admin-partners'],
    queryFn: () => api<PageResponse<PartnerSummary>>('/api/v1/admin/partners?size=100'),
  });

  const verify = useMutation({
    mutationFn: ({ id, verified }: { id: number; verified: boolean }) =>
      api<PartnerSummary>(`/api/v1/admin/partners/${id}/verification`, {
        method: 'PUT',
        body: { verified },
      }),
    onSuccess: (partner) => {
      setNotice(`${partner.organisationName} is now ${partner.verified ? 'verified' : 'unverified'}.`);
      void queryClient.invalidateQueries({ queryKey: ['admin-partners'] });
      void queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not update verification.'),
  });

  return (
    <div className="space-y-4">
      {notice && <Alert tone={notice.toLowerCase().includes('could not') ? 'error' : 'success'}>{notice}</Alert>}
      {partners.isLoading ? (
        <SpinnerRow label="Loading partners…" />
      ) : partners.isError ? (
        <ErrorState message="Could not load partners." onRetry={() => partners.refetch()} />
      ) : (partners.data?.items ?? []).length === 0 ? (
        <EmptyState title="No partners yet" description="Partner organisations appear here after they register." />
      ) : (
        <div className="grid gap-3 md:grid-cols-2">
          {(partners.data?.items ?? []).map((partner) => (
            <Card key={partner.partnerId} className="!p-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-semibold text-slate-900 dark:text-slate-100">{partner.organisationName}</p>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    {partner.name} · {partner.email}
                    {partner.country ? ` · ${partner.country}` : ''}
                  </p>
                  {partner.organisationType && (
                    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{titleCase(partner.organisationType)}</p>
                  )}
                </div>
                <Badge tone={partner.verified ? 'COMPLETED' : 'PENDING'}>
                  {partner.verified ? 'Verified' : 'Unverified'}
                </Badge>
              </div>
              {partner.description && <p className="mt-2 text-sm text-slate-700 dark:text-slate-300">{partner.description}</p>}
              <div className="mt-3 flex items-center gap-2">
                <Button
                  size="sm"
                  variant={partner.verified ? 'secondary' : 'primary'}
                  loading={verify.isPending}
                  onClick={() => verify.mutate({ id: partner.partnerId, verified: !partner.verified })}
                >
                  {partner.verified ? 'Remove verification' : 'Verify partner'}
                </Button>
                {partner.website && (
                  <a
                    className="text-sm font-medium text-brand-700 hover:underline dark:text-brand-300"
                    href={partner.website}
                    target="_blank"
                    rel="noreferrer"
                  >
                    Website
                  </a>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

function ProjectsPanel() {
  const projects = useQuery({
    queryKey: ['admin-projects'],
    queryFn: () => api<Project[]>('/api/v1/public/projects/showcase'),
  });

  return (
    <div className="space-y-4">
      <Card>
        <CardTitle>Impact project showcase</CardTitle>
        <CardDescription>Approved and completed projects that are visible to the public.</CardDescription>
        {projects.isLoading ? (
          <SpinnerRow label="Loading projects…" />
        ) : projects.isError ? (
          <ErrorState message="Could not load the showcase." onRetry={() => projects.refetch()} />
        ) : (projects.data ?? []).length === 0 ? (
          <EmptyState title="No showcase projects" description="Approved projects appear here once teams open them for visibility." />
        ) : (
          <ul className="mt-3 divide-y divide-slate-100 dark:divide-slate-700">
            {(projects.data ?? []).map((project) => (
              <li key={project.projectId} className="flex items-start justify-between gap-3 py-3">
                <div>
                  <Link
                    href={`/projects/${project.projectId}`}
                    className="text-sm font-semibold text-slate-900 hover:text-brand-700 hover:underline dark:text-slate-100 dark:hover:text-brand-300"
                  >
                    {project.title}
                  </Link>
                  <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                    {project.teamName} · {titleCase(project.challengeArea)}
                  </p>
                </div>
                <Badge tone={project.status}>{titleCase(project.status)}</Badge>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}

function AuditPanel() {
  const logs = useQuery({
    queryKey: ['admin-audit'],
    queryFn: () => api<PageResponse<AuditLog>>('/api/v1/admin/audit-logs?size=50'),
  });

  return (
    <Card>
      <CardTitle>Audit trail</CardTitle>
      <CardDescription>Administrative actions are recorded for accountability.</CardDescription>
      {logs.isLoading ? (
        <SpinnerRow label="Loading audit logs…" />
      ) : logs.isError ? (
        <ErrorState message="Could not load audit logs." onRetry={() => logs.refetch()} />
      ) : (logs.data?.items ?? []).length === 0 ? (
        <EmptyState title="No audit entries" description="Actions will appear here as administrators work." />
      ) : (
        <div className="mt-3 overflow-x-auto">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:text-slate-400">
              <tr>
                <th scope="col" className="px-2 py-2">When</th>
                <th scope="col" className="px-2 py-2">Actor</th>
                <th scope="col" className="px-2 py-2">Action</th>
                <th scope="col" className="px-2 py-2">Entity</th>
                <th scope="col" className="px-2 py-2">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
              {(logs.data?.items ?? []).map((log) => (
                <tr key={log.logId}>
                  <td className="whitespace-nowrap px-2 py-2 text-slate-500 dark:text-slate-400">{formatDateTime(log.createdAt)}</td>
                  <td className="px-2 py-2 text-slate-700 dark:text-slate-300">{log.userName ?? 'System'}</td>
                  <td className="px-2 py-2 font-mono text-xs text-slate-700 dark:text-slate-300">{log.action}</td>
                  <td className="px-2 py-2 text-slate-700 dark:text-slate-300">
                    {log.entity}{log.entityId ? ` #${log.entityId}` : ''}
                  </td>
                  <td className="px-2 py-2 text-slate-600 dark:text-slate-400">{log.details ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function SettingsPanel() {
  const queryClient = useQueryClient();
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);

  const settings = useQuery({
    queryKey: ['admin-settings'],
    queryFn: () => api<Setting[]>('/api/v1/admin/settings'),
  });

  const save = useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) =>
      api<Setting>(`/api/v1/admin/settings/${encodeURIComponent(key)}`, { method: 'PUT', body: { value } }),
    onSuccess: (setting) => {
      setNotice(`Saved ${setting.key}.`);
      void queryClient.invalidateQueries({ queryKey: ['admin-settings'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not save this setting.'),
  });

  return (
    <Card>
      <CardTitle>Platform settings</CardTitle>
      <CardDescription>These values control registration, assessment and capacity defaults.</CardDescription>
      {notice && <div className="mt-3"><Alert tone={notice.toLowerCase().includes('could not') ? 'error' : 'success'}>{notice}</Alert></div>}
      {settings.isLoading ? (
        <SpinnerRow label="Loading settings…" />
      ) : settings.isError ? (
        <ErrorState message="Could not load settings." onRetry={() => settings.refetch()} />
      ) : (
        <ul className="mt-3 space-y-3">
          {(settings.data ?? []).map((setting) => (
            <li key={setting.key} className="rounded-lg border border-slate-200 p-3 dark:border-slate-700">
              <div className="flex flex-wrap items-end gap-2">
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-mono text-slate-500 dark:text-slate-400">{setting.key}</p>
                  <Input
                    aria-label={setting.key}
                    className="mt-1"
                    value={drafts[setting.key] ?? setting.value}
                    onChange={(event) => setDrafts((current) => ({ ...current, [setting.key]: event.target.value }))}
                  />
                </div>
                <Button
                  size="sm"
                  variant="secondary"
                  loading={save.isPending}
                  onClick={() => save.mutate({ key: setting.key, value: drafts[setting.key] ?? setting.value })}
                >
                  Save
                </Button>
              </div>
              {setting.updatedAt && (
                <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Updated {formatDateTime(setting.updatedAt)}</p>
              )}
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

function AdminContent() {
  const [tab, setTab] = useState<Tab>('OVERVIEW');

  const tabs: Tab[] = ['OVERVIEW', 'USERS', 'ACTIVITIES', 'PARTNERS', 'PROJECTS', 'AUDIT', 'SETTINGS'];

  return (
    <>
      <PageHeader
        title="Administration"
        description="Manage accounts, activities, partners, the project showcase and platform settings."
        actions={
          <div className="flex flex-wrap gap-2" role="tablist" aria-label="Admin sections">
            {tabs.map((value) => (
              <Button
                key={value}
                role="tab"
                aria-selected={tab === value}
                size="sm"
                variant={tab === value ? 'primary' : 'secondary'}
                onClick={() => setTab(value)}
              >
                {titleCase(value)}
              </Button>
            ))}
          </div>
        }
      />

      {tab === 'OVERVIEW' && <OverviewPanel />}
      {tab === 'USERS' && <UsersPanel />}
      {tab === 'ACTIVITIES' && <ActivitiesPanel />}
      {tab === 'PARTNERS' && <PartnersPanel />}
      {tab === 'PROJECTS' && <ProjectsPanel />}
      {tab === 'AUDIT' && <AuditPanel />}
      {tab === 'SETTINGS' && <SettingsPanel />}
    </>
  );
}

export default function AdminPage() {
  return (
    <RequireAuth roles={['ADMIN']}>
      <AdminContent />
    </RequireAuth>
  );
}

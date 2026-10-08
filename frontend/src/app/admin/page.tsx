'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api, withQuery } from '@/lib/api';
import { exchangeSchema, type ExchangeValues } from '@/lib/schemas';
import { formatDate, titleCase } from '@/lib/format';
import type { Exchange, PageResponse, Project, Role, User } from '@/lib/types';
import { RequireAuth } from '@/components/RequireAuth';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';

type Tab = 'USERS' | 'PROJECTS' | 'EXCHANGES';
type AccountStatusValue = 'ACTIVE' | 'SUSPENDED';

function UsersPanel() {
  const queryClient = useQueryClient();
  const [role, setRole] = useState<'' | Role>('');
  const [search, setSearch] = useState('');
  const [q, setQ] = useState('');
  const [notice, setNotice] = useState<string | null>(null);

  const query = withQuery('/api/v1/users', { role: role || undefined, q: q || undefined, page: 0, size: 25 });
  const users = useQuery({
    queryKey: ['admin-users', query],
    queryFn: () => api<PageResponse<User>>(query),
    placeholderData: (previous) => previous,
  });

  const updateStatus = useMutation({
    mutationFn: ({ id, accountStatus }: { id: number; accountStatus: AccountStatusValue }) =>
      api<User>(`/api/v1/users/${id}`, { method: 'PUT', body: { accountStatus } }),
    onSuccess: (updated) => {
      setNotice(`${updated.fullName} is now ${titleCase(updated.accountStatus)}.`);
      void queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not update this account.'),
  });

  return (
    <>
      <form
        className="mb-4 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 sm:grid-cols-3"
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
            <option value="ADMIN">Admin</option>
          </Select>
        </Field>
        <div className="flex items-end">
          <Button type="submit" size="sm" className="w-full">
            Apply
          </Button>
        </div>
      </form>

      {notice && <div className="mb-4"><Alert>{notice}</Alert></div>}

      {users.isLoading ? (
        <SpinnerRow label="Loading users…" />
      ) : users.isError ? (
        <ErrorState
          message={users.error instanceof ApiError ? users.error.message : 'Could not load users.'}
          onRetry={() => users.refetch()}
        />
      ) : (users.data?.content ?? []).length === 0 ? (
        <EmptyState title="No users found" description="Try a different role or search term." />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th scope="col" className="px-4 py-3">Name</th>
                <th scope="col" className="px-4 py-3">Email</th>
                <th scope="col" className="px-4 py-3">Role</th>
                <th scope="col" className="px-4 py-3">Joined</th>
                <th scope="col" className="px-4 py-3">Status</th>
                <th scope="col" className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {(users.data?.content ?? []).map((user) => (
                <tr key={user.id}>
                  <td className="px-4 py-3 font-medium text-slate-900">{user.fullName}</td>
                  <td className="px-4 py-3 text-slate-600">{user.email}</td>
                  <td className="px-4 py-3"><Badge tone={user.role}>{titleCase(user.role)}</Badge></td>
                  <td className="px-4 py-3 text-slate-600">{formatDate(user.createdAt)}</td>
                  <td className="px-4 py-3">
                    <Badge tone={user.accountStatus === 'ACTIVE' ? 'COMPLETED' : 'REJECTED'}>
                      {titleCase(user.accountStatus)}
                    </Badge>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Button
                      size="sm"
                      variant="secondary"
                      loading={updateStatus.isPending}
                      onClick={() =>
                        updateStatus.mutate({
                          id: user.id,
                          accountStatus: user.accountStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE',
                        })
                      }
                    >
                      {user.accountStatus === 'ACTIVE' ? 'Suspend' : 'Reactivate'}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function ProjectsPanel() {
  const [status, setStatus] = useState('SUBMITTED');
  const query = withQuery('/api/v1/projects', { status });
  const projects = useQuery({
    queryKey: ['admin-projects', query],
    queryFn: () => api<Project[]>(query),
    placeholderData: (previous) => previous,
  });

  return (
    <>
      <form
        className="mb-4 rounded-2xl border border-slate-200 bg-white p-4 sm:max-w-xs"
        onSubmit={(event) => event.preventDefault()}
      >
        <Field label="Pipeline stage" htmlFor="pipeline">
          <Select id="pipeline" value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="SUBMITTED">Submitted</option>
            <option value="UNDER_REVIEW">Under review</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="COMPLETED">Completed</option>
            <option value="DRAFT">Draft</option>
          </Select>
        </Field>
      </form>

      {projects.isLoading ? (
        <SpinnerRow label="Loading projects…" />
      ) : projects.isError ? (
        <ErrorState message="Could not load projects." onRetry={() => projects.refetch()} />
      ) : (projects.data ?? []).length === 0 ? (
        <EmptyState title="Nothing at this stage" description="Change the pipeline stage to inspect another queue." />
      ) : (
        <ul className="space-y-3">
          {(projects.data ?? []).map((project) => (
            <li key={project.id}>
              <Card className="!p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <Link href={`/projects/${project.id}`} className="font-semibold text-slate-900 hover:text-brand-700 hover:underline">
                      {project.title}
                    </Link>
                    <p className="mt-0.5 text-xs text-slate-500">
                      Team {project.teamName ?? '—'}
                      {project.focusArea ? ` · ${titleCase(project.focusArea)}` : ''}
                      {project.submissionDate ? ` · submitted ${formatDate(project.submissionDate)}` : ''}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {project.aiAssisted && <Badge tone="DRAFT">AI-assisted</Badge>}
                    <Badge tone={project.status}>{titleCase(project.status)}</Badge>
                  </div>
                </div>
                <div className="mt-3">
                  <LinkButton href={`/projects/${project.id}`} size="sm" variant="secondary">
                    Review
                  </LinkButton>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

function ExchangesPanel() {
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);

  const exchanges = useQuery({
    queryKey: ['admin-exchanges'],
    queryFn: () => api<PageResponse<Exchange>>('/api/v1/exchanges?size=25'),
  });

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ExchangeValues>({
    resolver: zodResolver(exchangeSchema),
    defaultValues: { level: 'LEVEL_ONE', status: 'OPEN', startDate: '' },
  });

  const level = watch('level');

  const create = useMutation({
    mutationFn: (values: ExchangeValues) =>
      api<Exchange>('/api/v1/exchanges', {
        method: 'POST',
        body: {
          ...values,
          startDate: values.startDate,
          endDate: values.endDate || null,
          sessionLink: values.sessionLink || null,
          description: values.description || null,
        },
      }),
    onSuccess: (exchange) => {
      setNotice(`Created "${exchange.title}".`);
      reset();
      void queryClient.invalidateQueries({ queryKey: ['admin-exchanges'] });
      void queryClient.invalidateQueries({ queryKey: ['exchanges'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not create the exchange.'),
  });

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <Card>
        <CardTitle>Create an exchange</CardTitle>
        <CardDescription>
          Level One needs a topic; Level Two needs a focus area. Both need a start date.
        </CardDescription>

        <form className="mt-4 space-y-4" onSubmit={handleSubmit((values) => create.mutate(values))} noValidate>
          {notice && <Alert tone={notice.toLowerCase().includes('could') ? 'error' : 'success'}>{notice}</Alert>}

          <Field label="Title" htmlFor="exchange-title" error={errors.title?.message} required>
            <Input id="exchange-title" invalid={!!errors.title} {...register('title')} />
          </Field>

          <Field label="Level" htmlFor="exchange-level" required>
            <Select id="exchange-level" {...register('level')}>
              <option value="LEVEL_ONE">Level One - large group session</option>
              <option value="LEVEL_TWO">Level Two - small team programme</option>
            </Select>
          </Field>

          {level === 'LEVEL_ONE' ? (
            <Field label="Topic" htmlFor="exchange-topic" error={errors.topic?.message} required>
              <Select id="exchange-topic" defaultValue="" {...register('topic')}>
                <option value="" disabled>Choose a topic</option>
                <option value="DIGITAL_LITERACY">Digital literacy</option>
                <option value="AI_LITERACY">AI literacy</option>
                <option value="CRITICAL_THINKING">Critical thinking</option>
                <option value="MEDIA_LITERACY">Media literacy</option>
                <option value="INTERCULTURAL_COMM">Intercultural communication</option>
              </Select>
            </Field>
          ) : (
            <Field label="Focus area" htmlFor="exchange-focus" error={errors.focusArea?.message} required>
              <Select id="exchange-focus" defaultValue="" {...register('focusArea')}>
                <option value="" disabled>Choose a focus area</option>
                <option value="EMPLOYMENT">Employment</option>
                <option value="EDUCATION">Education</option>
                <option value="AGRICULTURE">Agriculture</option>
                <option value="HEALTH">Health</option>
                <option value="CLIMATE">Climate</option>
                <option value="DIGITAL_INCLUSION">Digital inclusion</option>
              </Select>
            </Field>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Start date" htmlFor="exchange-start" error={errors.startDate?.message} required>
              <Input id="exchange-start" type="date" invalid={!!errors.startDate} {...register('startDate')} />
            </Field>
            <Field label="End date" htmlFor="exchange-end" error={errors.endDate?.message}>
              <Input id="exchange-end" type="date" {...register('endDate')} />
            </Field>
          </div>

          <Field label="Description" htmlFor="exchange-description" error={errors.description?.message}>
            <Textarea id="exchange-description" rows={4} {...register('description')} />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            {level === 'LEVEL_ONE' ? (
              <>
                <Field label="Max participants" htmlFor="exchange-cap">
                  <Input id="exchange-cap" type="number" min={1} {...register('maxParticipants')} />
                </Field>
                <Field label="Session link" htmlFor="exchange-link">
                  <Input id="exchange-link" type="url" placeholder="https://…" {...register('sessionLink')} />
                </Field>
              </>
            ) : (
              <Field label="Max teams" htmlFor="exchange-teams">
                <Input id="exchange-teams" type="number" min={1} {...register('maxTeams')} />
              </Field>
            )}
          </div>

          <Button type="submit" loading={isSubmitting || create.isPending}>
            Create exchange
          </Button>
        </form>
      </Card>

      <Card>
        <CardTitle>Recent exchanges</CardTitle>
        {exchanges.isLoading ? (
          <SpinnerRow label="Loading exchanges…" />
        ) : exchanges.isError ? (
          <ErrorState message="Could not load exchanges." onRetry={() => exchanges.refetch()} />
        ) : (
          <ul className="mt-3 divide-y divide-slate-100">
            {(exchanges.data?.content ?? []).map((exchange) => (
              <li key={exchange.id} className="flex items-center justify-between gap-3 py-2.5">
                <div>
                  <Link href={`/exchanges/${exchange.id}`} className="text-sm font-medium text-slate-900 hover:text-brand-700 hover:underline">
                    {exchange.title}
                  </Link>
                  <p className="text-xs text-slate-500">
                    {exchange.level === 'LEVEL_ONE' ? 'Level One' : 'Level Two'} · starts {formatDate(exchange.startDate)}
                  </p>
                </div>
                <Badge tone={exchange.status}>{titleCase(exchange.status)}</Badge>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}

function AdminContent() {
  const [tab, setTab] = useState<Tab>('USERS');

  return (
    <>
      <PageHeader
        title="Administration"
        description="Manage accounts, review the impact-project pipeline and create virtual exchanges."
        actions={
          <div className="flex flex-wrap gap-2" role="tablist" aria-label="Admin sections">
            {(['USERS', 'PROJECTS', 'EXCHANGES'] as Tab[]).map((value) => (
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

      {tab === 'USERS' && <UsersPanel />}
      {tab === 'PROJECTS' && <ProjectsPanel />}
      {tab === 'EXCHANGES' && <ExchangesPanel />}
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

'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { opportunitySchema, type OpportunityValues } from '@/lib/schemas';
import { formatDate, formatDateTime, titleCase } from '@/lib/format';
import type { Interest, Opportunity, Support } from '@/lib/types';
import { RequireAuth } from '@/components/RequireAuth';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';

function InterestsPanel({ opportunity }: { opportunity: Opportunity }) {
  const interests = useQuery({
    queryKey: ['opportunity-interests', opportunity.opportunityId],
    queryFn: () => api<Interest[]>(`/api/v1/partner/opportunities/${opportunity.opportunityId}/interests`),
  });

  return (
    <div className="mt-3 rounded-lg border border-slate-200 p-3 dark:border-slate-700">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
        Interested participants
      </p>
      {interests.isLoading ? (
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Loading…</p>
      ) : (interests.data ?? []).length === 0 ? (
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">No expressions of interest yet.</p>
      ) : (
        <ul className="mt-2 divide-y divide-slate-100 dark:divide-slate-700">
          {(interests.data ?? []).map((interest) => (
            <li key={interest.interestId} className="py-2">
              <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{interest.youthName}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {interest.youthEmail} · {formatDateTime(interest.createdAt)}
              </p>
              {interest.message && <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">{interest.message}</p>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function PartnerContent() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);
  const [openInterests, setOpenInterests] = useState<number | null>(null);

  const opportunities = useQuery({
    queryKey: ['partner-opportunities'],
    queryFn: () => api<Opportunity[]>('/api/v1/partner/opportunities'),
  });

  const supports = useQuery({
    queryKey: ['partner-supports'],
    queryFn: () => api<Support[]>('/api/v1/partner/supports'),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<OpportunityValues>({
    resolver: zodResolver(opportunitySchema),
    defaultValues: { title: '', opportunityType: 'JOB', description: '', location: '', deadline: '' },
  });

  const create = useMutation({
    mutationFn: (values: OpportunityValues) =>
      api<Opportunity>('/api/v1/partner/opportunities', {
        method: 'POST',
        body: {
          title: values.title,
          opportunityType: values.opportunityType,
          description: values.description || null,
          location: values.location || null,
          deadline: values.deadline || null,
        },
      }),
    onSuccess: (opportunity) => {
      setNotice(`Published "${opportunity.title}".`);
      reset();
      void queryClient.invalidateQueries({ queryKey: ['partner-opportunities'] });
      void queryClient.invalidateQueries({ queryKey: ['opportunities'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not publish the opportunity.'),
  });

  const close = useMutation({
    mutationFn: (id: number) =>
      api<Opportunity>(`/api/v1/partner/opportunities/${id}/close`, { method: 'PUT' }),
    onSuccess: (opportunity) => {
      setNotice(`Closed "${opportunity.title}".`);
      void queryClient.invalidateQueries({ queryKey: ['partner-opportunities'] });
      void queryClient.invalidateQueries({ queryKey: ['opportunities'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not close the opportunity.'),
  });

  return (
    <>
      <PageHeader
        title="Partner workspace"
        description="Publish opportunities, review expressions of interest and track the support you offer."
        actions={
          <div className="flex gap-2">
            <LinkButton href="/opportunities" size="sm" variant="secondary">
              Public opportunities
            </LinkButton>
            <LinkButton href="/dashboard" size="sm" variant="ghost">
              Dashboard
            </LinkButton>
          </div>
        }
      />

      {!user?.verified && (
        <div className="mb-4">
          <Alert tone="warning">
            Your organisation is not verified yet. An administrator must verify you before you can publish
            opportunities or offer project support.
          </Alert>
        </div>
      )}

      {notice && (
        <div className="mb-4">
          <Alert tone={notice.toLowerCase().includes('could not') ? 'error' : 'success'}>{notice}</Alert>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="space-y-4 lg:col-span-2">
          <Card>
            <CardTitle>My opportunities</CardTitle>
            <CardDescription>Participants can express interest while an opportunity is open.</CardDescription>
            {opportunities.isLoading ? (
              <SpinnerRow label="Loading opportunities…" />
            ) : opportunities.isError ? (
              <ErrorState
                message={opportunities.error instanceof ApiError ? opportunities.error.message : 'Could not load opportunities.'}
                onRetry={() => opportunities.refetch()}
              />
            ) : (opportunities.data ?? []).length === 0 ? (
              <EmptyState title="No opportunities yet" description="Use the form to publish your first opportunity." />
            ) : (
              <ul className="mt-3 space-y-3">
                {(opportunities.data ?? []).map((opportunity) => (
                  <li key={opportunity.opportunityId} className="rounded-lg border border-slate-200 p-3 dark:border-slate-700">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">{opportunity.title}</p>
                        <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                          {titleCase(opportunity.opportunityType)}
                          {opportunity.location ? ` · ${opportunity.location}` : ''}
                          {opportunity.deadline ? ` · deadline ${formatDate(opportunity.deadline)}` : ''}
                          {` · ${opportunity.interestCount} interested`}
                        </p>
                      </div>
                      <Badge tone={opportunity.status === 'OPEN' ? 'PUBLISHED' : 'CLOSED'}>
                        {titleCase(opportunity.status)}
                      </Badge>
                    </div>
                    <div className="mt-2 flex flex-wrap gap-2">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => setOpenInterests((current) => (current === opportunity.opportunityId ? null : opportunity.opportunityId))}
                      >
                        {openInterests === opportunity.opportunityId ? 'Hide interests' : 'View interests'}
                      </Button>
                      {opportunity.status === 'OPEN' && (
                        <Button size="sm" variant="danger" loading={close.isPending} onClick={() => close.mutate(opportunity.opportunityId)}>
                          Close
                        </Button>
                      )}
                    </div>
                    {openInterests === opportunity.opportunityId && <InterestsPanel opportunity={opportunity} />}
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card>
            <CardTitle>Support offered</CardTitle>
            <CardDescription>Funding, mentorship, resources and visibility offers made to project teams.</CardDescription>
            {supports.isLoading ? (
              <SpinnerRow label="Loading support offers…" />
            ) : (supports.data ?? []).length === 0 ? (
              <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">
                Offer support from a project page when a team has opened itself for support.
              </p>
            ) : (
              <ul className="mt-3 divide-y divide-slate-100 dark:divide-slate-700">
                {(supports.data ?? []).map((support) => (
                  <li key={support.supportId} className="flex items-start justify-between gap-3 py-3">
                    <div>
                      <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{support.projectTitle}</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        {titleCase(support.supportType)} · {formatDate(support.createdAt)}
                      </p>
                      {support.message && <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">{support.message}</p>}
                    </div>
                    <Badge tone={support.status}>{titleCase(support.status)}</Badge>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </section>

        <aside>
          <Card>
            <CardTitle>Publish an opportunity</CardTitle>
            <CardDescription>Only verified partners can publish.</CardDescription>
            <form className="mt-3 space-y-3" onSubmit={handleSubmit((values) => create.mutate(values))} noValidate>
              <Field label="Title" htmlFor="opportunity-title" error={errors.title?.message} required>
                <Input id="opportunity-title" invalid={!!errors.title} {...register('title')} />
              </Field>
              <Field label="Type" htmlFor="opportunity-type" error={errors.opportunityType?.message} required>
                <Select id="opportunity-type" {...register('opportunityType')}>
                  <option value="JOB">Job</option>
                  <option value="INTERNSHIP">Internship</option>
                  <option value="MENTORSHIP">Mentorship</option>
                  <option value="SCHOLARSHIP">Scholarship</option>
                  <option value="TRAINING">Training</option>
                </Select>
              </Field>
              <Field label="Location" htmlFor="opportunity-location" error={errors.location?.message}>
                <Input id="opportunity-location" {...register('location')} />
              </Field>
              <Field label="Deadline" htmlFor="opportunity-deadline" error={errors.deadline?.message}>
                <Input id="opportunity-deadline" type="date" {...register('deadline')} />
              </Field>
              <Field label="Description" htmlFor="opportunity-description" error={errors.description?.message}>
                <Textarea id="opportunity-description" rows={4} {...register('description')} />
              </Field>
              <Button type="submit" className="w-full" loading={isSubmitting || create.isPending} disabled={!user?.verified}>
                Publish opportunity
              </Button>
            </form>
          </Card>
        </aside>
      </div>
    </>
  );
}

export default function PartnerPage() {
  return (
    <RequireAuth roles={['PARTNER']}>
      <PartnerContent />
    </RequireAuth>
  );
}

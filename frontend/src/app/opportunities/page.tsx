'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatDate, titleCase } from '@/lib/format';
import type { Interest, Opportunity, PageResponse } from '@/lib/types';
import { RequireAuth } from '@/components/RequireAuth';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Textarea } from '@/components/ui/Form';

function OpportunityCard({ opportunity, interested }: { opportunity: Opportunity; interested: boolean }) {
  const queryClient = useQueryClient();
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState<string | null>(null);

  const interest = useMutation({
    mutationFn: () =>
      api<Interest>(`/api/v1/opportunities/${opportunity.opportunityId}/interest`, {
        method: 'POST',
        body: { message: message || null },
      }),
    onSuccess: () => {
      setNotice('Your interest has been sent to the partner.');
      setMessage('');
      void queryClient.invalidateQueries({ queryKey: ['opportunities'] });
      void queryClient.invalidateQueries({ queryKey: ['my-interests'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not send your interest.'),
  });

  return (
    <Card className="!p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">{opportunity.title}</p>
          <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
            {opportunity.organisationName} · {titleCase(opportunity.opportunityType)}
            {opportunity.location ? ` · ${opportunity.location}` : ''}
          </p>
        </div>
        <Badge tone={opportunity.status === 'OPEN' ? 'PUBLISHED' : 'CLOSED'}>{titleCase(opportunity.status)}</Badge>
      </div>
      {opportunity.description && (
        <p className="mt-2 whitespace-pre-line text-sm text-slate-700 dark:text-slate-300">{opportunity.description}</p>
      )}
      <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500 dark:text-slate-400">
        {opportunity.deadline && <span>Deadline {formatDate(opportunity.deadline)}</span>}
        <span>{opportunity.interestCount} interested</span>
      </div>

      {notice && <p className={`mt-2 text-xs ${notice.includes('Could') ? 'text-rose-600' : 'text-emerald-700 dark:text-emerald-300'}`}>{notice}</p>}

      {opportunity.status === 'OPEN' && !interested && (
        <div className="mt-3">
          <Field label="Message (optional)" htmlFor={`interest-${opportunity.opportunityId}`}>
            <Textarea
              id={`interest-${opportunity.opportunityId}`}
              rows={2}
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              placeholder="Tell the partner why this opportunity fits you."
            />
          </Field>
          <Button size="sm" className="mt-2" loading={interest.isPending} onClick={() => interest.mutate()}>
            Express interest
          </Button>
        </div>
      )}
      {interested && (
        <p className="mt-3 text-xs font-medium text-emerald-700 dark:text-emerald-300">
          You have already expressed interest in this opportunity.
        </p>
      )}
    </Card>
  );
}

function OpportunitiesContent() {
  const { user } = useAuth();

  const opportunities = useQuery({
    queryKey: ['opportunities'],
    queryFn: () => api<PageResponse<Opportunity>>('/api/v1/public/opportunities?size=50'),
  });

  const interests = useQuery({
    queryKey: ['my-interests'],
    queryFn: () => api<Interest[]>('/api/v1/opportunities/my-interests'),
    enabled: user?.role === 'PARTICIPANT',
  });

  const interestedIds = new Set((interests.data ?? []).map((item) => item.opportunityId));

  return (
    <>
      <PageHeader
        title="Opportunities"
        description="Jobs, internships, mentorships, scholarships and training from verified YEP partners."
        actions={
          user?.role === 'PARTNER' ? (
            <LinkButton href="/partner" size="sm" variant="secondary">
              Manage opportunities
            </LinkButton>
          ) : (
            <LinkButton href="/dashboard" size="sm" variant="secondary">
              Dashboard
            </LinkButton>
          )
        }
      />

      {opportunities.isLoading ? (
        <SpinnerRow label="Loading opportunities…" />
      ) : opportunities.isError ? (
        <ErrorState
          message={opportunities.error instanceof ApiError ? opportunities.error.message : 'Could not load opportunities.'}
          onRetry={() => opportunities.refetch()}
        />
      ) : (opportunities.data?.items ?? []).length === 0 ? (
        <EmptyState
          title="No open opportunities"
          description="Verified partners publish opportunities here. Check back soon."
        />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {(opportunities.data?.items ?? []).map((opportunity) => (
            <OpportunityCard
              key={opportunity.opportunityId}
              opportunity={opportunity}
              interested={interestedIds.has(opportunity.opportunityId)}
            />
          ))}
        </div>
      )}

      {user?.role === 'PARTICIPANT' && (interests.data ?? []).length > 0 && (
        <Card className="mt-6">
          <CardTitle>My expressions of interest</CardTitle>
          <CardDescription>Partners can see your profile details when you express interest.</CardDescription>
          <ul className="mt-3 divide-y divide-slate-100 dark:divide-slate-700">
            {(interests.data ?? []).map((interest) => (
              <li key={interest.interestId} className="py-2.5">
                <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{interest.opportunityTitle}</p>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Sent {formatDate(interest.createdAt)}
                  {interest.message ? ` · ${interest.message}` : ''}
                </p>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </>
  );
}

export default function OpportunitiesPage() {
  return (
    <RequireAuth>
      <OpportunitiesContent />
    </RequireAuth>
  );
}

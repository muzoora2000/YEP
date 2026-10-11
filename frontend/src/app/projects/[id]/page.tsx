'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatDate, titleCase } from '@/lib/format';
import type { AiSuggestion, Project } from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, Breadcrumbs, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Textarea } from '@/components/ui/Form';

export default function ProjectDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const queryClient = useQueryClient();
  const { isStaff } = useAuth();
  const [notice, setNotice] = useState<string | null>(null);
  const [feedback, setFeedback] = useState('');

  const projectQuery = useQuery({
    queryKey: ['project', id],
    queryFn: () => api<Project>(`/api/v1/projects/${id}`),
  });

  const review = useMutation({
    mutationFn: (decision: 'START' | 'APPROVE' | 'REQUEST_CHANGES') =>
      api<Project>(`/api/v1/projects/${id}/review?decision=${decision}${feedback ? `&feedback=${encodeURIComponent(feedback)}` : ''}`, {
        method: 'POST',
      }),
    onSuccess: () => {
      setNotice('Review recorded.');
      setFeedback('');
      void queryClient.invalidateQueries({ queryKey: ['project', id] });
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not record the review.'),
  });

  const complete = useMutation({
    mutationFn: () => api<Project>(`/api/v1/projects/${id}/complete`, { method: 'POST' }),
    onSuccess: () => {
      setNotice('Project marked complete - certificates can now be issued.');
      void queryClient.invalidateQueries({ queryKey: ['project', id] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not complete the project.'),
  });

  const suggest = useMutation({
    mutationFn: () =>
      api<AiSuggestion>('/api/v1/ai/suggest', {
        method: 'POST',
        body: {
          input: `Review feedback for the impact project "${projectQuery.data?.title}" (${projectQuery.data?.status}): ${feedback || 'needs improvement'}`,
          kind: 'FEEDBACK',
        },
      }),
    onSuccess: (suggestion) => setFeedback(suggestion.draft),
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'AI suggestion is unavailable.'),
  });

  if (projectQuery.isLoading) return <SpinnerRow label="Loading project…" />;
  if (projectQuery.isError || !projectQuery.data) {
    return (
      <ErrorState
        message={projectQuery.error instanceof ApiError ? projectQuery.error.message : 'This project could not be found.'}
        onRetry={() => projectQuery.refetch()}
      />
    );
  }

  const project = projectQuery.data;
  const canReview = isStaff && ['SUBMITTED', 'UNDER_REVIEW'].includes(project.status);

  return (
    <>
      <Breadcrumbs
        items={[
          { label: 'Home', href: '/' },
          { label: 'Projects', href: '/projects' },
          { label: project.title },
        ]}
      />

      <PageHeader
        title={project.title}
        description={
          project.teamName
            ? `Team ${project.teamName}${project.focusArea ? ` · ${titleCase(project.focusArea)}` : ''}`
            : 'Impact project'
        }
        actions={
          <>
            <Badge tone={project.status}>{titleCase(project.status)}</Badge>
            {project.teamId && (
              <LinkButton href={`/teams/${project.teamId}`} size="sm" variant="secondary">
                Go to team
              </LinkButton>
            )}
          </>
        }
      />

      {notice && (
        <div className="mb-4">
          <Alert tone={notice.toLowerCase().includes('could not') ? 'error' : 'success'}>{notice}</Alert>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="space-y-4 lg:col-span-2">
          <Card>
            <CardTitle>Problem statement</CardTitle>
            <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700 dark:text-slate-200">
              {project.problemStatement || 'Not documented yet.'}
            </p>
          </Card>

          <Card>
            <CardTitle>Proposed solution</CardTitle>
            <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700 dark:text-slate-200">
              {project.proposedSolution || 'Not documented yet.'}
            </p>
          </Card>

          {project.aiAssisted && (
            <Card className="border-amber-200 bg-amber-50/50 dark:border-amber-500/30 dark:bg-amber-500/10">
              <CardTitle className="text-amber-900 dark:text-amber-200">Responsible AI disclosure</CardTitle>
              <CardDescription className="text-amber-900 dark:text-amber-200">
                This project declares AI assistance: {project.aiUsageNote || 'no usage note provided'}.
                The content was still drafted and approved by the team.
              </CardDescription>
            </Card>
          )}

          {project.reviewFeedback && (
            <Card>
              <CardTitle>Reviewer feedback</CardTitle>
              <p className="mt-3 whitespace-pre-line text-sm text-slate-700 dark:text-slate-200">{project.reviewFeedback}</p>
              {project.reviewedAt && (
                <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">Reviewed {formatDate(project.reviewedAt)}</p>
              )}
            </Card>
          )}
        </section>

        <aside className="space-y-4">
          <Card>
            <CardTitle>At a glance</CardTitle>
            <dl className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Status</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{titleCase(project.status)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Submitted</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">
                  {project.submissionDate ? formatDate(project.submissionDate) : 'Not submitted'}
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">Team</dt>
                <dd className="text-right font-medium text-slate-800 dark:text-slate-200">
                  {project.teamId ? (
                    <Link className="text-brand-700 dark:text-brand-300 hover:underline" href={`/teams/${project.teamId}`}>
                      {project.teamName}
                    </Link>
                  ) : (
                    '—'
                  )}
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500 dark:text-slate-400">AI assisted</dt>
                <dd className="font-medium text-slate-800 dark:text-slate-200">{project.aiAssisted ? 'Yes' : 'No'}</dd>
              </div>
            </dl>
          </Card>

          {isStaff && (
            <Card>
              <CardTitle>Human review</CardTitle>
              <CardDescription>
                AI can draft feedback below, but approving or rejecting always stays with a person.
              </CardDescription>

              <label htmlFor="review-feedback" className="mt-3 block text-sm font-medium text-slate-700 dark:text-slate-200">
                Feedback for the team
              </label>
              <Textarea
                id="review-feedback"
                rows={4}
                className="mt-1"
                value={feedback}
                onChange={(event) => setFeedback(event.target.value)}
                placeholder="What worked, what to improve before completion?"
              />

              <div className="mt-3 flex flex-wrap gap-2">
                <Button
                  size="sm"
                  variant="secondary"
                  loading={suggest.isPending}
                  onClick={() => suggest.mutate()}
                >
                  Draft with AI
                </Button>
                {project.status === 'SUBMITTED' && (
                  <Button size="sm" loading={review.isPending} disabled={!canReview} onClick={() => review.mutate('START')}>
                    Start review
                  </Button>
                )}
                <Button
                  size="sm"
                  loading={review.isPending}
                  disabled={!isStaff || project.status !== 'UNDER_REVIEW'}
                  onClick={() => review.mutate('APPROVE')}
                >
                  Approve
                </Button>
                <Button
                  size="sm"
                  variant="secondary"
                  className="!text-rose-700 dark:!text-rose-200 ring-rose-200 dark:ring-rose-500/30"
                  loading={review.isPending}
                  disabled={!canReview}
                  onClick={() => review.mutate('REQUEST_CHANGES')}
                >
                  Request changes
                </Button>
                <Button
                  size="sm"
                  variant="secondary"
                  loading={complete.isPending}
                  disabled={!isStaff || project.status !== 'APPROVED'}
                  onClick={() => complete.mutate()}
                >
                  Mark complete
                </Button>
              </div>
            </Card>
          )}
        </aside>
      </div>
    </>
  );
}

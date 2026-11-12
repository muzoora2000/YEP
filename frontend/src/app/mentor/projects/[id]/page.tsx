'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, CheckCircle, MessageSquare, AlertTriangle } from 'lucide-react';
import { api } from '@/lib/api';
import { PageHeader, SpinnerRow, EmptyState, ErrorState, Alert } from '@/components/ui/Feedback';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Field, Select, Textarea } from '@/components/ui/Form';
import { RequireAuth } from '@/components/RequireAuth';

interface Milestone { milestoneId: number; title: string; dueDate: string; status: string; }
interface ProjectResponse {
  projectId: number; title: string; description: string; challengeArea: string;
  status: string; teamId: number; teamName: string; aiUsed: boolean;
  aiUsageDeclaration: string | null; submittedAt: string | null; milestones: Milestone[];
}
interface ReviewResponse {
  reviewId: number; decision: string; feedback: string; createdAt: string; mentorName: string;
}

const DECISIONS = [
  { value: 'COMMENT', label: 'Comment (no status change)' },
  { value: 'CHANGES_REQUESTED', label: 'Request changes' },
  { value: 'APPROVED', label: 'Approve project' },
];

function ProjectReview({ projectId }: { projectId: number }) {
  const qc = useQueryClient();
  const [decision, setDecision] = useState('COMMENT');
  const [feedback, setFeedback] = useState('');
  const [submitError, setSubmitError] = useState('');

  const { data: project, isLoading, error: loadError } = useQuery<ProjectResponse>({
    queryKey: ['project', projectId],
    queryFn: () => api<ProjectResponse>(`/api/v1/projects/${projectId}`),
  });

  const { data: reviews } = useQuery<ReviewResponse[]>({
    queryKey: ['project-reviews', projectId],
    queryFn: () => api<ReviewResponse[]>(`/api/v1/projects/${projectId}/reviews`),
  });

  // Polymorphism: review() dispatches approve/changes-requested/comment by decision value
  const reviewMut = useMutation({
    mutationFn: () => api(`/api/v1/projects/${projectId}/reviews`, {
      method: 'POST',
      body: { decision, feedback },
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['project', projectId] });
      qc.invalidateQueries({ queryKey: ['project-reviews', projectId] });
      qc.invalidateQueries({ queryKey: ['mentor-projects'] });
      setFeedback('');
      setSubmitError('');
    },
    onError: () => setSubmitError('Could not submit review. Please try again.'),
  });

  if (isLoading) return <SpinnerRow />;
  if (loadError || !project) return <ErrorState message="Could not load project." />;

  const canReview = ['SUBMITTED', 'UNDER_REVIEW', 'CHANGES_REQUESTED'].includes(project.status);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex items-center gap-3">
        <LinkButton href="/mentor" size="sm" variant="secondary">
          <ArrowLeft className="h-4 w-4" /> Back
        </LinkButton>
        <PageHeader title={project.title} description={`${project.teamName} · ${project.challengeArea}`} />
      </div>

      <Badge tone={project.status}>{project.status.replace('_', ' ')}</Badge>

      {/* Description */}
      <Card className="space-y-3 p-5">
        <h2 className="font-semibold text-slate-900 dark:text-slate-50">Description</h2>
        <p className="whitespace-pre-wrap text-slate-700 dark:text-slate-300">{project.description}</p>
        {project.aiUsed && (
          <div className="flex items-start gap-2 rounded-lg bg-blue-50 p-3 text-sm text-blue-800 dark:bg-blue-900/20 dark:text-blue-300">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
            <span><strong>AI-assisted:</strong> {project.aiUsageDeclaration ?? 'Team declared use of AI tools.'}</span>
          </div>
        )}
      </Card>

      {/* Milestones */}
      <Card className="p-5">
        <h2 className="mb-3 font-semibold">Milestones</h2>
        {(!project.milestones || project.milestones.length === 0)
          ? <EmptyState title="No milestones" description="The team has not added milestones yet." />
          : (
            <ul className="divide-y divide-slate-100 dark:divide-slate-700">
              {project.milestones.map(m => (
                <li key={m.milestoneId} className="flex items-center justify-between py-3">
                  <div>
                    <p className="font-medium">{m.title}</p>
                    <p className="text-sm text-slate-500">Due {m.dueDate}</p>
                  </div>
                  <Badge tone={m.status}>{m.status}</Badge>
                </li>
              ))}
            </ul>
          )
        }
      </Card>

      {/* Review form */}
      {canReview && (
        <Card className="space-y-4 p-5">
          <h2 className="font-semibold">Submit review</h2>
          <Field label="Decision" htmlFor="decision">
            <Select id="decision" value={decision} onChange={e => setDecision(e.target.value)}>
              {DECISIONS.map(d => <option key={d.value} value={d.value}>{d.label}</option>)}
            </Select>
          </Field>
          <Field label="Feedback (shown to the team)" htmlFor="feedback">
            <Textarea
              id="feedback"
              value={feedback}
              onChange={e => setFeedback(e.target.value)}
              rows={4}
              placeholder="Share specific, constructive feedback…"
            />
          </Field>
          {submitError && <Alert tone="error">{submitError}</Alert>}
          <Button
            onClick={() => reviewMut.mutate()}
            disabled={reviewMut.isPending || !feedback.trim()}
            loading={reviewMut.isPending}
          >
            Submit review
          </Button>
        </Card>
      )}

      {/* Review history */}
      <section>
        <h2 className="mb-3 text-base font-semibold">Review history</h2>
        {(!reviews || reviews.length === 0)
          ? <EmptyState title="No reviews" description="No reviews have been submitted yet." />
          : (
            <div className="space-y-3">
              {reviews.map(r => (
                <Card key={r.reviewId} className="p-4">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-2">
                      {r.decision === 'APPROVED'
                        ? <CheckCircle className="h-4 w-4 text-green-600" />
                        : <MessageSquare className="h-4 w-4 text-slate-500" />}
                      <span className="font-medium">{r.mentorName}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge tone={r.decision}>{r.decision.replace('_', ' ')}</Badge>
                      <span className="text-xs text-slate-400">{new Date(r.createdAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                  <p className="mt-2 text-sm text-slate-700 dark:text-slate-300">{r.feedback}</p>
                </Card>
              ))}
            </div>
          )
        }
      </section>
    </div>
  );
}

export default function MentorProjectPage({ params }: { params: { id: string } }) {
  return (
    <RequireAuth roles={['MENTOR', 'ADMIN']}>
      <ProjectReview projectId={Number(params.id)} />
    </RequireAuth>
  );
}

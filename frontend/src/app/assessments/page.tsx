'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { assessmentSchema, type AssessmentValues } from '@/lib/schemas';
import { formatDate, titleCase } from '@/lib/format';
import type { Assessment, Certificate } from '@/lib/types';
import { RequireAuth } from '@/components/RequireAuth';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';

function AssessmentCard({ assessment }: { assessment: Assessment }) {
  return (
    <Card as="article" className="!p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-900">{assessment.criteria}</p>
          <p className="mt-0.5 text-xs text-slate-500">
            {assessment.targetLabel ? `${titleCase(assessment.targetType)}: ${assessment.targetLabel}` : titleCase(assessment.targetType)}
            {assessment.assessorName ? ` · by ${assessment.assessorName}` : ''} · {formatDate(assessment.assessmentDate)}
          </p>
        </div>
        <div className="text-right">
          <Badge tone={assessment.pass ? 'COMPLETED' : 'REJECTED'}>
            {assessment.pass ? 'Pass' : 'Needs work'}
          </Badge>
          <p className="mt-1 text-lg font-bold text-slate-900">{assessment.score}</p>
        </div>
      </div>
      {assessment.feedback && (
        <p className="mt-3 whitespace-pre-line text-sm text-slate-700">{assessment.feedback}</p>
      )}
    </Card>
  );
}

function StaffAssessmentForm() {
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<AssessmentValues>({
    resolver: zodResolver(assessmentSchema),
    defaultValues: { targetType: 'PARTICIPANT', criteria: '', score: 0, feedback: '' },
  });

  const targetType = watch('targetType');

  const create = useMutation({
    mutationFn: (values: AssessmentValues) => api<Assessment>('/api/v1/assessments', { method: 'POST', body: values }),
    onSuccess: () => {
      setNotice('Assessment recorded.');
      reset();
      void queryClient.invalidateQueries({ queryKey: ['assessments'] });
      void queryClient.invalidateQueries({ queryKey: ['certificates', 'mine'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not record the assessment.'),
  });

  return (
    <Card>
      <CardTitle>New assessment</CardTitle>
      <CardDescription>Scores drive pass/fail and certificate eligibility.</CardDescription>

      <form className="mt-4 space-y-4" onSubmit={handleSubmit((values) => create.mutate(values))} noValidate>
        {notice && <Alert tone={notice.includes('Could') ? 'error' : 'success'}>{notice}</Alert>}

        <Field label="Assess" htmlFor="target-type" required>
          <Select id="target-type" {...register('targetType')}>
            <option value="PARTICIPANT">A participant</option>
            <option value="TEAM">A team</option>
          </Select>
        </Field>

        <Field
          label={targetType === 'TEAM' ? 'Team ID' : 'Participant ID'}
          htmlFor="target-id"
          error={targetType === 'TEAM' ? errors.teamId?.message : errors.participantId?.message}
          required
        >
          <Input
            id="target-id"
            type="number"
            min={1}
            inputMode="numeric"
            invalid={!!(targetType === 'TEAM' ? errors.teamId : errors.participantId)}
            {...register(targetType === 'TEAM' ? 'teamId' : 'participantId')}
          />
        </Field>

        <Field label="Criteria" htmlFor="criteria" error={errors.criteria?.message} required>
          <Input id="criteria" placeholder="Collaboration and communication" invalid={!!errors.criteria} {...register('criteria')} />
        </Field>

        <Field label="Score (0-100)" htmlFor="score" error={errors.score?.message} required hint="60 and above is a pass">
          <Input
            id="score"
            type="number"
            min={0}
            max={100}
            inputMode="numeric"
            invalid={!!errors.score}
            {...register('score')}
          />
        </Field>

        <Field label="Feedback" htmlFor="feedback" error={errors.feedback?.message}>
          <Textarea id="feedback" rows={3} {...register('feedback')} />
        </Field>

        <Button type="submit" loading={isSubmitting || create.isPending}>
          Record assessment
        </Button>
      </form>
    </Card>
  );
}

function AssessmentsContent() {
  const { isStaff } = useAuth();

  const aboutMe = useQuery({
    queryKey: ['assessments', 'about-me'],
    queryFn: () => api<Assessment[]>('/api/v1/assessments/about-me'),
    enabled: !isStaff,
  });

  const givenByMe = useQuery({
    queryKey: ['assessments', 'mine'],
    queryFn: () => api<Assessment[]>('/api/v1/assessments?mine=true'),
    enabled: isStaff,
  });

  const certificates = useQuery({
    queryKey: ['certificates', 'mine'],
    queryFn: () => api<Certificate[]>('/api/v1/certificates/mine'),
  });

  const list = isStaff ? givenByMe : aboutMe;
  const loading = list.isLoading;
  const error = list.isError;

  return (
    <>
      <PageHeader
        title="Assessments & certificates"
        description={
          isStaff
            ? 'The assessments you have given, plus a form to record new ones. Participants see feedback written about them.'
            : 'Feedback written about you by facilitators and mentors, plus the certificates you have earned.'
        }
        actions={<LinkButton href="/dashboard" size="sm" variant="secondary">Dashboard</LinkButton>}
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="space-y-4 lg:col-span-2">
          <h2 className="text-base font-semibold text-slate-900">
            {isStaff ? 'Assessments you have given' : 'My feedback'}
          </h2>

          {loading ? (
            <SpinnerRow label="Loading assessments…" />
          ) : error ? (
            <ErrorState
              message={list.error instanceof ApiError ? list.error.message : 'Could not load assessments.'}
              onRetry={() => list.refetch()}
            />
          ) : (list.data ?? []).length === 0 ? (
            <EmptyState
              title={isStaff ? 'No assessments recorded yet' : 'No feedback yet'}
              description={
                isStaff
                  ? 'Use the form to score participants and teams against your criteria.'
                  : 'Once a facilitator assesses you, the score and feedback appear here.'
              }
            />
          ) : (
            <div className="space-y-3">
              {(list.data ?? []).map((assessment) => (
                <AssessmentCard key={assessment.id} assessment={assessment} />
              ))}
            </div>
          )}
        </section>

        <aside className="space-y-4">
          {isStaff && <StaffAssessmentForm />}

          <Card>
            <CardTitle>My certificates</CardTitle>
            <CardDescription>Verifiable with the code on the certificate.</CardDescription>
            {(certificates.data ?? []).length === 0 ? (
              <p className="mt-3 text-sm text-slate-600">
                No certificates yet - complete a Level One activity or an approved impact project.
              </p>
            ) : (
              <ul className="mt-3 space-y-2">
                {(certificates.data ?? []).map((certificate) => (
                  <li key={certificate.id} className="flex items-center justify-between gap-2">
                    <div>
                      <p className="text-sm font-medium text-slate-900">{certificate.title}</p>
                      <p className="text-xs text-slate-500">{formatDate(certificate.issueDate)}</p>
                    </div>
                    <LinkButton href={`/certificates/${certificate.verificationCode}`} size="sm" variant="secondary">
                      Verify
                    </LinkButton>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </aside>
      </div>
    </>
  );
}

export default function AssessmentsPage() {
  return (
    <RequireAuth>
      <AssessmentsContent />
    </RequireAuth>
  );
}

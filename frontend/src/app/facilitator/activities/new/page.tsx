'use client';

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { api } from '@/lib/api';
import { PageHeader, Alert } from '@/components/ui/Feedback';
import { Card } from '@/components/ui/Card';
import { Button, LinkButton } from '@/components/ui/Button';
import { Field, Input, Textarea, Select } from '@/components/ui/Form';
import { RequireAuth } from '@/components/RequireAuth';

const schema = z.object({
  title: z.string().min(5, 'Title must be at least 5 characters').max(190),
  description: z.string().min(20, 'Description must be at least 20 characters').max(4000),
  level: z.enum(['ONE', 'TWO']),
  category: z.string().min(2, 'Category is required'),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().optional(),
  capacity: z.coerce.number().int().min(1).optional().or(z.literal('')),
  passMark: z.coerce.number().int().min(0).max(100).default(50),
  minAttendancePercent: z.coerce.number().int().min(0).max(100).default(70),
});
type FormValues = z.infer<typeof schema>;

const CATEGORIES = ['Digital Literacy', 'AI Literacy', 'Critical Thinking', 'Media Literacy',
  'Intercultural Communication', 'Employment', 'Education', 'Agriculture',
  'Health', 'Climate Change', 'Digital Inclusion', 'Other'];

function NewActivityForm() {
  const router = useRouter();
  const qc = useQueryClient();
  const [serverError, setServerError] = useState('');

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { level: 'ONE', passMark: 50, minAttendancePercent: 70 },
  });

  const mutation = useMutation({
    mutationFn: (data: FormValues) => api<{ activityId: number }>('/api/v1/activities', {
      method: 'POST',
      body: { ...data, capacity: data.capacity === '' ? null : data.capacity },
    }),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['facilitator-activities'] });
      router.push(`/facilitator/activities/${created.activityId}/materials`);
    },
    onError: (e: Error) => setServerError(e.message || 'Could not create activity.'),
  });

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-center gap-3">
        <LinkButton href="/facilitator" size="sm" variant="secondary">
          <ArrowLeft className="h-4 w-4" /> Back
        </LinkButton>
        <PageHeader title="New activity" description="Create a Level One or Level Two virtual exchange." />
      </div>

      <Card className="p-6">
        <form onSubmit={handleSubmit(d => mutation.mutate(d))} className="space-y-5" noValidate>
          <Field label="Title" htmlFor="title" error={errors.title?.message} required>
            <Input id="title" {...register('title')} placeholder="e.g. AI Literacy for Youth, Cohort 3" />
          </Field>

          <Field label="Description" htmlFor="description" error={errors.description?.message} required>
            <Textarea id="description" {...register('description')} rows={5}
              placeholder="Describe what participants will learn and do…" />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Level" htmlFor="level" error={errors.level?.message} required>
              <Select id="level" {...register('level')}>
                <option value="ONE">Level 1 – Group learning</option>
                <option value="TWO">Level 2 – Impact projects</option>
              </Select>
            </Field>
            <Field label="Category" htmlFor="category" error={errors.category?.message} required>
              <Select id="category" {...register('category')}>
                <option value="">Select category…</option>
                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </Select>
            </Field>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Start date" htmlFor="startDate" error={errors.startDate?.message} required>
              <Input id="startDate" type="date" {...register('startDate')} />
            </Field>
            <Field label="End date (optional)" htmlFor="endDate" error={errors.endDate?.message}>
              <Input id="endDate" type="date" {...register('endDate')} />
            </Field>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <Field label="Capacity (blank = unlimited)" htmlFor="capacity" error={errors.capacity?.message?.toString()}>
              <Input id="capacity" type="number" min={1} {...register('capacity')} placeholder="e.g. 100" />
            </Field>
            <Field label="Pass mark %" htmlFor="passMark" error={errors.passMark?.message} required>
              <Input id="passMark" type="number" min={0} max={100} {...register('passMark')} />
            </Field>
            <Field label="Min attendance %" htmlFor="minAttendancePercent" error={errors.minAttendancePercent?.message} required>
              <Input id="minAttendancePercent" type="number" min={0} max={100} {...register('minAttendancePercent')} />
            </Field>
          </div>

          {serverError && <Alert tone="error">{serverError}</Alert>}
          <Button type="submit" disabled={isSubmitting || mutation.isPending} loading={mutation.isPending} className="w-full">
            Create activity
          </Button>
        </form>
      </Card>
    </div>
  );
}

export default function NewActivityPage() {
  return (
    <RequireAuth roles={['FACILITATOR', 'ADMIN']}>
      <NewActivityForm />
    </RequireAuth>
  );
}

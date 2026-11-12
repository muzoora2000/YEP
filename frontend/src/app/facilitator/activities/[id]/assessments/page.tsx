'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Plus, Eye, EyeOff } from 'lucide-react';
import { api } from '@/lib/api';
import { PageHeader, SpinnerRow, EmptyState, ErrorState, Alert } from '@/components/ui/Feedback';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Field, Input, Textarea, Select } from '@/components/ui/Form';
import { RequireAuth } from '@/components/RequireAuth';
import { ActivityNav } from '@/components/ActivityNav';

interface TaskResponse {
  taskId: number; title: string; taskType: string;
  instructions: string; maxScore: number; dueDate: string | null; published: boolean;
}

function AssessmentsManager({ activityId }: { activityId: number }) {
  const qc = useQueryClient();
  const [adding, setAdding] = useState(false);
  const [title, setTitle] = useState('');
  const [taskType, setTaskType] = useState<'QUIZ' | 'ASSIGNMENT'>('ASSIGNMENT');
  const [instructions, setInstructions] = useState('');
  const [maxScore, setMaxScore] = useState(100);
  const [dueDate, setDueDate] = useState('');
  const [err, setErr] = useState('');

  const { data: tasks, isLoading, error } = useQuery<TaskResponse[]>({
    queryKey: ['tasks', activityId],
    queryFn: () => api<TaskResponse[]>(`/api/v1/activities/${activityId}/tasks`),
  });

  const addMut = useMutation({
    mutationFn: () => api(`/api/v1/activities/${activityId}/tasks`, {
      method: 'POST',
      body: {
        title, taskType, instructions, maxScore,
        dueDate: dueDate || null,
        questions: taskType === 'QUIZ' ? [] : null,
        published: false,
      },
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tasks', activityId] });
      setAdding(false); setTitle(''); setInstructions(''); setMaxScore(100); setDueDate(''); setErr('');
    },
    onError: () => setErr('Could not create assessment task.'),
  });

  const publishMut = useMutation({
    mutationFn: (t: TaskResponse) =>
      api(`/api/v1/tasks/${t.taskId}`, {
        method: 'PUT',
        body: {
          title: t.title, taskType: t.taskType, instructions: t.instructions,
          maxScore: t.maxScore, dueDate: t.dueDate, published: !t.published,
        },
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tasks', activityId] }),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Assessment tasks" description={`Activity #${activityId}`}
        actions={
          <>
            <LinkButton href="/facilitator" size="sm" variant="secondary"><ArrowLeft className="h-4 w-4" /> Back</LinkButton>
            <Button size="sm" onClick={() => setAdding(v => !v)}>
              <Plus className="h-4 w-4" /> {adding ? 'Cancel' : 'Create task'}
            </Button>
          </>
        }
      />

      <ActivityNav activityId={activityId} active="assessments" />

      {adding && (
        <Card className="space-y-4 p-5">
          <h3 className="font-semibold">New assessment task</h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Title" htmlFor="task-title" required>
              <Input id="task-title" value={title} onChange={e => setTitle(e.target.value)} placeholder="e.g. Week 2 quiz" />
            </Field>
            <Field label="Type" htmlFor="task-type" required>
              <Select id="task-type" value={taskType} onChange={e => setTaskType(e.target.value as 'QUIZ' | 'ASSIGNMENT')}>
                <option value="ASSIGNMENT">Assignment (text submission)</option>
                <option value="QUIZ">Quiz (auto-graded MCQ)</option>
              </Select>
            </Field>
          </div>
          <Field label="Instructions" htmlFor="task-instr" required>
            <Textarea id="task-instr" value={instructions} onChange={e => setInstructions(e.target.value)} rows={4}
              placeholder="Describe what participants must do…" />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Maximum score" htmlFor="task-score">
              <Input id="task-score" type="number" min={1} value={maxScore} onChange={e => setMaxScore(Number(e.target.value))} />
            </Field>
            <Field label="Due date (optional)" htmlFor="task-due">
              <Input id="task-due" type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} />
            </Field>
          </div>
          {taskType === 'QUIZ' && (
            <Alert tone="info">Save this draft first, then edit it to add quiz questions with answer options.</Alert>
          )}
          {err && <Alert tone="error">{err}</Alert>}
          <Button onClick={() => addMut.mutate()} disabled={!title || !instructions || addMut.isPending} loading={addMut.isPending}>
            Save draft
          </Button>
        </Card>
      )}

      {isLoading && <SpinnerRow />}
      {error && <ErrorState message="Could not load tasks." />}
      {!isLoading && tasks?.length === 0 && <EmptyState title="No tasks" description="Create the first assessment task above." />}

      <div className="space-y-3">
        {tasks?.map(t => (
          <Card key={t.taskId} className="flex items-start justify-between gap-4 p-4">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <Badge tone={t.taskType}>{t.taskType}</Badge>
                <p className="font-medium">{t.title}</p>
                {!t.published && <Badge>Draft</Badge>}
              </div>
              <p className="mt-1 line-clamp-2 text-sm text-slate-500">{t.instructions}</p>
              <p className="mt-1 text-xs text-slate-400">
                Max score: {t.maxScore}{t.dueDate ? ` · Due: ${t.dueDate}` : ''}
              </p>
            </div>
            <Button size="sm" variant="secondary"
              onClick={() => publishMut.mutate(t)}
              title={t.published ? 'Unpublish' : 'Publish to participants'}>
              {t.published ? <EyeOff className="h-3 w-3" /> : <Eye className="h-3 w-3" />}
            </Button>
          </Card>
        ))}
      </div>
    </div>
  );
}

export default function AssessmentsPage({ params }: { params: { id: string } }) {
  return (
    <RequireAuth roles={['FACILITATOR', 'ADMIN']}>
      <AssessmentsManager activityId={Number(params.id)} />
    </RequireAuth>
  );
}

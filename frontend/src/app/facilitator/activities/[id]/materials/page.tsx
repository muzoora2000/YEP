'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Plus, Eye, EyeOff, Trash2 } from 'lucide-react';
import { api } from '@/lib/api';
import { PageHeader, SpinnerRow, EmptyState, ErrorState, Alert } from '@/components/ui/Feedback';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Field, Input, Textarea, Select } from '@/components/ui/Form';
import { RequireAuth } from '@/components/RequireAuth';
import { ActivityNav } from '@/components/ActivityNav';

interface Material {
  materialId: number; title: string; materialType: string;
  url: string; description: string | null; published: boolean;
}

const TYPES = ['VIDEO', 'PDF', 'LINK', 'SLIDES', 'TEXT'];

function MaterialsManager({ activityId }: { activityId: number }) {
  const qc = useQueryClient();
  const [adding, setAdding] = useState(false);
  const [title, setTitle] = useState('');
  const [matType, setMatType] = useState('LINK');
  const [url, setUrl] = useState('');
  const [description, setDescription] = useState('');
  const [err, setErr] = useState('');

  const { data: materials, isLoading, error } = useQuery<Material[]>({
    queryKey: ['materials', activityId],
    queryFn: () => api<Material[]>(`/api/v1/activities/${activityId}/materials`),
  });

  const addMut = useMutation({
    mutationFn: () => api(`/api/v1/activities/${activityId}/materials`, {
      method: 'POST',
      body: { title, materialType: matType, url, description: description || null, published: false },
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['materials', activityId] });
      setAdding(false); setTitle(''); setUrl(''); setDescription(''); setErr('');
    },
    onError: () => setErr('Could not add material.'),
  });

  const publishMut = useMutation({
    mutationFn: (m: Material) =>
      api(`/api/v1/materials/${m.materialId}`, {
        method: 'PUT',
        body: {
          title: m.title, materialType: m.materialType, url: m.url,
          description: m.description, published: !m.published,
        },
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['materials', activityId] }),
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => api(`/api/v1/materials/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['materials', activityId] }),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Learning materials" description={`Activity #${activityId}`}
        actions={
          <>
            <LinkButton href="/facilitator" size="sm" variant="secondary"><ArrowLeft className="h-4 w-4" /> Back</LinkButton>
            <Button size="sm" onClick={() => setAdding(v => !v)}>
              <Plus className="h-4 w-4" /> {adding ? 'Cancel' : 'Add material'}
            </Button>
          </>
        }
      />

      <ActivityNav activityId={activityId} active="materials" />

      {adding && (
        <Card className="space-y-4 p-5">
          <h3 className="font-semibold">New material</h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Title" htmlFor="mat-title" required>
              <Input id="mat-title" value={title} onChange={e => setTitle(e.target.value)} placeholder="e.g. Week 1 reading" />
            </Field>
            <Field label="Type" htmlFor="mat-type" required>
              <Select id="mat-type" value={matType} onChange={e => setMatType(e.target.value)}>
                {TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </Select>
            </Field>
          </div>
          <Field label="URL / link" htmlFor="mat-url" required>
            <Input id="mat-url" value={url} onChange={e => setUrl(e.target.value)} placeholder="https://…" />
          </Field>
          <Field label="Description (optional)" htmlFor="mat-desc">
            <Textarea id="mat-desc" value={description} onChange={e => setDescription(e.target.value)} rows={2} />
          </Field>
          {err && <Alert tone="error">{err}</Alert>}
          <Button onClick={() => addMut.mutate()} disabled={!title || !url || addMut.isPending} loading={addMut.isPending}>
            Save material
          </Button>
        </Card>
      )}

      {isLoading && <SpinnerRow />}
      {error && <ErrorState message="Could not load materials." />}
      {!isLoading && materials?.length === 0 && <EmptyState title="No materials" description="Add the first material above." />}

      <div className="space-y-3">
        {materials?.map(m => (
          <Card key={m.materialId} className="flex items-start justify-between gap-4 p-4">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <Badge tone={m.materialType}>{m.materialType}</Badge>
                <p className="truncate font-medium">{m.title}</p>
                {!m.published && <Badge>Draft</Badge>}
              </div>
              {m.description && <p className="mt-1 truncate text-sm text-slate-500">{m.description}</p>}
              <a href={m.url} target="_blank" rel="noreferrer"
                className="mt-1 block truncate text-xs text-brand-600 hover:underline">{m.url}</a>
            </div>
            <div className="flex shrink-0 gap-2">
              <Button size="sm" variant="secondary"
                onClick={() => publishMut.mutate(m)}
                title={m.published ? 'Unpublish' : 'Publish'}>
                {m.published ? <EyeOff className="h-3 w-3" /> : <Eye className="h-3 w-3" />}
              </Button>
              <Button size="sm" variant="danger"
                onClick={() => { if (confirm('Delete this material?')) deleteMut.mutate(m.materialId); }}>
                <Trash2 className="h-3 w-3" />
              </Button>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}

export default function MaterialsPage({ params }: { params: { id: string } }) {
  return (
    <RequireAuth roles={['FACILITATOR', 'ADMIN']}>
      <MaterialsManager activityId={Number(params.id)} />
    </RequireAuth>
  );
}

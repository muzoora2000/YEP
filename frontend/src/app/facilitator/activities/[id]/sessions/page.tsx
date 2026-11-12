'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Plus, ExternalLink } from 'lucide-react';
import { api } from '@/lib/api';
import { PageHeader, SpinnerRow, EmptyState, ErrorState, Alert } from '@/components/ui/Feedback';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Field, Input } from '@/components/ui/Form';
import { RequireAuth } from '@/components/RequireAuth';
import { ActivityNav } from '@/components/ActivityNav';

interface VirtualSession {
  sessionId: number; title: string; scheduledStart: string;
  durationMinutes: number; meetingLink: string; status: string;
}

function SessionsManager({ activityId }: { activityId: number }) {
  const qc = useQueryClient();
  const [adding, setAdding] = useState(false);
  const [title, setTitle] = useState('');
  const [start, setStart] = useState('');
  const [duration, setDuration] = useState(60);
  const [link, setLink] = useState('');
  const [err, setErr] = useState('');

  const { data: sessions, isLoading, error } = useQuery<VirtualSession[]>({
    queryKey: ['sessions', activityId],
    queryFn: () => api<VirtualSession[]>(`/api/v1/activities/${activityId}/sessions`),
  });

  const addMut = useMutation({
    mutationFn: () => api(`/api/v1/activities/${activityId}/sessions`, {
      method: 'POST',
      body: { title, scheduledStart: new Date(start).toISOString(), durationMinutes: duration, meetingLink: link },
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sessions', activityId] });
      setAdding(false); setTitle(''); setStart(''); setDuration(60); setLink(''); setErr('');
    },
    onError: () => setErr('Could not create session.'),
  });

  const updateStatusMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      api(`/api/v1/sessions/${id}/status`, { method: 'PUT', body: { status } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sessions', activityId] }),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Virtual sessions" description={`Activity #${activityId}`}
        actions={
          <>
            <LinkButton href="/facilitator" size="sm" variant="secondary"><ArrowLeft className="h-4 w-4" /> Back</LinkButton>
            <Button size="sm" onClick={() => setAdding(v => !v)}>
              <Plus className="h-4 w-4" /> {adding ? 'Cancel' : 'Schedule session'}
            </Button>
          </>
        }
      />

      <ActivityNav activityId={activityId} active="sessions" />

      {adding && (
        <Card className="space-y-4 p-5">
          <h3 className="font-semibold">New session</h3>
          <Field label="Session title" htmlFor="sess-title" required>
            <Input id="sess-title" value={title} onChange={e => setTitle(e.target.value)} placeholder="e.g. Week 2 live Q&A" />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Start date and time" htmlFor="sess-start" required>
              <Input id="sess-start" type="datetime-local" value={start} onChange={e => setStart(e.target.value)} />
            </Field>
            <Field label="Duration (minutes)" htmlFor="sess-dur" required>
              <Input id="sess-dur" type="number" min={15} value={duration} onChange={e => setDuration(Number(e.target.value))} />
            </Field>
          </div>
          <Field label="Meeting link (Google Meet / Zoom / Jitsi)" htmlFor="sess-link" required>
            <Input id="sess-link" value={link} onChange={e => setLink(e.target.value)} placeholder="https://meet.google.com/…" />
          </Field>
          {err && <Alert tone="error">{err}</Alert>}
          <Button onClick={() => addMut.mutate()} disabled={!title || !start || !link || addMut.isPending} loading={addMut.isPending}>
            Schedule
          </Button>
        </Card>
      )}

      {isLoading && <SpinnerRow />}
      {error && <ErrorState message="Could not load sessions." />}
      {!isLoading && sessions?.length === 0 && <EmptyState title="No sessions" description="No sessions scheduled yet." />}

      <div className="space-y-3">
        {sessions?.map(s => (
          <Card key={s.sessionId} className="p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <Badge tone={s.status}>{s.status}</Badge>
                  <p className="font-medium">{s.title}</p>
                </div>
                <p className="mt-1 text-sm text-slate-500">
                  {new Date(s.scheduledStart).toLocaleString()} · {s.durationMinutes} min
                </p>
                <a href={s.meetingLink} target="_blank" rel="noreferrer"
                  className="mt-1 flex items-center gap-1 text-sm text-brand-600 hover:underline">
                  <ExternalLink className="h-3 w-3" /> {s.meetingLink}
                </a>
              </div>
              {/* Encapsulation: status transitions guarded by backend service */}
              <div className="flex gap-2">
                {s.status === 'SCHEDULED' && (
                  <Button size="sm" onClick={() => updateStatusMut.mutate({ id: s.sessionId, status: 'LIVE' })}>
                    Go live
                  </Button>
                )}
                {s.status === 'LIVE' && (
                  <Button size="sm" onClick={() => updateStatusMut.mutate({ id: s.sessionId, status: 'COMPLETED' })}>
                    End session
                  </Button>
                )}
                {(s.status === 'SCHEDULED' || s.status === 'LIVE') && (
                  <Button size="sm" variant="danger"
                    onClick={() => updateStatusMut.mutate({ id: s.sessionId, status: 'CANCELLED' })}>
                    Cancel
                  </Button>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}

export default function SessionsPage({ params }: { params: { id: string } }) {
  return (
    <RequireAuth roles={['FACILITATOR', 'ADMIN']}>
      <SessionsManager activityId={Number(params.id)} />
    </RequireAuth>
  );
}

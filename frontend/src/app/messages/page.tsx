'use client';

import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { announcementSchema, messageSchema, type AnnouncementValues, type MessageValues } from '@/lib/schemas';
import { formatDateTime, titleCase } from '@/lib/format';
import type {
  AiSuggestion,
  Enrollment,
  Exchange,
  Message,
  PageResponse,
  Team,
} from '@/lib/types';
import { RequireAuth } from '@/components/RequireAuth';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';

type Tab = 'inbox' | 'announcements';

function MessageRow({ message, onRead }: { message: Message; onRead: (id: number) => void }) {
  return (
    <li>
      <button
        type="button"
        onClick={() => !message.read && onRead(message.id)}
        className={`w-full rounded-xl border px-4 py-3 text-left transition ${
          message.read
            ? 'border-slate-200 bg-white'
            : 'border-brand-200 bg-brand-50/60 hover:bg-brand-50'
        }`}
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-sm font-semibold text-slate-900">
              {message.subject || (message.messageType === 'ANNOUNCEMENT' ? 'Announcement' : 'Message')}
              {!message.read && <span className="ml-2 text-xs font-bold text-brand-700">NEW</span>}
            </p>
            <p className="text-xs text-slate-500">
              {message.messageType === 'ANNOUNCEMENT'
                ? `From ${message.senderName ?? 'YEP'} · scope ${message.scope ?? 'ALL_USERS'}`
                : `From ${message.senderName ?? 'Participant'}`}
              {message.recipientName && message.messageType !== 'ANNOUNCEMENT' ? ` → ${message.recipientName}` : ''}
              {' · '}
              {formatDateTime(message.sentAt)}
            </p>
          </div>
          <div className="flex shrink-0 flex-col items-end gap-1">
            <Badge tone={message.messageType === 'ANNOUNCEMENT' ? 'LEVEL_ONE' : 'DRAFT'}>
              {titleCase(message.messageType)}
            </Badge>
            {message.aiGenerated && <Badge tone="DRAFT">AI draft</Badge>}
            {message.requiresHumanApproval && message.published === false && (
              <Badge tone="REJECTED">Awaiting approval</Badge>
            )}
          </div>
        </div>
        <p className="mt-2 whitespace-pre-line text-sm text-slate-700">{message.body}</p>
      </button>
    </li>
  );
}

function ComposeForm() {
  const queryClient = useQueryClient();
  const { isStaff } = useAuth();
  const [notice, setNotice] = useState<string | null>(null);
  const [mode, setMode] = useState<'TEAM' | 'DIRECT'>('TEAM');
  const [exchangeId, setExchangeId] = useState('');

  const myTeams = useQuery({
    queryKey: ['teams', 'mine'],
    queryFn: () => api<Team[]>('/api/v1/teams'),
  });

  const exchanges = useQuery({
    queryKey: ['exchanges', 'staff-pick'],
    queryFn: () => api<PageResponse<Exchange>>('/api/v1/exchanges?size=100&status=ACTIVE'),
    enabled: isStaff && mode === 'DIRECT',
  });

  const enrolments = useQuery({
    queryKey: ['exchange', exchangeId, 'enrollments'],
    queryFn: () => api<Enrollment[]>(`/api/v1/exchanges/${exchangeId}/enrollments`),
    enabled: isStaff && mode === 'DIRECT' && !!exchangeId,
  });

  const teammateOptions = useMemo(() => {
    const seen = new Map<number, string>();
    for (const team of myTeams.data ?? []) {
      for (const member of team.members ?? []) {
        if (member.participantId && member.fullName) seen.set(member.participantId, member.fullName);
      }
    }
    return [...seen.entries()].map(([id, name]) => ({ id, name }));
  }, [myTeams.data]);

  const staffOptions = useMemo(
    () =>
      (enrolments.data ?? [])
        .filter((enrollment) => enrollment.participantId && enrollment.participantName)
        .map((enrollment) => ({ id: enrollment.participantId as number, name: enrollment.participantName as string })),
    [enrolments.data],
  );

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<MessageValues>({
    resolver: zodResolver(messageSchema),
    defaultValues: { subject: '', body: '' },
  });

  const bodyValue = watch('body');

  const suggest = useMutation({
    mutationFn: () =>
      api<AiSuggestion>('/api/v1/ai/suggest', {
        method: 'POST',
        body: { input: bodyValue || 'Draft a short motivational message for YEP participants', kind: 'MESSAGE' },
      }),
    onSuccess: (suggestion) => reset({ ...watch(), body: suggestion.draft }),
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'AI draft is unavailable.'),
  });

  const send = useMutation({
    mutationFn: (values: MessageValues) =>
      api<Message>('/api/v1/messages', {
        method: 'POST',
        body: {
          recipientId: mode === 'DIRECT' ? values.recipientId : undefined,
          teamId: mode === 'TEAM' ? values.teamId : undefined,
          subject: values.subject || undefined,
          body: values.body,
        },
      }),
    onSuccess: () => {
      setNotice('Message sent.');
      reset({ subject: '', body: '' });
      void queryClient.invalidateQueries({ queryKey: ['messages'] });
      void queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not send the message.'),
  });

  const recipientSource = isStaff ? staffOptions : teammateOptions;

  return (
    <Card>
      <CardTitle>Compose</CardTitle>
      <CardDescription>
        {isStaff
          ? 'Message participants directly, or pick a team inside one of your exchanges.'
          : 'Send a message to a teammate, or to your whole team.'}
      </CardDescription>

      <form className="mt-4 space-y-4" onSubmit={handleSubmit((values) => send.mutate(values))} noValidate>
        {notice && <Alert tone={notice.toLowerCase().includes('could') ? 'error' : 'success'}>{notice}</Alert>}

        <Field label="Send to" htmlFor="message-mode">
          <Select
            id="message-mode"
            value={mode}
            onChange={(event) => setMode(event.target.value as 'TEAM' | 'DIRECT')}
          >
            <option value="TEAM">My team</option>
            <option value="DIRECT">One person</option>
          </Select>
        </Field>

        {mode === 'TEAM' ? (
          <Field label="Team" htmlFor="team-id" error={errors.teamId?.message} required>
            <Select id="team-id" defaultValue="" {...register('teamId')}>
              <option value="" disabled>
                Choose a team
              </option>
              {(myTeams.data ?? []).map((team) => (
                <option key={team.id} value={team.id}>
                  {team.name}
                </option>
              ))}
            </Select>
          </Field>
        ) : (
          <>
            {isStaff && (
              <Field label="Exchange" htmlFor="exchange-pick" required>
                <Select id="exchange-pick" value={exchangeId} onChange={(event) => setExchangeId(event.target.value)}>
                  <option value="">Choose an exchange</option>
                  {(exchanges.data?.content ?? []).map((exchange) => (
                    <option key={exchange.id} value={exchange.id}>
                      {exchange.title}
                    </option>
                  ))}
                </Select>
              </Field>
            )}

            <Field label="Recipient" htmlFor="recipient-id" error={errors.recipientId?.message} required>
              <Select id="recipient-id" defaultValue="" {...register('recipientId')}>
                <option value="" disabled>
                  {recipientSource.length === 0 ? 'No recipients available yet' : 'Choose a person'}
                </option>
                {recipientSource.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.name}
                  </option>
                ))}
              </Select>
            </Field>
          </>
        )}

        <Field label="Subject (optional)" htmlFor="message-subject">
          <Input id="message-subject" maxLength={200} {...register('subject')} />
        </Field>

        <Field label="Message" htmlFor="message-body" error={errors.body?.message} required>
          <Textarea id="message-body" rows={5} invalid={!!errors.body} {...register('body')} />
        </Field>

        <div className="flex flex-wrap items-center gap-2">
          <Button type="submit" loading={isSubmitting || send.isPending}>
            Send
          </Button>
          {isStaff && (
            <Button type="button" variant="secondary" size="sm" loading={suggest.isPending} onClick={() => suggest.mutate()}>
              Draft with AI
            </Button>
          )}
        </div>
      </form>
    </Card>
  );
}

function AnnouncementForm() {
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<AnnouncementValues>({
    resolver: zodResolver(announcementSchema),
    defaultValues: { subject: '', body: '', scope: 'ALL_USERS' },
  });

  const bodyValue = watch('body');

  const suggest = useMutation({
    mutationFn: () =>
      api<AiSuggestion>('/api/v1/ai/suggest', {
        method: 'POST',
        body: { input: bodyValue || 'Announcement about the next Level One session', kind: 'ANNOUNCEMENT' },
      }),
    onSuccess: (suggestion) => reset({ ...watch(), body: suggestion.draft }),
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'AI draft is unavailable.'),
  });

  const create = useMutation({
    mutationFn: (values: AnnouncementValues) =>
      api<Message>('/api/v1/messages/announcements', {
        method: 'POST',
        body: {
          subject: values.subject,
          body: values.body,
          scope: values.scope ?? 'ALL_USERS',
          exchangeId: values.exchangeId || null,
          teamId: values.teamId || null,
        },
      }),
    onSuccess: () => {
      setNotice('Announcement submitted - AI-drafted text waits for human approval first.');
      reset({ subject: '', body: '', scope: 'ALL_USERS' });
      void queryClient.invalidateQueries({ queryKey: ['messages'] });
      void queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
    onError: (error) => setNotice(error instanceof ApiError ? error.message : 'Could not post the announcement.'),
  });

  return (
    <Card>
      <CardTitle>New announcement</CardTitle>
      <CardDescription>Visible to everyone, a single exchange or one team.</CardDescription>
      <form className="mt-4 space-y-4" onSubmit={handleSubmit((values) => create.mutate(values))} noValidate>
        {notice && <Alert tone={notice.toLowerCase().includes('could') ? 'error' : 'success'}>{notice}</Alert>}

        <Field label="Subject" htmlFor="announcement-subject" error={errors.subject?.message} required>
          <Input id="announcement-subject" invalid={!!errors.subject} {...register('subject')} />
        </Field>

        <Field label="Audience" htmlFor="announcement-scope">
          <Select id="announcement-scope" {...register('scope')}>
            <option value="ALL_USERS">Everyone</option>
            <option value="EXCHANGE_COHORT">A single exchange</option>
            <option value="TEAM">A single team</option>
          </Select>
        </Field>

        <Field label="Body" htmlFor="announcement-body" error={errors.body?.message} required>
          <Textarea id="announcement-body" rows={5} invalid={!!errors.body} {...register('body')} />
        </Field>

        <div className="flex flex-wrap items-center gap-2">
          <Button type="submit" size="sm" loading={isSubmitting || create.isPending}>
            Publish
          </Button>
          <Button type="button" variant="secondary" size="sm" loading={suggest.isPending} onClick={() => suggest.mutate()}>
            Draft with AI
          </Button>
        </div>
      </form>
    </Card>
  );
}

function MessagesContent() {
  const [tab, setTab] = useState<Tab>('inbox');
  const { isStaff } = useAuth();
  const queryClient = useQueryClient();

  const inbox = useQuery({
    queryKey: ['messages', 'inbox'],
    queryFn: () => api<PageResponse<Message>>('/api/v1/messages?page=0&size=25'),
    enabled: tab === 'inbox',
  });

  const announcements = useQuery({
    queryKey: ['messages', 'announcements'],
    queryFn: () => api<PageResponse<Message>>('/api/v1/messages/announcements?page=0&size=25'),
    enabled: tab === 'announcements',
  });

  const pending = useQuery({
    queryKey: ['messages', 'pending'],
    queryFn: () => api<Message[]>('/api/v1/messages/pending-approval'),
    enabled: isStaff && tab === 'announcements',
  });

  const markRead = useMutation({
    mutationFn: (id: number) => api<Message>(`/api/v1/messages/${id}/read`, { method: 'POST' }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['messages'] });
      void queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
  });

  const approve = useMutation({
    mutationFn: (id: number) => api<Message>(`/api/v1/messages/announcements/${id}/approve`, { method: 'POST' }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['messages'] });
      void queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
  });

  const list = tab === 'inbox' ? inbox : announcements;

  return (
    <>
      <PageHeader
        title="Messages & announcements"
        description="Direct messages, team messages and cohort announcements. AI-drafted text is always labelled and never published without a human approval."
        actions={
          <div className="flex gap-2" role="tablist" aria-label="Message sections">
            <Button
              role="tab"
              aria-selected={tab === 'inbox'}
              size="sm"
              variant={tab === 'inbox' ? 'primary' : 'secondary'}
              onClick={() => setTab('inbox')}
            >
              Inbox
            </Button>
            <Button
              role="tab"
              aria-selected={tab === 'announcements'}
              size="sm"
              variant={tab === 'announcements' ? 'primary' : 'secondary'}
              onClick={() => setTab('announcements')}
            >
              Announcements
            </Button>
          </div>
        }
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="space-y-4 lg:col-span-2">
          {isStaff && tab === 'announcements' && (pending.data ?? []).length > 0 && (
            <Card className="border-amber-200 bg-amber-50/60">
              <CardTitle className="text-amber-900">Awaiting human approval</CardTitle>
              <CardDescription className="text-amber-900">
                AI-drafted announcements must be approved by a person before anyone sees them.
              </CardDescription>
              <ul className="mt-3 space-y-2">
                {(pending.data ?? []).map((message) => (
                  <li key={message.id} className="flex items-start justify-between gap-3 rounded-lg bg-white px-3 py-2">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{message.subject}</p>
                      <p className="text-xs text-slate-600">{message.body}</p>
                    </div>
                    <Button
                      size="sm"
                      loading={approve.isPending}
                      onClick={() => approve.mutate(message.id)}
                    >
                      Approve
                    </Button>
                  </li>
                ))}
              </ul>
            </Card>
          )}

          {list.isLoading ? (
            <SpinnerRow label="Loading messages…" />
          ) : list.isError ? (
            <ErrorState
              message={list.error instanceof ApiError ? list.error.message : 'Could not load messages.'}
              onRetry={() => list.refetch()}
            />
          ) : (list.data?.content ?? []).length === 0 ? (
            <EmptyState
              title={tab === 'inbox' ? 'Your inbox is empty' : 'No announcements yet'}
              description={
                tab === 'inbox'
                  ? 'Messages from teammates, mentors and facilitators arrive here.'
                  : 'Announcements from facilitators and mentors appear here.'
              }
            />
          ) : (
            <ul className="space-y-3">
              {(list.data?.content ?? []).map((message) => (
                <MessageRow key={message.id} message={message} onRead={(id) => markRead.mutate(id)} />
              ))}
            </ul>
          )}
        </section>

        <aside className="space-y-4">
          {tab === 'inbox' && <ComposeForm />}
          {tab === 'announcements' && isStaff && <AnnouncementForm />}
          <Card>
            <CardTitle>Responsible AI note</CardTitle>
            <CardDescription>
              Messages flagged as AI-generated show an AI draft badge. Nothing generated by AI is ever
              delivered without a human review step.
            </CardDescription>
          </Card>
        </aside>
      </div>
    </>
  );
}

export default function MessagesPage() {
  return (
    <RequireAuth>
      <MessagesContent />
    </RequireAuth>
  );
}

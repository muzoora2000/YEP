'use client';

import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { formatDateTime, titleCase } from '@/lib/format';
import type { Notification, UnreadCount } from '@/lib/types';
import { RequireAuth } from '@/components/RequireAuth';
import { Badge } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';

function NotificationsContent() {
  const queryClient = useQueryClient();

  const notifications = useQuery({
    queryKey: ['notifications'],
    queryFn: () => api<Notification[]>('/api/v1/notifications'),
  });

  const unread = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => api<UnreadCount>('/api/v1/notifications/unread-count'),
  });

  const markRead = useMutation({
    mutationFn: (id: number) =>
      api<Notification>(`/api/v1/notifications/${id}/read`, { method: 'PUT' }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] });
      void queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });
    },
  });

  const markAll = useMutation({
    mutationFn: () => api<void>('/api/v1/notifications/read-all', { method: 'PUT' }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] });
      void queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });
    },
  });

  return (
    <>
      <PageHeader
        title="Notifications"
        description="Activity updates, grading results, certificate news and project reviews."
        actions={
          <div className="flex items-center gap-2">
            <Badge tone={unread.data?.unread ? 'SUBMITTED' : 'COMPLETED'}>
              {unread.data?.unread ?? 0} unread
            </Badge>
            <Button
              size="sm"
              variant="secondary"
              loading={markAll.isPending}
              disabled={!unread.data?.unread}
              onClick={() => markAll.mutate()}
            >
              Mark all read
            </Button>
            <LinkButton href="/dashboard" size="sm" variant="ghost">
              Dashboard
            </LinkButton>
          </div>
        }
      />

      {notifications.isLoading ? (
        <SpinnerRow label="Loading notifications…" />
      ) : notifications.isError ? (
        <ErrorState
          message={notifications.error instanceof ApiError ? notifications.error.message : 'Could not load notifications.'}
          onRetry={() => notifications.refetch()}
        />
      ) : (notifications.data ?? []).length === 0 ? (
        <EmptyState
          title="You are all caught up"
          description="Notifications appear here when your enrollment, assessment or project status changes."
        />
      ) : (
        <div className="space-y-3">
          {(notifications.data ?? []).map((notification) => (
            <Card key={notification.notificationId} className={notification.read ? '!p-4' : '!p-4 border-brand-300 dark:border-brand-500/40'}>
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge tone={notification.type === 'EMAIL' ? 'PUBLISHED' : notification.type === 'SMS' ? 'PENDING' : 'DRAFT'}>
                      {titleCase(notification.type)}
                    </Badge>
                    {!notification.read && <span className="h-2 w-2 rounded-full bg-brand-600" aria-label="Unread" />}
                    <span className="text-xs text-slate-500 dark:text-slate-400">{formatDateTime(notification.dateSent)}</span>
                  </div>
                  <p className="mt-2 text-sm text-slate-800 dark:text-slate-200">{notification.message}</p>
                  {notification.link && (
                    <Link
                      href={notification.link}
                      className="mt-1 inline-block text-sm font-semibold text-brand-700 hover:underline dark:text-brand-300"
                    >
                      Open
                    </Link>
                  )}
                </div>
                {!notification.read && (
                  <Button
                    size="sm"
                    variant="secondary"
                    loading={markRead.isPending}
                    onClick={() => markRead.mutate(notification.notificationId)}
                  >
                    Mark read
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      <Card className="mt-6">
        <CardTitle>Communication</CardTitle>
        <CardDescription>
          Activity discussions and project feedback live inside each activity or project. This inbox only carries
          platform updates.
        </CardDescription>
      </Card>
    </>
  );
}

export default function NotificationsPage() {
  return (
    <RequireAuth>
      <NotificationsContent />
    </RequireAuth>
  );
}

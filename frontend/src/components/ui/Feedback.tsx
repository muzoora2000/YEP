'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';

import { Spinner } from './Button';

export function SpinnerRow({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-10 text-slate-500 dark:text-slate-400" role="status">
      <Spinner className="h-5 w-5" />
      <span className="text-sm">{label}</span>
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-dashed border-slate-300 bg-white/60 p-8 text-center dark:border-slate-700 dark:bg-slate-800/40">
      <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{title}</h3>
      {description && <p className="mx-auto mt-1 max-w-prose text-sm text-slate-600 dark:text-slate-400">{description}</p>}
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div
      role="alert"
      className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300"
    >
      <p>{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 font-semibold underline underline-offset-2 hover:text-rose-900 dark:hover:text-rose-200"
        >
          Try again
        </button>
      )}
    </div>
  );
}

export function Alert({ tone = 'error', children }: { tone?: 'error' | 'success' | 'info' | 'warning'; children: ReactNode }) {
  const styles = {
    error: 'border-rose-200 bg-rose-50 text-rose-800 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300',
    success: 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300',
    info: 'border-brand-200 bg-brand-50 text-brand-800 dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-200',
    warning: 'border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200',
  } as const;
  return (
    <div role="status" className={`rounded-lg border px-3 py-2 text-sm ${styles[tone]}`}>
      {children}
    </div>
  );
}

export function PageHeader({
  title,
  description,
  actions,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">{title}</h1>
        {description && <p className="mt-1 max-w-prose text-sm text-slate-600 dark:text-slate-400">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function Breadcrumbs({ items }: { items: { label: string; href?: string }[] }) {
  return (
    <nav aria-label="Breadcrumb" className="mb-4 text-sm text-slate-500 dark:text-slate-400">
      <ol className="flex flex-wrap items-center gap-1.5">
        {items.map((item, index) => (
          <li key={item.label} className="flex items-center gap-1.5">
            {index > 0 && <span aria-hidden="true">/</span>}
            {item.href ? (
              <Link href={item.href} className="hover:text-brand-700 hover:underline">
                {item.label}
              </Link>
            ) : (
              <span aria-current="page" className="text-slate-700 dark:text-slate-200">
                {item.label}
              </span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  );
}

/**
 * OfflineBanner: shown when the browser has no network connection.
 * Uses the online/offline browser events (accessibility-friendly live region).
 */
export function OfflineBanner() {
  const [offline, setOffline] = useState(false);

  useEffect(() => {
    setOffline(!navigator.onLine);
    const on = () => setOffline(false);
    const off = () => setOffline(true);
    window.addEventListener('online', on);
    window.addEventListener('offline', off);
    return () => { window.removeEventListener('online', on); window.removeEventListener('offline', off); };
  }, []);

  if (!offline) return null;
  return (
    <div
      role="status"
      aria-live="polite"
      className="fixed bottom-0 inset-x-0 z-50 flex items-center justify-center gap-2 bg-amber-600 px-4 py-2 text-sm font-semibold text-white"
    >
      <span aria-hidden>📡</span>
      <span>You are offline. Some features may be unavailable.</span>
    </div>
  );
}

/**
 * ServerWakingBanner: shown when the Render backend is cold-starting.
 * Tells users what is happening so they don't think the site is broken.
 */
export function ServerWakingBanner({ visible }: { visible: boolean }) {
  if (!visible) return null;
  return (
    <div
      role="status"
      aria-live="polite"
      className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200"
    >
      <strong>Waking up the server…</strong> YEP runs on a free-tier server that sleeps when
      inactive. This can take up to a minute. Please wait — it will retry automatically.
    </div>
  );
}

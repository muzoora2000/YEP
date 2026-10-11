'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { UnreadCount } from '@/lib/types';
import { initials } from '@/lib/format';
import { ThemeToggle } from '@/components/ThemeToggle';

const publicLinks = [
  { href: '/', label: 'Home' },
  { href: '/exchanges', label: 'Exchanges' },
  { href: '/certificates/verify', label: 'Verify a certificate' },
];

export function Navbar() {
  const { user, loading, logout, isStaff } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);

  const { data: unread } = useQuery({
    queryKey: ['unread-count'],
    queryFn: () => api<UnreadCount>('/api/v1/messages/unread-count'),
    enabled: !!user,
    refetchInterval: 60_000,
  });

  const links = user
    ? [
        { href: '/dashboard', label: 'Dashboard' },
        { href: '/exchanges', label: 'Exchanges' },
        { href: '/teams', label: 'My teams' },
        { href: '/assessments', label: 'Assessments' },
        { href: '/messages', label: 'Messages' },
        ...(isStaff ? [{ href: '/admin', label: 'Admin' }] : []),
      ]
    : publicLinks;

  const isActive = (href: string) => (href === '/' ? pathname === '/' : pathname.startsWith(href));

  async function handleLogout() {
    await logout();
    setOpen(false);
    router.push('/login');
  }

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur dark:border-slate-800 dark:bg-slate-900/95">
      <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3">
        <Link href="/" className="flex items-center gap-2 font-bold text-slate-900 dark:text-slate-100">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand-600 text-sm font-black text-white">
            Y
          </span>
          <span className="hidden sm:inline">YEP</span>
        </Link>

        <nav className="hidden flex-1 items-center gap-1 md:flex" aria-label="Main">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`rounded-lg px-3 py-2 text-sm font-medium ${
                isActive(link.href)
                  ? 'bg-brand-50 text-brand-800 dark:bg-brand-500/15 dark:text-brand-200'
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-slate-100'
              }`}
              aria-current={isActive(link.href) ? 'page' : undefined}
            >
              {link.label}
              {link.href === '/messages' && unread && unread.unread > 0 && (
                <span className="ml-1.5 rounded-full bg-rose-600 px-1.5 py-0.5 text-[10px] font-bold text-white">
                  {unread.unread}
                </span>
              )}
            </Link>
          ))}
        </nav>

        <div className="ml-auto hidden items-center gap-2 md:flex">
          {loading ? (
            <span className="text-sm text-slate-400 dark:text-slate-500">…</span>
          ) : user ? (
            <>
              <Link href="/dashboard" className="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-slate-100 dark:hover:bg-slate-800">
                <span className="grid h-8 w-8 place-items-center rounded-full bg-brand-100 text-xs font-bold text-brand-800 dark:bg-brand-500/20 dark:text-brand-200">
                  {initials(user.fullName)}
                </span>
                <span className="hidden text-sm font-medium text-slate-700 sm:inline dark:text-slate-200">{user.fullName}</span>
              </Link>
              <ThemeToggle />
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
              >
                Sign out
              </button>
            </>
          ) : (
            <>
              <ThemeToggle />
              <Link href="/login" className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800">
                Sign in
              </Link>
              <Link
                href="/register"
                className="rounded-lg bg-brand-600 px-3 py-2 text-sm font-semibold text-white hover:bg-brand-700"
              >
                Join YEP
              </Link>
            </>
          )}
        </div>

        <button
          type="button"
          className="ml-auto rounded-lg p-2 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800 md:hidden"
          aria-expanded={open}
          aria-controls="mobile-menu"
          aria-label={open ? 'Close menu' : 'Open menu'}
          onClick={() => setOpen((value) => !value)}
        >
          <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            {open ? <path d="M6 6l12 12M18 6L6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
          </svg>
        </button>
      </div>

      {open && (
        <div id="mobile-menu" className="border-t border-slate-200 bg-white px-4 py-3 md:hidden dark:border-slate-800 dark:bg-slate-900">
          <nav className="flex flex-col gap-1" aria-label="Mobile">
            {links.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setOpen(false)}
                className={`rounded-lg px-3 py-2 text-sm font-medium ${
                  isActive(link.href) ? 'bg-brand-50 text-brand-800 dark:bg-brand-500/15 dark:text-brand-200' : 'text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800'
                }`}
              >
                {link.label}
              </Link>
            ))}
          </nav>
          <div className="mt-3 flex flex-col gap-2 border-t border-slate-200 pt-3 dark:border-slate-700">
            {user ? (
              <>
                <span className="px-1 text-sm text-slate-600 dark:text-slate-300">Signed in as {user.fullName}</span>
                <ThemeToggle className="self-start" />
                <button
                  type="button"
                  onClick={handleLogout}
                  className="rounded-lg px-3 py-2 text-left text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
                >
                  Sign out
                </button>
              </>
            ) : (
              <>
                <ThemeToggle className="self-start" />
                <Link href="/login" onClick={() => setOpen(false)} className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800">
                  Sign in
                </Link>
                <Link
                  href="/register"
                  onClick={() => setOpen(false)}
                  className="rounded-lg bg-brand-600 px-3 py-2 text-center text-sm font-semibold text-white"
                >
                  Join YEP
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}

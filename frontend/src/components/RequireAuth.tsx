'use client';

import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

import type { Role } from '@/lib/types';
import { useAuth } from '@/lib/auth';
import { SpinnerRow } from './ui/Feedback';

interface RequireAuthProps {
  children: React.ReactNode;
  /** When present, only these roles are allowed in. */
  roles?: Role[];
}

/**
 * Client side route guard. The API enforces authorisation on every request -
 * this only keeps the UI honest and sends anonymous visitors to /login.
 */
export function RequireAuth({ children, roles }: RequireAuthProps) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace('/login');
      return;
    }
    if (roles && !roles.includes(user.role)) {
      router.replace('/dashboard');
    }
  }, [loading, user, roles, router]);

  if (loading || !user || (roles && !roles.includes(user.role))) {
    return <SpinnerRow label="Checking your session…" />;
  }

  return <>{children}</>;
}

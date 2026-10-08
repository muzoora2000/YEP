'use client';

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import { ApiError, api, setAccessToken } from './api';
import type { AuthResponse, Role, User } from './types';

interface RegisterInput {
  fullName: string;
  email: string;
  password: string;
  country?: string;
  phone?: string;
  role: Role;
  consentGiven: boolean;
  age?: number;
  skillLevel?: string;
  expertiseArea?: string;
  expertiseTopics?: string;
  mentorshipAreas?: string;
}

interface AuthContextValue {
  user: User | null;
  /** True while the session is being restored from the httpOnly cookie. */
  loading: boolean;
  login: (email: string, password: string) => Promise<User>;
  register: (input: RegisterInput) => Promise<User>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
  isStaff: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      const me = await api<User>('/api/v1/auth/me');
      setUser(me);
    } catch (error) {
      if (error instanceof ApiError && error.status !== 0 && error.status !== 401 && error.status !== 403) {
        // Unexpected backend failure: keep the previous user instead of logging out.
        return;
      }
      setAccessToken(null);
      setUser(null);
    }
  }, []);

  useEffect(() => {
    let active = true;
    void (async () => {
      await refresh();
      if (active) setLoading(false);
    })();
    return () => {
      active = false;
    };
  }, [refresh]);

  const login = useCallback(async (email: string, password: string) => {
    const response = await api<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: { email, password },
    });
    setAccessToken(response.token);
    setUser(response.user);
    return response.user;
  }, []);

  const register = useCallback(async (input: RegisterInput) => {
    const created = await api<User>('/api/v1/auth/register', { method: 'POST', body: input });
    try {
      const response = await api<AuthResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: { email: input.email, password: input.password },
      });
      setAccessToken(response.token);
      setUser(response.user);
    } catch {
      setUser(created);
    }
    return created;
  }, []);

  const logout = useCallback(async () => {
    try {
      await api<void>('/api/v1/auth/logout', { method: 'POST' });
    } finally {
      setAccessToken(null);
      setUser(null);
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      login,
      register,
      logout,
      refresh,
      isStaff: user?.role === 'FACILITATOR' || user?.role === 'MENTOR' || user?.role === 'ADMIN',
    }),
    [user, loading, login, register, logout, refresh],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return context;
}

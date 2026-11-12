'use client';

import { useSearchParams, useRouter } from 'next/navigation';
import { Suspense, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Link from 'next/link';

import { ApiError, api } from '@/lib/api';
import { Alert, SpinnerRow } from '@/components/ui/Feedback';
import { Button } from '@/components/ui/Button';
import { Field, PasswordInput } from '@/components/ui/Form';

const schema = z.object({
  password: z.string().min(8, 'At least 8 characters').regex(/[a-zA-Z]/, 'Must contain a letter').regex(/[0-9]/, 'Must contain a digit'),
  confirm: z.string(),
}).refine((data) => data.password === data.confirm, { message: 'Passwords do not match', path: ['confirm'] });
type Values = z.infer<typeof schema>;

function ResetForm() {
  const params = useSearchParams();
  const token = params.get('token') ?? '';
  const router = useRouter();
  const [done, setDone] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(schema),
  });

  async function onSubmit(values: Values) {
    setServerError(null);
    try {
      await api('/api/v1/auth/reset-password', { method: 'POST', body: { token, newPassword: values.password } });
      setDone(true);
      setTimeout(() => router.push('/login'), 2500);
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : 'Password reset failed. The link may have expired.');
    }
  }

  if (!token) {
    return (
      <div className="mx-auto max-w-md text-center">
        <Alert>No reset token found. Please request a new password reset link.</Alert>
        <Link href="/forgot-password" className="mt-4 inline-block text-sm font-semibold text-brand-700 hover:underline dark:text-brand-300">
          Request a new link
        </Link>
      </div>
    );
  }

  if (done) {
    return (
      <div className="mx-auto max-w-md text-center">
        <p className="text-lg font-semibold text-green-700 dark:text-green-400">Password changed successfully!</p>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">Redirecting you to sign in…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md">
      <div className="mb-6 text-center">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Choose a new password</h1>
      </div>
      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900"
      >
        {serverError && <Alert>{serverError}</Alert>}
        <Field label="New password" htmlFor="password" error={errors.password?.message} required>
          <PasswordInput id="password" autoComplete="new-password" invalid={!!errors.password} {...register('password')} />
        </Field>
        <Field label="Confirm password" htmlFor="confirm" error={errors.confirm?.message} required>
          <PasswordInput id="confirm" autoComplete="new-password" invalid={!!errors.confirm} {...register('confirm')} />
        </Field>
        <Button type="submit" loading={isSubmitting} className="w-full">Set new password</Button>
      </form>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<SpinnerRow label="Loading…" />}>
      <ResetForm />
    </Suspense>
  );
}

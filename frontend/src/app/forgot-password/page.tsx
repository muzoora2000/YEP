'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { ApiError, api } from '@/lib/api';
import { Alert } from '@/components/ui/Feedback';
import { Button } from '@/components/ui/Button';
import { Field, Input } from '@/components/ui/Form';

const schema = z.object({ email: z.string().email('Enter a valid email address') });
type Values = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const [sent, setSent] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(schema),
  });

  async function onSubmit(values: Values) {
    setServerError(null);
    try {
      await api('/api/v1/auth/forgot-password', { method: 'POST', body: { email: values.email } });
      setSent(true);
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : 'Could not send reset email. Please try again.');
    }
  }

  if (sent) {
    return (
      <div className="mx-auto max-w-md text-center">
        <div className="mb-6">
          <span className="grid h-16 w-16 place-items-center rounded-full bg-green-100 mx-auto text-3xl" aria-hidden>✉</span>
        </div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Check your inbox</h1>
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
          If that email is registered, we have sent a password reset link. It expires in one hour.
        </p>
        <Link href="/login" className="mt-6 inline-block text-sm font-semibold text-brand-700 hover:underline dark:text-brand-300">
          Back to sign in
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md">
      <div className="mb-6 text-center">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Reset your password</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
          Enter your email address and we will send you a link to reset your password.
        </p>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900"
      >
        {serverError && <Alert>{serverError}</Alert>}

        <Field label="Email" htmlFor="email" error={errors.email?.message} required>
          <Input id="email" type="email" autoComplete="email" placeholder="you@example.com" invalid={!!errors.email} {...register('email')} />
        </Field>

        <Button type="submit" loading={isSubmitting} className="w-full">
          Send reset link
        </Button>

        <p className="text-center text-sm text-slate-600 dark:text-slate-300">
          <Link href="/login" className="font-semibold text-brand-700 dark:text-brand-300 hover:underline">
            Back to sign in
          </Link>
        </p>
      </form>
    </div>
  );
}

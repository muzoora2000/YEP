'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { ApiError } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { registerSchema, type RegisterValues } from '@/lib/schemas';
import { Alert } from '@/components/ui/Feedback';
import { Button } from '@/components/ui/Button';
import { Checkbox, Field, Input, Select, Textarea } from '@/components/ui/Form';

const roleOptions = [
  { value: 'PARTICIPANT', label: 'Participant - join exchanges and teams' },
  { value: 'FACILITATOR', label: 'Facilitator - run sessions and assess' },
  { value: 'MENTOR', label: 'Mentor - guide teams' },
] as const;

export default function RegisterPage() {
  const { register: signUp } = useAuth();
  const router = useRouter();
  const [serverError, setServerError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      role: 'PARTICIPANT',
      skillLevel: 'BEGINNER',
      country: '',
      phone: '',
    },
  });

  const role = watch('role');

  async function onSubmit(values: RegisterValues) {
    setServerError(null);
    setFieldErrors({});
    try {
      await signUp({
        fullName: values.fullName,
        email: values.email,
        password: values.password,
        country: values.country || undefined,
        phone: values.phone || undefined,
        role: values.role,
        consentGiven: values.consentGiven,
        age: values.role === 'PARTICIPANT' ? values.age : undefined,
        skillLevel: values.role === 'PARTICIPANT' ? values.skillLevel : undefined,
        expertiseArea: values.role === 'FACILITATOR' ? values.expertiseArea : undefined,
        expertiseTopics: values.role === 'FACILITATOR' ? values.expertiseTopics : undefined,
        mentorshipAreas: values.role === 'MENTOR' ? values.mentorshipAreas : undefined,
      });
      router.push('/dashboard');
    } catch (error) {
      if (error instanceof ApiError) {
        setServerError(error.message);
        setFieldErrors(error.fieldErrors);
      } else {
        setServerError('Registration failed. Please try again.');
      }
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6 text-center">
        <h1 className="text-2xl font-bold text-slate-900">Create your YEP account</h1>
        <p className="mt-1 text-sm text-slate-600">
          Only the details needed to run the programme are collected, and only with your consent.
        </p>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="space-y-5 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
      >
        {serverError && <Alert>{serverError}</Alert>}

        <Field label="I am joining as" htmlFor="role" required>
          <Select id="role" {...register('role')}>
            {roleOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Full name" htmlFor="fullName" error={errors.fullName?.message} required>
            <Input id="fullName" autoComplete="name" invalid={!!errors.fullName} {...register('fullName')} />
          </Field>

          <Field
            label="Email"
            htmlFor="email"
            error={errors.email?.message ?? fieldErrors.email}
            required
          >
            <Input id="email" type="email" autoComplete="email" invalid={!!errors.email} {...register('email')} />
          </Field>

          <Field label="Password" htmlFor="password" error={errors.password?.message} required>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              invalid={!!errors.password}
              {...register('password')}
            />
          </Field>

          <Field label="Confirm password" htmlFor="confirmPassword" error={errors.confirmPassword?.message} required>
            <Input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              invalid={!!errors.confirmPassword}
              {...register('confirmPassword')}
            />
          </Field>

          <Field label="Country" htmlFor="country">
            <Input id="country" autoComplete="country-name" placeholder="Uganda" {...register('country')} />
          </Field>

          <Field label="Phone (optional)" htmlFor="phone">
            <Input id="phone" autoComplete="tel" placeholder="+256…" {...register('phone')} />
          </Field>
        </div>

        {role === 'PARTICIPANT' && (
          <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2">
            <Field label="Age" htmlFor="age" error={errors.age?.message} required hint="Participants must be 16-40">
              <Input
                id="age"
                type="number"
                min={16}
                max={40}
                inputMode="numeric"
                invalid={!!errors.age}
                {...register('age')}
              />
            </Field>
            <Field label="Experience level" htmlFor="skillLevel">
              <Select id="skillLevel" {...register('skillLevel')}>
                <option value="BEGINNER">Beginner</option>
                <option value="INTERMEDIATE">Intermediate</option>
                <option value="ADVANCED">Advanced</option>
              </Select>
            </Field>
          </div>
        )}

        {role === 'FACILITATOR' && (
          <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2">
            <Field label="Area of expertise" htmlFor="expertiseArea" error={errors.expertiseArea?.message}>
              <Input id="expertiseArea" placeholder="Digital literacy" invalid={!!errors.expertiseArea} {...register('expertiseArea')} />
            </Field>
            <Field label="Topics" htmlFor="expertiseTopics" error={errors.expertiseTopics?.message}>
              <Textarea
                id="expertiseTopics"
                placeholder="AI, media literacy, intercultural communication"
                className="min-h-[80px]"
                {...register('expertiseTopics')}
              />
            </Field>
          </div>
        )}

        {role === 'MENTOR' && (
          <div className="rounded-xl bg-slate-50 p-4">
            <Field
              label="Mentorship areas"
              htmlFor="mentorshipAreas"
              error={errors.mentorshipAreas?.message}
              hint="Comma separated, e.g. HEALTH, CLIMATE"
            >
              <Textarea
                id="mentorshipAreas"
                className="min-h-[80px]"
                invalid={!!errors.mentorshipAreas}
                {...register('mentorshipAreas')}
              />
            </Field>
          </div>
        )}

        <Checkbox
          label="I consent to YEP storing the details above for running virtual exchanges, assessments and certificates. Data is never sold or shared with advertisers."
          error={errors.consentGiven?.message ?? fieldErrors.consentGiven}
          {...register('consentGiven')}
        />

        <Button type="submit" loading={isSubmitting} className="w-full">
          Create account
        </Button>

        <p className="text-center text-sm text-slate-600">
          Already registered?{' '}
          <Link href="/login" className="font-semibold text-brand-700 hover:underline">
            Sign in
          </Link>
        </p>
      </form>
    </div>
  );
}

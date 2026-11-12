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
import { Field, Input, PasswordInput, Select, Textarea } from '@/components/ui/Form';

const roleOptions = [
  { value: 'PARTICIPANT', label: 'Participant - join exchanges and teams' },
  { value: 'FACILITATOR', label: 'Facilitator - run sessions and assess' },
  { value: 'MENTOR', label: 'Mentor - guide teams' },
  { value: 'PARTNER', label: 'Partner organisation - opportunities and support' },
] as const;

const organisationTypes = [
  { value: 'NGO', label: 'NGO' },
  { value: 'UNIVERSITY', label: 'University' },
  { value: 'EMPLOYER', label: 'Employer' },
  { value: 'DEVELOPMENT_AGENCY', label: 'Development agency' },
  { value: 'TECH_COMPANY', label: 'Tech company' },
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
      country: '',
      phone: '',
      shareProfileWithPartners: true,
    },
  });

  const role = watch('role');

  async function onSubmit(values: RegisterValues) {
    setServerError(null);
    setFieldErrors({});
    try {
      await signUp({
        name: values.name,
        email: values.email,
        password: values.password,
        role: values.role,
        country: values.country || undefined,
        phone: values.phone || undefined,
        age: values.role === 'PARTICIPANT' ? values.age : undefined,
        digitalSkills: values.role === 'PARTICIPANT' ? values.digitalSkills : undefined,
        institution: values.role === 'PARTICIPANT' ? values.institution : undefined,
        bio: values.bio || undefined,
        shareProfileWithPartners: values.shareProfileWithPartners,
        specialisation: values.role === 'FACILITATOR' ? values.specialisation : undefined,
        organisation: values.role === 'FACILITATOR' ? values.organisation : undefined,
        expertise: values.role === 'MENTOR' ? values.expertise : undefined,
        department: values.role === 'MENTOR' ? values.department : undefined,
        organisationName: values.role === 'PARTNER' ? values.organisationName : undefined,
        organisationType: values.role === 'PARTNER' ? values.organisationType : undefined,
        website: values.role === 'PARTNER' ? values.website : undefined,
        description: values.role === 'PARTNER' ? values.description : undefined,
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
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Create your YEP account</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
          Only the details needed to run the programme are collected. Participants are active immediately;
          staff and partner accounts are reviewed by an administrator.
        </p>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="space-y-5 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900"
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
          <Field label="Full name" htmlFor="name" error={errors.name?.message ?? fieldErrors.name} required>
            <Input id="name" autoComplete="name" invalid={!!errors.name} {...register('name')} />
          </Field>

          <Field label="Email" htmlFor="email" error={errors.email?.message ?? fieldErrors.email} required>
            <Input id="email" type="email" autoComplete="email" invalid={!!errors.email} {...register('email')} />
          </Field>

          <Field label="Password" htmlFor="password" error={errors.password?.message} required hint="At least 8 characters with a letter and a number">
            <PasswordInput id="password" autoComplete="new-password" invalid={!!errors.password} {...register('password')} />
          </Field>

          <Field label="Confirm password" htmlFor="confirmPassword" error={errors.confirmPassword?.message} required>
            <PasswordInput
              id="confirmPassword"
              autoComplete="new-password"
              invalid={!!errors.confirmPassword}
              {...register('confirmPassword')}
            />
          </Field>

          <Field label="Country" htmlFor="country" error={errors.country?.message ?? fieldErrors.country}>
            <Input id="country" autoComplete="country-name" placeholder="Uganda" {...register('country')} />
          </Field>

          <Field label="Phone (optional)" htmlFor="phone">
            <Input id="phone" autoComplete="tel" placeholder="+256…" {...register('phone')} />
          </Field>
        </div>

        {role === 'PARTICIPANT' && (
          <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2 dark:bg-slate-800/60">
            <Field label="Age" htmlFor="age" error={errors.age?.message ?? fieldErrors.age} required hint="Participants must be 18-35">
              <Input id="age" type="number" min={18} max={35} inputMode="numeric" invalid={!!errors.age} {...register('age')} />
            </Field>
            <Field label="Digital skills" htmlFor="digitalSkills">
              <Input id="digitalSkills" placeholder="Beginner, intermediate…" {...register('digitalSkills')} />
            </Field>
            <Field label="Institution (optional)" htmlFor="institution" className="sm:col-span-2">
              <Input id="institution" placeholder="School, college or employer" {...register('institution')} />
            </Field>
          </div>
        )}

        {role === 'FACILITATOR' && (
          <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2 dark:bg-slate-800/60">
            <Field label="Specialisation" htmlFor="specialisation">
              <Input id="specialisation" placeholder="Digital literacy" {...register('specialisation')} />
            </Field>
            <Field label="Organisation" htmlFor="organisation">
              <Input id="organisation" placeholder="YEP partner" {...register('organisation')} />
            </Field>
          </div>
        )}

        {role === 'MENTOR' && (
          <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2 dark:bg-slate-800/60">
            <Field label="Expertise" htmlFor="expertise">
              <Textarea id="expertise" placeholder="Climate, health, education…" className="min-h-[80px]" {...register('expertise')} />
            </Field>
            <Field label="Department" htmlFor="department">
              <Input id="department" placeholder="Programmes" {...register('department')} />
            </Field>
          </div>
        )}

        {role === 'PARTNER' && (
          <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2 dark:bg-slate-800/60">
            <Field label="Organisation name" htmlFor="organisationName" error={errors.organisationName?.message ?? fieldErrors.organisationName} required>
              <Input id="organisationName" invalid={!!errors.organisationName} {...register('organisationName')} />
            </Field>
            <Field label="Organisation type" htmlFor="organisationType">
              <Select id="organisationType" {...register('organisationType')}>
                {organisationTypes.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Website" htmlFor="website" className="sm:col-span-2">
              <Input id="website" type="url" placeholder="https://…" {...register('website')} />
            </Field>
            <Field label="What your organisation does" htmlFor="description" className="sm:col-span-2">
              <Textarea id="description" {...register('description')} />
            </Field>
          </div>
        )}

        <Field label="Short bio (optional)" htmlFor="bio">
          <Textarea id="bio" placeholder="Tell us a little about yourself" {...register('bio')} />
        </Field>

        <label className="flex items-start gap-3 text-sm text-slate-700 dark:text-slate-300">
          <input
            type="checkbox"
            className="mt-0.5 h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-600 dark:border-slate-600 dark:bg-slate-800"
            {...register('shareProfileWithPartners')}
          />
          <span>
            I agree that my profile may be shared with verified partners for opportunities and support. I consent to
            YEP storing these details to run virtual exchanges, assessments and certificates. Data is never sold.
          </span>
        </label>

        <Button type="submit" loading={isSubmitting} className="w-full">
          Create account
        </Button>

        <p className="text-center text-sm text-slate-600 dark:text-slate-300">
          Already registered?{' '}
          <Link href="/login" className="font-semibold text-brand-700 hover:underline dark:text-brand-300">
            Sign in
          </Link>
        </p>
      </form>
    </div>
  );
}

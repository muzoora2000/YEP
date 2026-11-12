'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import Link from 'next/link';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatDate } from '@/lib/format';
import type { User } from '@/lib/types';
import { RequireAuth } from '@/components/RequireAuth';
import { Button, LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { Alert, EmptyState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Input, Textarea, Select } from '@/components/ui/Form';

const COUNTRIES = [
  'Uganda', 'Kenya', 'Tanzania', 'Rwanda', 'Burundi',
  'South Sudan', 'DR Congo', 'Somalia', 'Ethiopia', 'Djibouti', 'Other',
];

function ProfileContent() {
  const { user, refresh } = useAuth();
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);
  const [editMode, setEditMode] = useState(false);
  const [form, setForm] = useState({
    name: user?.name ?? '',
    phone: user?.phone ?? '',
    bio: user?.bio ?? '',
    institution: user?.institution ?? '',
    digitalSkills: user?.digitalSkills ?? '',
    country: user?.country ?? '',
    specialisation: user?.specialisation ?? '',
    organisation: user?.organisation ?? '',
    expertise: user?.expertise ?? '',
    department: user?.department ?? '',
    website: user?.website ?? '',
    description: user?.description ?? '',
  });

  // Password change state
  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [pwError, setPwError] = useState<string | null>(null);

  const updateProfile = useMutation({
    mutationFn: () => api<User>('/api/v1/auth/me', { method: 'PUT', body: { ...form } }),
    onSuccess: () => {
      setNotice('Profile updated.');
      setEditMode(false);
      void refresh();
      void queryClient.invalidateQueries({ queryKey: ['me'] });
    },
    onError: (err) => setNotice(err instanceof ApiError ? err.message : 'Could not update profile.'),
  });

  const updateConsent = useMutation({
    mutationFn: (share: boolean) => api<User>('/api/v1/auth/me/consent', { method: 'PUT', body: { shareProfileWithPartners: share } }),
    onSuccess: () => {
      setNotice('Consent preference saved.');
      void refresh();
    },
    onError: (err) => setNotice(err instanceof ApiError ? err.message : 'Could not update consent.'),
  });

  const changePassword = useMutation({
    mutationFn: () => {
      if (pwForm.newPassword !== pwForm.confirm) throw new Error('Passwords do not match.');
      if (pwForm.newPassword.length < 8) throw new Error('Password must be at least 8 characters.');
      return api('/api/v1/auth/me/password', { method: 'PUT', body: { currentPassword: pwForm.currentPassword, newPassword: pwForm.newPassword } });
    },
    onSuccess: () => {
      setPwError(null);
      setNotice('Password changed successfully.');
      setPwForm({ currentPassword: '', newPassword: '', confirm: '' });
    },
    onError: (err) => setPwError(err instanceof ApiError ? err.message : err instanceof Error ? err.message : 'Could not change password.'),
  });

  if (!user) return <SpinnerRow label="Loading profile…" />;

  return (
    <>
      <PageHeader
        title="My profile"
        description="Manage your personal information and preferences."
        actions={
          <LinkButton href="/dashboard" size="sm" variant="secondary">Dashboard</LinkButton>
        }
      />

      {notice && (
        <div className="mb-4">
          <Alert tone={notice.toLowerCase().includes('could not') ? 'error' : 'success'}>{notice}</Alert>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Card>
            <div className="flex items-start justify-between gap-3">
              <CardTitle>Personal information</CardTitle>
              <Button size="sm" variant="secondary" onClick={() => setEditMode((v) => !v)}>
                {editMode ? 'Cancel' : 'Edit'}
              </Button>
            </div>

            {editMode ? (
              <form
                className="mt-4 space-y-3"
                onSubmit={(e) => { e.preventDefault(); updateProfile.mutate(); }}
              >
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Full name" htmlFor="name" required>
                    <Input id="name" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
                  </Field>
                  <Field label="Phone" htmlFor="phone">
                    <Input id="phone" type="tel" value={form.phone} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} />
                  </Field>
                </div>

                {user.role === 'PARTICIPANT' && (
                  <>
                    <div className="grid gap-3 sm:grid-cols-2">
                      <Field label="Country" htmlFor="country">
                        <Select id="country" value={form.country} onChange={(e) => setForm((f) => ({ ...f, country: e.target.value }))}>
                          <option value="">Choose…</option>
                          {COUNTRIES.map((c) => <option key={c} value={c}>{c}</option>)}
                        </Select>
                      </Field>
                      <Field label="Institution" htmlFor="institution">
                        <Input id="institution" value={form.institution} onChange={(e) => setForm((f) => ({ ...f, institution: e.target.value }))} />
                      </Field>
                    </div>
                    <Field label="Digital skills" htmlFor="digitalSkills">
                      <Input id="digitalSkills" placeholder="e.g. Beginner, Intermediate, Advanced" value={form.digitalSkills} onChange={(e) => setForm((f) => ({ ...f, digitalSkills: e.target.value }))} />
                    </Field>
                    <Field label="Bio" htmlFor="bio">
                      <Textarea id="bio" rows={3} value={form.bio} onChange={(e) => setForm((f) => ({ ...f, bio: e.target.value }))} />
                    </Field>
                  </>
                )}
                {user.role === 'FACILITATOR' && (
                  <Field label="Specialisation" htmlFor="specialisation">
                    <Input id="specialisation" value={form.specialisation} onChange={(e) => setForm((f) => ({ ...f, specialisation: e.target.value }))} />
                  </Field>
                )}
                {user.role === 'MENTOR' && (
                  <div className="grid gap-3 sm:grid-cols-2">
                    <Field label="Organisation" htmlFor="org">
                      <Input id="org" value={form.organisation} onChange={(e) => setForm((f) => ({ ...f, organisation: e.target.value }))} />
                    </Field>
                    <Field label="Expertise" htmlFor="expertise">
                      <Input id="expertise" value={form.expertise} onChange={(e) => setForm((f) => ({ ...f, expertise: e.target.value }))} />
                    </Field>
                  </div>
                )}
                {user.role === 'PARTNER' && (
                  <>
                    <div className="grid gap-3 sm:grid-cols-2">
                      <Field label="Website" htmlFor="website">
                        <Input id="website" type="url" value={form.website} onChange={(e) => setForm((f) => ({ ...f, website: e.target.value }))} />
                      </Field>
                    </div>
                    <Field label="Description" htmlFor="description">
                      <Textarea id="description" rows={3} value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
                    </Field>
                  </>
                )}

                <Button type="submit" loading={updateProfile.isPending}>Save changes</Button>
              </form>
            ) : (
              <dl className="mt-4 space-y-3 text-sm">
                {[
                  ['Name', user.name],
                  ['Email', user.email],
                  ['Role', user.role],
                  ['Phone', user.phone ?? '—'],
                  ['Country', user.country ?? '—'],
                  ['Institution', user.institution ?? '—'],
                  ['Digital skills', user.digitalSkills ?? '—'],
                  ['Bio', user.bio ?? '—'],
                  ['Joined', formatDate(user.createdAt)],
                ].map(([label, value]) => (
                  <div key={label} className="flex justify-between gap-3 border-b border-slate-100 pb-2 dark:border-slate-800">
                    <dt className="text-slate-500 dark:text-slate-400">{label}</dt>
                    <dd className="font-medium text-slate-900 dark:text-slate-100 text-right">{value}</dd>
                  </div>
                ))}
              </dl>
            )}
          </Card>

          <Card>
            <CardTitle>Change password</CardTitle>
            <form
              className="mt-4 space-y-3"
              onSubmit={(e) => { e.preventDefault(); changePassword.mutate(); }}
            >
              {pwError && <Alert>{pwError}</Alert>}
              <Field label="Current password" htmlFor="current-pw">
                <Input id="current-pw" type="password" value={pwForm.currentPassword} onChange={(e) => setPwForm((f) => ({ ...f, currentPassword: e.target.value }))} />
              </Field>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="New password" htmlFor="new-pw">
                  <Input id="new-pw" type="password" value={pwForm.newPassword} onChange={(e) => setPwForm((f) => ({ ...f, newPassword: e.target.value }))} />
                </Field>
                <Field label="Confirm new password" htmlFor="confirm-pw">
                  <Input id="confirm-pw" type="password" value={pwForm.confirm} onChange={(e) => setPwForm((f) => ({ ...f, confirm: e.target.value }))} />
                </Field>
              </div>
              <Button type="submit" size="sm" loading={changePassword.isPending}>Change password</Button>
            </form>
          </Card>
        </div>

        <aside className="space-y-4">
          {user.role === 'PARTICIPANT' && (
            <Card>
              <CardTitle>Partner sharing consent</CardTitle>
              <CardDescription>
                Allow verified partner organisations (universities, employers) to find your profile
                in talent searches. Your email and phone are never shared.
              </CardDescription>
              <div className="mt-4 flex items-center gap-3">
                <span className={`text-sm font-semibold ${user.shareProfileWithPartners ? 'text-green-700 dark:text-green-400' : 'text-slate-500 dark:text-slate-400'}`}>
                  {user.shareProfileWithPartners ? 'Sharing enabled' : 'Not sharing'}
                </span>
                <Button
                  size="sm"
                  variant={user.shareProfileWithPartners ? 'danger' : 'primary'}
                  loading={updateConsent.isPending}
                  onClick={() => updateConsent.mutate(!user.shareProfileWithPartners)}
                >
                  {user.shareProfileWithPartners ? 'Withdraw consent' : 'Give consent'}
                </Button>
              </div>
              {user.consentDate && (
                <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
                  Consent given on {formatDate(user.consentDate)}
                </p>
              )}
            </Card>
          )}

          <Card>
            <CardTitle>Level Two eligibility</CardTitle>
            <CardDescription>
              You become eligible for Level Two programmes when you receive a Level One certificate.
              Facilitators can also grant eligibility directly.
            </CardDescription>
            <p className={`mt-3 text-sm font-semibold ${user.levelTwoEligible ? 'text-green-700 dark:text-green-400' : 'text-slate-500 dark:text-slate-400'}`}>
              {user.levelTwoEligible ? 'You are eligible for Level Two' : 'Not yet eligible for Level Two'}
            </p>
          </Card>

          <Card>
            <CardTitle>Quick links</CardTitle>
            <nav className="mt-3 flex flex-col gap-2">
              <Link href="/exchanges" className="text-sm font-medium text-brand-700 hover:underline dark:text-brand-300">Browse activities</Link>
              <Link href="/assessments" className="text-sm font-medium text-brand-700 hover:underline dark:text-brand-300">My assessments</Link>
              <Link href="/teams" className="text-sm font-medium text-brand-700 hover:underline dark:text-brand-300">My teams</Link>
              <Link href="/opportunities" className="text-sm font-medium text-brand-700 hover:underline dark:text-brand-300">Opportunities</Link>
              <Link href="/privacy" className="text-sm font-medium text-brand-700 hover:underline dark:text-brand-300">Privacy notice</Link>
            </nav>
          </Card>
        </aside>
      </div>
    </>
  );
}

export default function ProfilePage() {
  return (
    <RequireAuth>
      <ProfileContent />
    </RequireAuth>
  );
}

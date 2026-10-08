'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';

import { Button } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/Feedback';
import { Field, Input } from '@/components/ui/Form';

export default function CertificateLookupPage() {
  const router = useRouter();
  const [code, setCode] = useState('');

  return (
    <div className="mx-auto max-w-md">
      <PageHeader
        title="Verify a certificate"
        description="Enter the verification code printed on the certificate. Anyone can check a credential - no account needed."
      />

      <form
        className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
        onSubmit={(event) => {
          event.preventDefault();
          const trimmed = code.trim();
          if (trimmed) router.push(`/certificates/${encodeURIComponent(trimmed)}`);
        }}
      >
        <Field label="Verification code" htmlFor="verification-code" required hint="Example: YEP-2026-4F7K-9QZ2">
          <Input
            id="verification-code"
            value={code}
            onChange={(event) => setCode(event.target.value)}
            placeholder="YEP-2026-XXXX-XXXX"
            autoComplete="off"
          />
        </Field>
        <Button type="submit" disabled={!code.trim()} className="w-full">
          Verify certificate
        </Button>
      </form>

      <Card className="mt-6">
        <CardTitle>Employers and universities</CardTitle>
        <CardDescription>
          Certificates include a public verification code, so you can confirm a candidate&apos;s credential
          without contacting us.
        </CardDescription>
      </Card>
    </div>
  );
}

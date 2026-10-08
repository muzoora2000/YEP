'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { formatDate } from '@/lib/format';
import type { CertificateVerification } from '@/lib/types';
import { LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';

export default function CertificateVerifyPage() {
  const params = useParams<{ code: string }>();
  const code = params.code;

  const verification = useQuery({
    queryKey: ['certificate', code],
    queryFn: () => api<CertificateVerification>(`/api/v1/certificates/verify/${encodeURIComponent(code)}`),
    retry: false,
  });

  if (verification.isLoading) return <SpinnerRow label="Verifying certificate…" />;
  if (verification.isError) {
    return (
      <ErrorState
        message={
          verification.error instanceof ApiError
            ? verification.error.message
            : 'That verification code could not be checked.'
        }
        onRetry={() => verification.refetch()}
      />
    );
  }

  const result = verification.data as CertificateVerification;

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="Certificate verification"
        description={`Code ${code}`}
        actions={<LinkButton href="/certificates/verify" size="sm" variant="secondary">Check another code</LinkButton>}
      />

      <Card className={result.valid ? 'border-emerald-300 bg-emerald-50/60' : 'border-rose-300 bg-rose-50/60'}>
        <CardTitle className={result.valid ? 'text-emerald-900' : 'text-rose-900'}>
          {result.valid ? 'Valid certificate' : 'Certificate not found'}
        </CardTitle>
        <CardDescription className={result.valid ? 'text-emerald-900' : 'text-rose-900'}>
          {result.message}
        </CardDescription>

        {result.valid && (
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between gap-3">
              <dt className="text-slate-500">Awarded to</dt>
              <dd className="font-semibold text-slate-900">{result.recipientName ?? 'Unknown recipient'}</dd>
            </div>
            <div className="flex justify-between gap-3">
              <dt className="text-slate-500">Title</dt>
              <dd className="text-right font-semibold text-slate-900">{result.title ?? 'Certificate'}</dd>
            </div>
            <div className="flex justify-between gap-3">
              <dt className="text-slate-500">Issued</dt>
              <dd className="font-medium text-slate-800">
                {result.issueDate ? formatDate(result.issueDate) : 'Unknown'}
              </dd>
            </div>
            <div className="flex justify-between gap-3">
              <dt className="text-slate-500">For</dt>
              <dd className="text-right font-medium text-slate-800">
                {result.relatedActivityOrProject ?? 'YEP programme'}
              </dd>
            </div>
            <div className="flex justify-between gap-3">
              <dt className="text-slate-500">Verification code</dt>
              <dd className="break-all font-mono text-xs text-slate-700">{code}</dd>
            </div>
          </dl>
        )}
      </Card>

      <p className="mt-6 text-sm text-slate-600">
        Only YEP can mint these codes. If you believe a certificate is misreported,{' '}
        <Link href="/" className="font-semibold text-brand-700 hover:underline">
          contact the programme team
        </Link>
        .
      </p>
    </div>
  );
}

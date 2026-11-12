'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { formatDate } from '@/lib/format';
import type { Certificate } from '@/lib/types';
import { LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';

export default function CertificateVerifyPage() {
  const params = useParams<{ code: string }>();
  const code = decodeURIComponent(params.code);

  const certificate = useQuery({
    queryKey: ['certificate', code],
    queryFn: () => api<Certificate>(`/api/v1/public/certificates/${encodeURIComponent(code)}`),
    retry: false,
  });

  if (certificate.isLoading) return <SpinnerRow label="Verifying certificate…" />;
  if (certificate.isError) {
    return (
      <div className="mx-auto max-w-2xl">
        <PageHeader
          title="Certificate verification"
          description={`Code ${code}`}
          actions={
            <LinkButton href="/certificates/verify" size="sm" variant="secondary">
              Check another code
            </LinkButton>
          }
        />
        <ErrorState
          message={
            certificate.error instanceof ApiError
              ? certificate.error.message
              : 'That certificate could not be found.'
          }
          onRetry={() => certificate.refetch()}
        />
      </div>
    );
  }

  const result = certificate.data as Certificate;
  const valid = !result.revoked;

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="Certificate verification"
        description={`Code ${code}`}
        actions={
          <LinkButton href="/certificates/verify" size="sm" variant="secondary">
            Check another code
          </LinkButton>
        }
      />

      <Card
        className={
          valid
            ? 'border-emerald-300 bg-emerald-50/60 dark:border-emerald-500/30 dark:bg-emerald-500/10'
            : 'border-rose-300 bg-rose-50/60 dark:border-rose-500/30 dark:bg-rose-500/10'
        }
      >
        <CardTitle className={valid ? 'text-emerald-900 dark:text-emerald-200' : 'text-rose-900 dark:text-rose-200'}>
          {valid ? 'Valid certificate' : 'Certificate revoked'}
        </CardTitle>
        <CardDescription className={valid ? 'text-emerald-900 dark:text-emerald-200' : 'text-rose-900 dark:text-rose-200'}>
          {valid
            ? 'This certificate was issued by the YEP programme and is currently valid.'
            : 'This certificate has been revoked and should not be accepted.'}
        </CardDescription>

        <dl className="mt-4 space-y-2 text-sm">
          <div className="flex justify-between gap-3">
            <dt className="text-slate-500 dark:text-slate-400">Awarded to</dt>
            <dd className="font-semibold text-slate-900 dark:text-slate-100">{result.youthName}</dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-slate-500 dark:text-slate-400">Achievement</dt>
            <dd className="text-right font-semibold text-slate-900 dark:text-slate-100">{result.achievement}</dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-slate-500 dark:text-slate-400">Activity</dt>
            <dd className="text-right font-medium text-slate-800 dark:text-slate-200">{result.activityTitle}</dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-slate-500 dark:text-slate-400">Issued</dt>
            <dd className="font-medium text-slate-800 dark:text-slate-200">{formatDate(result.issueDate)}</dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-slate-500 dark:text-slate-400">Certificate number</dt>
            <dd className="break-all font-mono text-xs text-slate-700 dark:text-slate-200">{result.certificateNumber}</dd>
          </div>
        </dl>

        <div className="mt-4 flex flex-wrap gap-2">
          <a
            className="inline-flex items-center rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800"
            href={`/api/v1/public/certificates/${encodeURIComponent(result.certificateNumber)}/pdf`}
          >
            Download PDF
          </a>
        </div>
      </Card>

      <p className="mt-6 text-sm text-slate-600 dark:text-slate-300">
        Only YEP can mint these certificates. If you believe a certificate is misreported,{' '}
        <Link href="/" className="font-semibold text-brand-700 hover:underline dark:text-brand-300">
          contact the programme team
        </Link>
        .
      </p>
    </div>
  );
}

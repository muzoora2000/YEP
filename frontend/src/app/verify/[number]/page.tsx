'use client';

import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';

import { api } from '@/lib/api';
import { formatDate } from '@/lib/format';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { SpinnerRow, ErrorState } from '@/components/ui/Feedback';

interface PublicCertificate {
  certificateNumber: string;
  recipientName: string;
  achievement: string;
  activityTitle: string;
  issueDate: string;
  valid: boolean;
}

export default function VerifyByNumberPage() {
  const params = useParams<{ number: string }>();
  const number = params.number;

  const query = useQuery({
    queryKey: ['public-cert', number],
    queryFn: () => api<PublicCertificate>(`/api/v1/public/certificates/${encodeURIComponent(number)}`),
    retry: 1,
  });

  if (query.isLoading) return <SpinnerRow label="Verifying certificate…" />;

  if (query.isError) {
    return (
      <div className="mx-auto max-w-lg text-center">
        <div className="mb-4 text-5xl" aria-hidden>✗</div>
        <h1 className="text-2xl font-bold text-rose-700 dark:text-rose-400">Certificate not found</h1>
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
          No certificate with number <code className="rounded bg-slate-100 px-1 py-0.5 dark:bg-slate-800">{number}</code> exists on this platform.
        </p>
        <Link href="/certificates/verify" className="mt-4 inline-block text-sm font-semibold text-brand-700 hover:underline dark:text-brand-300">
          Try another number
        </Link>
      </div>
    );
  }

  const cert = query.data!;

  return (
    <div className="mx-auto max-w-lg">
      <div className="mb-6 text-center">
        <div className="mb-3 text-5xl" aria-hidden>{cert.valid ? '✓' : '✗'}</div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Certificate verification</h1>
      </div>

      <Card>
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">Certificate number</p>
            <p className="mt-0.5 font-mono text-sm font-semibold text-slate-800 dark:text-slate-200">{cert.certificateNumber}</p>
          </div>
          <Badge tone={cert.valid ? 'COMPLETED' : 'CANCELLED'} className="shrink-0 text-sm font-bold px-3 py-1">
            {cert.valid ? 'VALID' : 'REVOKED'}
          </Badge>
        </div>

        <dl className="mt-5 space-y-3 text-sm">
          <div className="flex justify-between gap-3 border-t border-slate-100 pt-3 dark:border-slate-800">
            <dt className="text-slate-500 dark:text-slate-400">Recipient</dt>
            <dd className="font-semibold text-slate-900 dark:text-slate-100">{cert.recipientName}</dd>
          </div>
          <div className="flex justify-between gap-3 border-t border-slate-100 pt-3 dark:border-slate-800">
            <dt className="text-slate-500 dark:text-slate-400">Programme</dt>
            <dd className="font-medium text-slate-800 dark:text-slate-200">{cert.activityTitle}</dd>
          </div>
          <div className="flex justify-between gap-3 border-t border-slate-100 pt-3 dark:border-slate-800">
            <dt className="text-slate-500 dark:text-slate-400">Achievement</dt>
            <dd className="text-slate-800 dark:text-slate-200">{cert.achievement}</dd>
          </div>
          <div className="flex justify-between gap-3 border-t border-slate-100 pt-3 dark:border-slate-800">
            <dt className="text-slate-500 dark:text-slate-400">Issue date</dt>
            <dd className="text-slate-800 dark:text-slate-200">{formatDate(cert.issueDate)}</dd>
          </div>
        </dl>

        <p className="mt-5 text-xs text-slate-500 dark:text-slate-400">
          This information is provided by the YEP platform. The certificate number uniquely identifies this achievement.
        </p>
      </Card>

      <div className="mt-4 text-center">
        <Link href="/certificates/verify" className="text-sm font-semibold text-brand-700 hover:underline dark:text-brand-300">
          Verify another certificate
        </Link>
      </div>
    </div>
  );
}

import Link from 'next/link';

import { LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';

export const metadata = {
  title: 'YEP - Youth Digital Empowerment and Virtual Exchange Platform',
  description:
    'Level One and Level Two virtual exchanges, impact projects, mentoring and verifiable certificates for young people in Uganda and East Africa.',
};

const features = [
  {
    title: 'Level One exchanges',
    description:
      'Large-group online sessions on digital literacy, AI, critical thinking, media literacy and intercultural communication.',
  },
  {
    title: 'Level Two exchanges',
    description:
      'Small teams solving real problems in employment, education, agriculture, health, climate and digital inclusion.',
  },
  {
    title: 'Impact projects',
    description:
      'Teams document the problem, propose a solution, submit for review and complete a project with a human reviewer in the loop.',
  },
  {
    title: 'Assessment & recognition',
    description:
      'Facilitators and mentors assess participants and teams; every completion produces a publicly verifiable certificate.',
  },
  {
    title: 'Messaging & announcements',
    description:
      'Direct messages, team messages and cohort announcements, with unread counts and moderation for AI-drafted text.',
  },
  {
    title: 'Responsible AI by default',
    description:
      'AI can suggest drafts and moderate content, but every suggestion is labelled AI-generated and waits for human approval.',
  },
];

const steps = [
  'Create an account and give consent',
  'Browse and enrol in a virtual exchange',
  'Form a team inside a Level Two programme',
  'Build and submit an impact project',
  'Get assessed and receive your certificate',
];

export default function HomePage() {
  return (
    <div className="space-y-14">
      <section className="rounded-3xl bg-gradient-to-br from-brand-700 via-brand-600 to-indigo-500 px-6 py-12 text-white sm:px-10 sm:py-16">
        <p className="text-sm font-semibold uppercase tracking-wide text-brand-100">
          Youth Digital Empowerment &amp; Virtual Exchange
        </p>
        <h1 className="mt-3 max-w-3xl text-3xl font-bold leading-tight sm:text-5xl">
          Young people across East Africa, learning together and solving real problems.
        </h1>
        <p className="mt-4 max-w-2xl text-sm leading-relaxed text-brand-50 sm:text-base">
          YEP runs mobile-first, low-bandwidth virtual exchanges: join a Level One session, team up for a
          Level Two impact project, get mentored, and graduate with a certificate anyone can verify.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <LinkButton href="/register" className="bg-white dark:bg-slate-900 !text-brand-700 dark:!text-brand-300 hover:bg-brand-50 dark:hover:bg-brand-500/20">
            Join YEP
          </LinkButton>
          <LinkButton href="/exchanges" variant="secondary" className="!text-brand-800 dark:!text-brand-200 ring-white/60">
            Browse exchanges
          </LinkButton>
        </div>
      </section>

      <section aria-labelledby="what-you-can-do">
        <h2 id="what-you-can-do" className="text-xl font-bold text-slate-900 dark:text-slate-100 sm:text-2xl">
          What you can do on YEP
        </h2>
        <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((feature) => (
            <Card key={feature.title} as="article">
              <CardTitle>{feature.title}</CardTitle>
              <CardDescription>{feature.description}</CardDescription>
            </Card>
          ))}
        </div>
      </section>

      <section aria-labelledby="how-it-works" className="grid gap-8 lg:grid-cols-2">
        <div>
          <h2 id="how-it-works" className="text-xl font-bold text-slate-900 dark:text-slate-100 sm:text-2xl">
            How it works
          </h2>
          <ol className="mt-5 space-y-3">
            {steps.map((step, index) => (
              <li key={step} className="flex items-start gap-3 text-sm text-slate-700 dark:text-slate-200">
                <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-brand-100 text-xs font-bold text-brand-800 dark:bg-brand-500/20 dark:text-brand-200">
                  {index + 1}
                </span>
                <span className="pt-0.5">{step}</span>
              </li>
            ))}
          </ol>
        </div>

        <Card>
          <CardTitle>Built for low bandwidth</CardTitle>
          <CardDescription>
            YEP is a progressive web app: it installs on a phone, keeps an offline shell, lazy-loads images
            and paginates every API response, so it stays usable on a metered connection.
          </CardDescription>
          <div className="mt-4 flex flex-wrap gap-2 text-xs font-medium text-slate-600 dark:text-slate-300">
            <span className="rounded-full bg-slate-100 dark:bg-slate-800 px-3 py-1">PWA offline shell</span>
            <span className="rounded-full bg-slate-100 dark:bg-slate-800 px-3 py-1">WCAG 2.1 AA patterns</span>
            <span className="rounded-full bg-slate-100 dark:bg-slate-800 px-3 py-1">BCrypt + JWT</span>
            <span className="rounded-full bg-slate-100 dark:bg-slate-800 px-3 py-1">Consent-first registration</span>
          </div>
          <div className="mt-5">
            <Link href="/certificates/verify" className="text-sm font-semibold text-brand-700 dark:text-brand-300 hover:underline">
              Verify an existing certificate →
            </Link>
          </div>
        </Card>
      </section>
    </div>
  );
}

'use client';

import Link from 'next/link';

/** Tab navigation bar shared across all facilitator activity sub-pages. */
export function ActivityNav({ activityId, active }: { activityId: number; active: string }) {
  const tabs = [
    { key: 'materials', label: 'Materials', href: `/facilitator/activities/${activityId}/materials` },
    { key: 'sessions', label: 'Sessions', href: `/facilitator/activities/${activityId}/sessions` },
    { key: 'assessments', label: 'Assessments', href: `/facilitator/activities/${activityId}/assessments` },
    { key: 'participants', label: 'Participants', href: `/facilitator/activities/${activityId}/participants` },
    { key: 'teams', label: 'Teams', href: `/facilitator/activities/${activityId}/teams` },
  ];
  return (
    <nav aria-label="Activity sections"
      className="flex flex-wrap gap-1 rounded-xl bg-slate-100 p-1 dark:bg-slate-800">
      {tabs.map(t => (
        <Link key={t.key} href={t.href}
          className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
            active === t.key
              ? 'bg-white text-slate-900 shadow dark:bg-slate-700 dark:text-slate-50'
              : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100'
          }`}>
          {t.label}
        </Link>
      ))}
    </nav>
  );
}

/** Small display helpers shared across pages. */

export function formatDate(value: string | null | undefined): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium' }).format(date);
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export function titleCase(value: string | null | undefined): string {
  if (!value) return '—';
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export function initials(name: string | null | undefined): string {
  if (!name) return '?';
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}

/** Maps an enum to a Tailwind badge colour class. */
export function badgeTone(value: string | null | undefined): string {
  switch (value) {
    case 'ACTIVE':
    case 'OPEN':
    case 'COMPLETED':
    case 'APPROVED':
    case 'VALID':
      return 'bg-emerald-100 text-emerald-800 ring-emerald-200 dark:bg-emerald-500/20 dark:text-emerald-300 dark:ring-emerald-500/40';
    case 'DRAFT':
    case 'PENDING':
    case 'IN_PROGRESS':
    case 'SUBMITTED':
    case 'UNDER_REVIEW':
      return 'bg-amber-100 text-amber-800 ring-amber-200 dark:bg-amber-500/20 dark:text-amber-300 dark:ring-amber-500/40';
    case 'CANCELLED':
    case 'WITHDRAWN':
    case 'REJECTED':
    case 'SUSPENDED':
    case 'BLOCKED':
      return 'bg-rose-100 text-rose-800 ring-rose-200 dark:bg-rose-500/20 dark:text-rose-300 dark:ring-rose-500/40';
    default:
      return 'bg-slate-100 text-slate-700 ring-slate-200 dark:bg-slate-700/40 dark:text-slate-300 dark:ring-slate-600';
  }
}

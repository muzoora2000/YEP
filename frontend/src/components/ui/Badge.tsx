import { badgeTone } from '@/lib/format';

interface BadgeProps {
  children: React.ReactNode;
  /** Any enum-like value - drives the colour automatically. */
  tone?: string;
  className?: string;
}

export function Badge({ children, tone, className = '' }: BadgeProps) {
  const palette = tone
    ? badgeTone(tone)
    : 'bg-brand-100 text-brand-800 ring-brand-200 dark:bg-brand-500/20 dark:text-brand-200 dark:ring-brand-500/40';
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${palette} ${className}`.trim()}
    >
      {children}
    </span>
  );
}

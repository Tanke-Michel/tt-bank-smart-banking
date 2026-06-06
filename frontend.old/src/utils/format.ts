/** Format a number as a currency string */
export function formatCurrency(amount: number | undefined | null, currency = 'XAF'): string {
  if (amount == null) return '—';
  return new Intl.NumberFormat('fr-CM', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
}

/** Format an ISO datetime string to a readable local date + time */
export function formatDateTime(dt: string | undefined | null): string {
  if (!dt) return '—';
  return new Intl.DateTimeFormat('fr-CM', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(dt));
}

/** Format an ISO date string to a short date */
export function formatDate(dt: string | undefined | null): string {
  if (!dt) return '—';
  return new Intl.DateTimeFormat('fr-CM', { dateStyle: 'medium' }).format(new Date(dt));
}

/** Truncate a string and add ellipsis */
export function truncate(s: string, max = 20): string {
  return s.length > max ? s.slice(0, max) + '…' : s;
}

/** Status badge colour classes */
export const statusColour: Record<string, string> = {
  // Wallet
  ACTIVE:    'bg-emerald-500/15 text-emerald-400',
  SUSPENDED: 'bg-amber-500/15  text-amber-400',
  CLOSED:    'bg-red-500/15    text-red-400',
  // Transaction / Payment
  COMPLETED: 'bg-emerald-500/15 text-emerald-400',
  FAILED:    'bg-red-500/15    text-red-400',
  PENDING:   'bg-amber-500/15  text-amber-400',
  REVERSED:  'bg-slate-500/15  text-slate-400',
  // Merchant
  REJECTED:  'bg-red-500/15    text-red-400',
  // Savings
  FORMING:   'bg-blue-500/15   text-blue-400',
  CANCELLED: 'bg-slate-500/15  text-slate-400',
  // Contribution
  PAID:      'bg-emerald-500/15 text-emerald-400',
  WAIVED:    'bg-slate-500/15  text-slate-400',
};

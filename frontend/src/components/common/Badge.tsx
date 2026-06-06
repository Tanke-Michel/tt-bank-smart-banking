import { statusColour } from '../../utils/format';

interface BadgeProps { label: string; status?: string; }

export function Badge({ label, status }: BadgeProps) {
  const cls = status ? (statusColour[status] ?? 'bg-slate-500/15 text-slate-400') : '';
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${cls}`}>
      {label}
    </span>
  );
}

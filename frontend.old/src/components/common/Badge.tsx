import { type ReactNode, type CSSProperties } from 'react';

interface BadgeProps {
  children?: ReactNode;
  label?: string;
  status?: string;
  color?: string;
}

const statusMap: Record<string, string> = {
  ACTIVE: 'green', FORMING: 'amber', COMPLETED: 'violet',
  CANCELLED: 'rose', SUSPENDED: 'rose', SUCCESS: 'green',
  PENDING: 'amber', FAILED: 'rose', USER: 'violet', ADMIN: 'amber',
};

export function Badge({ children, label, status, color }: BadgeProps) {
  const text = children ?? label ?? status ?? '';
  const c = color ?? (status ? statusMap[status.toUpperCase()] ?? 'default' : 'default');
  const styles: Record<string, CSSProperties> = {
    green:   { background: 'rgba(16,185,129,0.1)',  border: '1px solid rgba(16,185,129,0.2)',  color: '#10b981' },
    violet:  { background: 'rgba(139,92,246,0.1)',  border: '1px solid rgba(139,92,246,0.2)',  color: '#8b5cf6' },
    amber:   { background: 'rgba(245,158,11,0.1)',  border: '1px solid rgba(245,158,11,0.2)',  color: '#f59e0b' },
    rose:    { background: 'rgba(244,63,94,0.1)',   border: '1px solid rgba(244,63,94,0.2)',   color: '#f43f5e' },
    emerald: { background: 'rgba(16,185,129,0.1)',  border: '1px solid rgba(16,185,129,0.2)',  color: '#10b981' },
    default: { background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.1)', color: '#8080a8' },
  };
  return (
    <span className="text-xs font-semibold px-2.5 py-1 rounded-full" style={styles[c] ?? styles.default}>
      {String(text)}
    </span>
  );
}

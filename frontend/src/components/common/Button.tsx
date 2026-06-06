import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Spinner } from './Spinner';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode;
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  loading?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

export function Button({
  children, variant = 'primary', loading, size = 'md', className = '', disabled, ...props
}: ButtonProps) {
  const base = 'inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition-all focus:outline-none focus:ring-2 focus:ring-[var(--accent)] disabled:opacity-50 disabled:cursor-not-allowed';
  const sizes = { sm: 'px-3 py-1.5 text-sm', md: 'px-4 py-2.5 text-sm', lg: 'px-6 py-3 text-base' };
  const variants = {
    primary:   'bg-[var(--accent)] text-white hover:opacity-90 active:scale-95',
    secondary: 'bg-[var(--card)] border border-[var(--border)] text-[var(--text)] hover:bg-[var(--border)]',
    danger:    'bg-red-600 text-white hover:bg-red-700 active:scale-95',
    ghost:     'text-[var(--muted)] hover:text-[var(--text)] hover:bg-[var(--border)]',
  };
  return (
    <button
      {...props}
      disabled={loading || disabled}
      className={`${base} ${sizes[size]} ${variants[variant]} ${className}`}
    >
      {loading && <Spinner size="sm" />}
      {children}
    </button>
  );
}

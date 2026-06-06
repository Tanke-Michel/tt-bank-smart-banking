import { type ReactNode, type ButtonHTMLAttributes, type CSSProperties } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  children: ReactNode;
}

export function Button({ variant = 'primary', size = 'md', loading = false, children, disabled, className = '', style, ...props }: ButtonProps) {
  const sizes: Record<string, string> = { sm: 'px-3 py-1.5 text-xs', md: 'px-5 py-3 text-sm', lg: 'px-6 py-4 text-base' };
  const variantStyles: Record<string, CSSProperties> = {
    primary:   { background: 'linear-gradient(135deg, #10b981, #059669)', color: 'white' },
    secondary: { background: 'rgba(16,185,129,0.1)', border: '1px solid rgba(16,185,129,0.2)', color: '#10b981' },
    ghost:     { background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)', color: '#f0f0f8' },
    danger:    { background: 'rgba(244,63,94,0.1)', border: '1px solid rgba(244,63,94,0.2)', color: '#f43f5e' },
  };
  return (
    <button disabled={disabled || loading}
      className={`${sizes[size]} rounded-xl font-bold transition-all flex items-center gap-2 justify-center ${variant === 'primary' ? 'btn-primary' : ''} ${className}`}
      style={{ ...variantStyles[variant], opacity: (disabled || loading) ? 0.5 : 1, cursor: (disabled || loading) ? 'not-allowed' : 'pointer', ...style }}
      {...props}>
      {loading ? <div className="w-4 h-4 border-2 rounded-full animate-spin" style={{ borderColor: 'rgba(255,255,255,0.3)', borderTopColor: 'white' }} /> : children}
    </button>
  );
}

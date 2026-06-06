import { type InputHTMLAttributes, forwardRef } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(({ label, error, className = '', style, ...props }, ref) => (
  <div className="space-y-1.5">
    {label && <label className="block text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>{label}</label>}
    <input ref={ref}
      className={`input-field w-full px-4 py-3 text-sm ${className}`}
      style={{ borderColor: error ? '#f43f5e' : undefined, ...style }}
      {...props} />
    {error && <p className="text-xs" style={{ color: '#f43f5e' }}>{error}</p>}
  </div>
));
Input.displayName = 'Input';

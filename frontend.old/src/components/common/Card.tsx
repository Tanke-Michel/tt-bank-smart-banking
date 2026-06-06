import { type ReactNode, type CSSProperties } from 'react';

interface CardProps {
  children: ReactNode;
  className?: string;
  glow?: 'green' | 'violet' | 'amber' | 'rose' | false;
  style?: CSSProperties;
}

export function Card({ children, className = '', glow = false, style = {} }: CardProps) {
  const glows: Record<string, string> = {
    green:  '0 4px 40px rgba(16,185,129,0.15)',
    violet: '0 4px 40px rgba(139,92,246,0.15)',
    amber:  '0 4px 40px rgba(245,158,11,0.15)',
    rose:   '0 4px 40px rgba(244,63,94,0.15)',
  };
  return (
    <div className={`rounded-2xl p-6 ${className}`}
      style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)',
               backdropFilter: 'blur(20px)', boxShadow: glow ? glows[glow] : 'none', ...style }}>
      {children}
    </div>
  );
}

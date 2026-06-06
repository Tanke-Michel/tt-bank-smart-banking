import type { ReactNode, CSSProperties, MouseEventHandler } from 'react';

interface CardProps {
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
  onClick?: MouseEventHandler<HTMLDivElement>;
}

export function Card({ children, className = '', style, onClick }: CardProps) {
  return (
    <div
      className={`rounded-2xl bg-[var(--card)] border border-[var(--border)] p-6 ${className}`}
      style={style}
      onClick={onClick}
    >
      {children}
    </div>
  );
}

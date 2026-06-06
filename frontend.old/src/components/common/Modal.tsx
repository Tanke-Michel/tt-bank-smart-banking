import { type ReactNode } from 'react';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  children: ReactNode;
}

export function Modal({ open, onClose, title, children }: ModalProps) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" onClick={onClose}
      style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(8px)' }}>
      <div className="w-full max-w-lg rounded-2xl p-6 animate-fade-in"
        style={{ background: '#0d0d24', border: '1px solid rgba(255,255,255,0.08)', boxShadow: '0 40px 100px rgba(0,0,0,0.6)' }}
        onClick={e => e.stopPropagation()}>
        {title && (
          <div className="flex items-center justify-between mb-5">
            <h3 className="text-white font-bold text-lg" style={{ fontFamily: "'Clash Display', sans-serif" }}>{title}</h3>
            <button onClick={onClose} className="text-gray-600 hover:text-gray-300 transition-colors text-lg">✕</button>
          </div>
        )}
        {children}
      </div>
    </div>
  );
}

interface EmptyStateProps { icon?: string; message?: string; subtitle?: string; }

export function EmptyState({ icon = '◎', message = 'Nothing here yet', subtitle }: EmptyStateProps) {
  return (
    <div className="py-16 text-center">
      <p className="text-4xl mb-3">{icon}</p>
      <p className="font-medium" style={{ color: '#8080a8' }}>{message}</p>
      {subtitle && <p className="text-sm mt-1" style={{ color: '#404060' }}>{subtitle}</p>}
    </div>
  );
}

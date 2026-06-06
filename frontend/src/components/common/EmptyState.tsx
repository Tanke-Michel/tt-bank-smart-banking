interface EmptyStateProps { icon?: string; message: string; }
export function EmptyState({ icon = '📭', message }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 gap-3 text-[var(--muted)]">
      <span className="text-4xl">{icon}</span>
      <p className="text-sm">{message}</p>
    </div>
  );
}

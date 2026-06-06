export function Spinner({ size = 'md' }: { size?: 'sm' | 'md' | 'lg' }) {
  const s = size === 'sm' ? 'h-4 w-4' : size === 'lg' ? 'h-12 w-12' : 'h-6 w-6';
  return (
    <span className={`${s} inline-block animate-spin rounded-full border-2 border-current border-t-transparent opacity-70`} />
  );
}

export function Spinner({ size = 'md' }: { size?: 'sm' | 'md' | 'lg' }) {
  const s = { sm: 'w-4 h-4', md: 'w-6 h-6', lg: 'w-8 h-8' }[size];
  return <div className={`${s} border-2 rounded-full animate-spin`}
    style={{ borderColor: 'rgba(16,185,129,0.3)', borderTopColor: '#10b981' }} />;
}

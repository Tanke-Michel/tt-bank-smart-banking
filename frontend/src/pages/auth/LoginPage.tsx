import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/auth';
import { walletApi } from '../../api/wallet';
import { useAuthStore } from '../../store/auth.store';
import { getErrorMessage } from '../../api/client';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';

export function LoginPage() {
  const navigate = useNavigate();
  const { setAuth, setWalletNumber } = useAuthStore();
  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const { data } = await authApi.login(form);
      setAuth(data);
      // Eagerly fetch wallet number so the header is available immediately
      try {
        const { data: wallet } = await walletApi.getMyWallet();
        setWalletNumber(wallet.walletNumber);
      } catch {
        // Wallet may not exist yet — user will be prompted to create one
      }
      navigate('/dashboard');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[var(--bg)] flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        {/* Brand */}
        <div className="text-center mb-8">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--accent)] text-white text-2xl font-black mb-4">S</div>
          <h1 className="text-2xl font-black text-[var(--text)]">SmartBank</h1>
          <p className="text-sm text-[var(--muted)] mt-1">Digital Wallet for Africa</p>
        </div>

        <form onSubmit={handleSubmit} className="rounded-2xl bg-[var(--card)] border border-[var(--border)] p-6 space-y-4">
          <h2 className="text-lg font-bold text-[var(--text)]">Sign in</h2>
          {error && (
            <div className="rounded-xl bg-red-500/10 border border-red-500/20 px-4 py-3 text-sm text-red-400">
              {error}
            </div>
          )}
          <Input
            label="Email"
            type="email"
            placeholder="you@example.com"
            value={form.email}
            onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
            required
          />
          <Input
            label="Password"
            type="password"
            placeholder="••••••••"
            value={form.password}
            onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
            required
          />
          <div className="flex items-center justify-end">
            <Link to="/forgot-password" className="text-xs text-[var(--accent)] hover:underline">
              Forgot password?
            </Link>
          </div>
          <Button type="submit" loading={loading} className="w-full">
            Sign in
          </Button>
          <p className="text-center text-sm text-[var(--muted)]">
            No account?{' '}
            <Link to="/register" className="text-[var(--accent)] hover:underline font-medium">
              Create one
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}

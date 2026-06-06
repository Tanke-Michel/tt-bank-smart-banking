import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/auth';
import { getErrorMessage } from '../../api/client';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';

export function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', phoneNumber: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await authApi.register(form);
      setSuccess(true);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen bg-[var(--bg)] flex items-center justify-center p-4">
        <div className="w-full max-w-sm rounded-2xl bg-[var(--card)] border border-[var(--border)] p-6 text-center space-y-4">
          <div className="text-4xl">✉️</div>
          <h2 className="text-lg font-bold text-[var(--text)]">Check your email</h2>
          <p className="text-sm text-[var(--muted)]">
            We sent a verification code to <strong>{form.email}</strong>.
            Enter it to activate your account.
          </p>
          <Button onClick={() => navigate('/login')} className="w-full">
            Go to sign in
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[var(--bg)] flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--accent)] text-white text-2xl font-black mb-4">S</div>
          <h1 className="text-2xl font-black text-[var(--text)]">SmartBank</h1>
        </div>
        <form onSubmit={handleSubmit} className="rounded-2xl bg-[var(--card)] border border-[var(--border)] p-6 space-y-4">
          <h2 className="text-lg font-bold text-[var(--text)]">Create account</h2>
          {error && (
            <div className="rounded-xl bg-red-500/10 border border-red-500/20 px-4 py-3 text-sm text-red-400">
              {error}
            </div>
          )}
          <Input label="Full name" placeholder="Jean Dupont" value={form.fullName}
            onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} required />
          <Input label="Email" type="email" placeholder="you@example.com" value={form.email}
            onChange={e => setForm(f => ({ ...f, email: e.target.value }))} required />
          <Input label="Phone number" placeholder="+237600000000" value={form.phoneNumber}
            onChange={e => setForm(f => ({ ...f, phoneNumber: e.target.value }))} required />
          <Input label="Password" type="password" placeholder="••••••••" value={form.password}
            onChange={e => setForm(f => ({ ...f, password: e.target.value }))} required />
          <Button type="submit" loading={loading} className="w-full">Create account</Button>
          <p className="text-center text-sm text-[var(--muted)]">
            Already have an account?{' '}
            <Link to="/login" className="text-[var(--accent)] hover:underline font-medium">Sign in</Link>
          </p>
        </form>
      </div>
    </div>
  );
}

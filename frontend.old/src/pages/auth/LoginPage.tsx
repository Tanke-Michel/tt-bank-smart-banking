import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/auth.store';
import { authApi } from '../../api/auth';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { setAuth } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!email || !password) { setError('Please fill in all fields'); return; }
    setLoading(true);
    try {
      const res = await authApi.login({ email, password });
      setAuth(res.data);
      navigate('/dashboard');
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? 'Invalid credentials';
      setError(msg.includes('verified') ? 'Please verify your email first. Check your inbox for the OTP.' : msg);
    } finally { setLoading(false); }
  };

  const inputStyle = {
    background: 'rgba(8,8,24,0.8)',
    border: '1px solid rgba(255,255,255,0.06)',
    color: '#f0f0f8',
    outline: 'none',
    borderRadius: '12px',
    padding: '14px 16px',
    width: '100%',
    fontSize: '14px',
    fontFamily: "'Cabinet Grotesk', sans-serif",
    transition: 'border-color 0.2s',
  };

  return (
    <div className="min-h-screen flex relative overflow-hidden" style={{ background: '#04040e' }}>
      <div className="absolute w-96 h-96 rounded-full animate-pulse-slow pointer-events-none"
        style={{ background: 'radial-gradient(circle, #10b981, transparent 70%)', top: '-100px', right: '-100px', filter: 'blur(60px)', opacity: 0.3 }} />
      <div className="absolute w-80 h-80 rounded-full animate-pulse-slow pointer-events-none"
        style={{ background: 'radial-gradient(circle, #8b5cf6, transparent 70%)', bottom: '-100px', left: '-80px', filter: 'blur(60px)', opacity: 0.2, animationDelay: '1.5s' }} />

      {/* Left branding panel */}
      <div className="hidden lg:flex flex-col justify-between w-1/2 p-12 relative z-10">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl flex items-center justify-center text-white font-black text-xl"
            style={{ background: 'linear-gradient(135deg, #10b981, #059669)', boxShadow: '0 0 30px rgba(16,185,129,0.4)' }}>S</div>
          <div>
            <span className="text-white font-bold text-2xl" style={{ fontFamily: "'Clash Display', sans-serif" }}>Smart</span>
            <span className="font-bold text-2xl" style={{ color: '#10b981' }}>Bank</span>
          </div>
        </div>

        <div>
          <span className="inline-block text-xs font-bold tracking-widest uppercase px-3 py-1.5 rounded-full mb-6"
            style={{ background: 'rgba(16,185,129,0.1)', border: '1px solid rgba(16,185,129,0.2)', color: '#10b981' }}>
            🌍 Banking for Africa
          </span>
          <h2 className="text-5xl font-bold text-white leading-tight mb-4" style={{ fontFamily: "'Clash Display', sans-serif" }}>
            Your money,<br /><span style={{ color: '#10b981' }}>smarter.</span>
          </h2>
          <p className="text-lg leading-relaxed max-w-sm" style={{ color: '#8080a8' }}>
            Send, save, and manage your money with zero friction. Built for the modern generation.
          </p>
          <div className="flex gap-8 mt-10">
            {[{ label: 'Active Users', value: '50K+' }, { label: 'Daily Transfers', value: 'XAF 2B+' }, { label: 'Countries', value: '8' }].map(s => (
              <div key={s.label}>
                <p className="text-2xl font-bold" style={{ color: '#10b981', fontFamily: "'Clash Display', sans-serif" }}>{s.value}</p>
                <p className="text-xs mt-0.5" style={{ color: '#404060' }}>{s.label}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="relative h-48">
          <div className="absolute left-0 bottom-0 w-64 h-36 rounded-2xl p-5 card-shine animate-float"
            style={{ background: 'linear-gradient(135deg, #064e3b, #10b981, #059669)', boxShadow: '0 20px 60px rgba(16,185,129,0.3)', transform: 'rotate(-6deg)' }}>
            <p className="text-xs uppercase tracking-widest" style={{ color: 'rgba(255,255,255,0.6)' }}>Smart Card</p>
            <p className="text-xs mt-4 tracking-widest" style={{ color: 'rgba(255,255,255,0.8)', fontFamily: "'JetBrains Mono', monospace" }}>•••• •••• •••• 4291</p>
            <div className="flex justify-between items-end mt-3">
              <p className="text-white text-xs font-bold">ACCOUNT HOLDER</p>
              <p className="text-xs" style={{ color: 'rgba(255,255,255,0.7)' }}>12/27</p>
            </div>
          </div>
          <div className="absolute left-16 bottom-8 w-64 h-36 rounded-2xl p-5"
            style={{ background: 'linear-gradient(135deg, #2e1065, #8b5cf6, #6d28d9)', boxShadow: '0 20px 60px rgba(139,92,246,0.3)', transform: 'rotate(3deg)', zIndex: -1 }}>
            <p className="text-xs uppercase tracking-widest" style={{ color: 'rgba(255,255,255,0.6)' }}>Savings Card</p>
          </div>
        </div>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex items-center justify-center p-8 relative z-10">
        <div className="w-full max-w-md rounded-3xl p-8"
          style={{ background: 'rgba(8,8,24,0.9)', border: '1px solid rgba(255,255,255,0.06)', backdropFilter: 'blur(30px)', boxShadow: '0 40px 100px rgba(0,0,0,0.5)' }}>

          <div className="flex lg:hidden items-center gap-2 mb-8">
            <div className="w-9 h-9 rounded-xl flex items-center justify-center text-white font-black"
              style={{ background: 'linear-gradient(135deg, #10b981, #059669)' }}>S</div>
            <span className="text-white font-bold text-xl" style={{ fontFamily: "'Clash Display', sans-serif" }}>SmartBank</span>
          </div>

          <div className="mb-8">
            <h3 className="text-white text-2xl font-bold" style={{ fontFamily: "'Clash Display', sans-serif" }}>Welcome back 👋</h3>
            <p className="text-sm mt-1" style={{ color: '#8080a8' }}>Sign in to your account</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Email Address</label>
              <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="your@email.com"
                style={inputStyle}
                onFocus={e => e.target.style.borderColor = '#10b981'}
                onBlur={e => e.target.style.borderColor = 'rgba(255,255,255,0.06)'} />
            </div>
            <div>
              <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Password</label>
              <div className="relative">
                <input type={showPass ? 'text' : 'password'} value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••"
                  style={{ ...inputStyle, paddingRight: '48px' }}
                  onFocus={e => e.target.style.borderColor = '#10b981'}
                  onBlur={e => e.target.style.borderColor = 'rgba(255,255,255,0.06)'} />
                <button type="button" onClick={() => setShowPass(!showPass)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-sm transition-colors" style={{ color: '#404060' }}>
                  {showPass ? '🙈' : '👁'}
                </button>
              </div>
            </div>

            <div className="flex justify-between items-center">
              <span />
              <button type="button" onClick={() => navigate('/forgot-password')} className="text-xs transition-colors" style={{ color: '#10b981' }}>
                Forgot password?
              </button>
            </div>

            {error && (
              <div className="px-4 py-3 rounded-xl text-sm" style={{ background: 'rgba(244,63,94,0.1)', border: '1px solid rgba(244,63,94,0.2)', color: '#f43f5e' }}>
                {error}
              </div>
            )}

            <button type="submit" disabled={loading}
              className="btn-primary w-full py-4 flex items-center justify-center gap-2 font-bold text-sm mt-2">
              {loading ? <div className="w-5 h-5 border-2 rounded-full animate-spin" style={{ borderColor: 'rgba(255,255,255,0.3)', borderTopColor: 'white' }} /> : <>Sign In →</>}
            </button>
          </form>

          <div className="flex items-center gap-4 my-6">
            <div className="flex-1 h-px" style={{ background: 'rgba(255,255,255,0.06)' }} />
            <span className="text-xs" style={{ color: '#404060' }}>or</span>
            <div className="flex-1 h-px" style={{ background: 'rgba(255,255,255,0.06)' }} />
          </div>

          <p className="text-xs text-center" style={{ color: '#404060' }}>
            No account?{' '}
            <button onClick={() => navigate('/register')} className="font-medium transition-colors" style={{ color: '#10b981' }}>
              Create one →
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}

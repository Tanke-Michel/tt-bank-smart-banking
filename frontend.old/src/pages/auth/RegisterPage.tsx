import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/auth';
import { getErrorMessage } from '../../api/client';
import { useAuthStore } from '../../store/auth.store';

export function RegisterPage() {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const [step, setStep] = useState<'form' | 'otp'>('form');
  const [form, setForm] = useState({ fullName: '', email: '', phoneNumber: '', password: '' });
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  const inputStyle: React.CSSProperties = {
    background: 'rgba(8,8,24,0.8)', border: '1px solid rgba(255,255,255,0.06)',
    color: '#f0f0f8', outline: 'none', borderRadius: '12px',
    padding: '14px 16px', width: '100%', fontSize: '14px',
    fontFamily: "'Cabinet Grotesk', sans-serif", transition: 'border-color 0.2s',
  };
  const onFocus = (e: React.FocusEvent<HTMLInputElement>) => { e.target.style.borderColor = '#10b981'; };
  const onBlur  = (e: React.FocusEvent<HTMLInputElement>) => { e.target.style.borderColor = 'rgba(255,255,255,0.06)'; };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      const res = await authApi.register(form);
      setAuth(res.data);
      setInfo('We sent a 6-digit code to ' + form.email);
      setStep('otp');
    } catch (err) { setError(getErrorMessage(err)); }
    finally { setLoading(false); }
  };

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      await authApi.verifyEmail(form.email, otp.trim());
      navigate('/dashboard');
    } catch (err) { setError(getErrorMessage(err)); }
    finally { setLoading(false); }
  };

  const resend = async () => {
    setError(''); setInfo('');
    try { await authApi.register(form); setInfo('Code resent to ' + form.email); }
    catch { setInfo('A new code was sent if your account is pending.'); }
  };

  return (
    <div className="min-h-screen flex relative overflow-hidden" style={{ background: '#04040e' }}>
      <div className="absolute w-96 h-96 rounded-full animate-pulse-slow pointer-events-none"
        style={{ background: 'radial-gradient(circle, #10b981, transparent 70%)', top: '-100px', right: '-100px', filter: 'blur(60px)', opacity: 0.3 }} />
      <div className="absolute w-80 h-80 rounded-full animate-pulse-slow pointer-events-none"
        style={{ background: 'radial-gradient(circle, #8b5cf6, transparent 70%)', bottom: '-100px', left: '-80px', filter: 'blur(60px)', opacity: 0.2, animationDelay: '1.5s' }} />

      {/* Left branding */}
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
            🌍 Join SmartBank
          </span>
          <h2 className="text-5xl font-bold text-white leading-tight mb-4" style={{ fontFamily: "'Clash Display', sans-serif" }}>
            Open your<br /><span style={{ color: '#10b981' }}>account.</span>
          </h2>
          <p className="text-lg leading-relaxed max-w-sm" style={{ color: '#8080a8' }}>
            Create a free account in seconds. Send money, save, and manage everything in one place.
          </p>
        </div>
        <div className="relative h-48">
          <div className="absolute left-0 bottom-0 w-64 h-36 rounded-2xl p-5 card-shine animate-float"
            style={{ background: 'linear-gradient(135deg, #064e3b, #10b981, #059669)', boxShadow: '0 20px 60px rgba(16,185,129,0.3)', transform: 'rotate(-6deg)' }}>
            <p className="text-xs uppercase tracking-widest" style={{ color: 'rgba(255,255,255,0.6)' }}>Smart Card</p>
            <p className="text-xs mt-4 tracking-widest" style={{ color: 'rgba(255,255,255,0.8)', fontFamily: "'JetBrains Mono', monospace" }}>•••• •••• •••• 4291</p>
          </div>
        </div>
      </div>

      {/* Right form */}
      <div className="flex-1 flex items-center justify-center p-8 relative z-10">
        <div className="w-full max-w-md rounded-3xl p-8"
          style={{ background: 'rgba(8,8,24,0.9)', border: '1px solid rgba(255,255,255,0.06)', backdropFilter: 'blur(30px)', boxShadow: '0 40px 100px rgba(0,0,0,0.5)' }}>

          <div className="flex lg:hidden items-center gap-2 mb-8">
            <div className="w-9 h-9 rounded-xl flex items-center justify-center text-white font-black"
              style={{ background: 'linear-gradient(135deg, #10b981, #059669)' }}>S</div>
            <span className="text-white font-bold text-xl" style={{ fontFamily: "'Clash Display', sans-serif" }}>SmartBank</span>
          </div>

          {step === 'form' ? (
            <>
              <div className="mb-8">
                <h3 className="text-white text-2xl font-bold" style={{ fontFamily: "'Clash Display', sans-serif" }}>Create account ✨</h3>
                <p className="text-sm mt-1" style={{ color: '#8080a8' }}>Get started in under a minute</p>
              </div>
              {error && <div className="px-4 py-3 rounded-xl text-sm mb-4" style={{ background: 'rgba(244,63,94,0.1)', border: '1px solid rgba(244,63,94,0.2)', color: '#f43f5e' }}>{error}</div>}
              <form onSubmit={handleRegister} className="space-y-4">
                <div>
                  <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Full Name</label>
                  <input value={form.fullName} onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} placeholder="Jean Dupont" required style={inputStyle} onFocus={onFocus} onBlur={onBlur} />
                </div>
                <div>
                  <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Email Address</label>
                  <input type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} placeholder="you@email.com" required style={inputStyle} onFocus={onFocus} onBlur={onBlur} />
                </div>
                <div>
                  <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Phone Number</label>
                  <input value={form.phoneNumber} onChange={e => setForm(f => ({ ...f, phoneNumber: e.target.value }))} placeholder="+237600000000" required style={inputStyle} onFocus={onFocus} onBlur={onBlur} />
                </div>
                <div>
                  <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Password</label>
                  <input type="password" value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} placeholder="••••••••" required style={inputStyle} onFocus={onFocus} onBlur={onBlur} />
                </div>
                <button type="submit" disabled={loading} className="btn-primary w-full py-4 flex items-center justify-center gap-2 font-bold text-sm mt-2">
                  {loading ? <div className="w-5 h-5 border-2 rounded-full animate-spin" style={{ borderColor: 'rgba(255,255,255,0.3)', borderTopColor: 'white' }} /> : 'Create account →'}
                </button>
              </form>
              <p className="text-xs text-center mt-6" style={{ color: '#404060' }}>
                Already have an account?{' '}
                <button onClick={() => navigate('/login')} className="font-medium transition-colors" style={{ color: '#10b981' }}>Sign in →</button>
              </p>
            </>
          ) : (
            <>
              <div className="mb-6 text-center">
                <div className="w-16 h-16 rounded-2xl mx-auto flex items-center justify-center text-3xl mb-4"
                  style={{ background: 'rgba(16,185,129,0.1)', border: '1px solid rgba(16,185,129,0.2)' }}>✉️</div>
                <h3 className="text-white text-2xl font-bold" style={{ fontFamily: "'Clash Display', sans-serif" }}>Verify your email</h3>
                <p className="text-sm mt-2" style={{ color: '#8080a8' }}>{info}</p>
              </div>
              {error && <div className="px-4 py-3 rounded-xl text-sm mb-4" style={{ background: 'rgba(244,63,94,0.1)', border: '1px solid rgba(244,63,94,0.2)', color: '#f43f5e' }}>{error}</div>}
              <form onSubmit={handleVerify} className="space-y-5">
                <div>
                  <label className="block mb-2 text-xs font-semibold uppercase tracking-widest text-center" style={{ color: '#8080a8' }}>Enter 6-digit code</label>
                  <input value={otp} onChange={e => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    placeholder="000000" inputMode="numeric" maxLength={6} required
                    style={{ ...inputStyle, textAlign: 'center', fontSize: '28px', letterSpacing: '0.5em', fontFamily: "'JetBrains Mono', monospace" }}
                    onFocus={onFocus} onBlur={onBlur} />
                </div>
                <button type="submit" disabled={loading || otp.length < 6} className="btn-primary w-full py-4 flex items-center justify-center gap-2 font-bold text-sm">
                  {loading ? <div className="w-5 h-5 border-2 rounded-full animate-spin" style={{ borderColor: 'rgba(255,255,255,0.3)', borderTopColor: 'white' }} /> : 'Verify & Continue →'}
                </button>
              </form>
              <div className="flex items-center justify-between mt-6 text-xs">
                <button onClick={() => setStep('form')} style={{ color: '#404060' }}>← Back</button>
                <button onClick={resend} style={{ color: '#10b981' }}>Resend code</button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

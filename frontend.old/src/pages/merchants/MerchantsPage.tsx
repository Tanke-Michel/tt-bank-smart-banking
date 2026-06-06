import { useEffect, useState } from 'react';
import { merchantApi } from '../../api/merchants';
import { getErrorMessage } from '../../api/client';

const fmt = (n: number) => new Intl.NumberFormat('fr-CM').format(n ?? 0);

export function MerchantsPage() {
  const [code, setCode] = useState('');
  const [amount, setAmount] = useState('');
  const [desc, setDesc] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');
  const [payments, setPayments] = useState<any[]>([]);

  const load = async () => {
    try { const r = await merchantApi.getMyPayments(0, 10); setPayments(r.data?.content ?? []); } catch {}
  };
  useEffect(() => { load(); }, []);

  const pay = async () => {
    const amt = Number(amount);
    if (!code || !amt) { setErr('Enter merchant code and amount'); return; }
    setBusy(true); setErr(''); setMsg('');
    try {
      await merchantApi.pay(code, amt, desc || 'Payment');
      setMsg(`Paid XAF ${fmt(amt)} to ${code}`);
      setCode(''); setAmount(''); setDesc(''); await load();
    } catch (e) { setErr(getErrorMessage(e)); } finally { setBusy(false); }
  };

  return (
    <div className="space-y-8">
      <div className="animate-fade-up">
        <h1 className="text-3xl font-bold text-white" style={{ fontFamily: "'Clash Display', sans-serif" }}>Merchants</h1>
        <p className="text-sm mt-1" style={{ color: '#8080a8' }}>Pay merchants instantly with a merchant code</p>
      </div>

      <div className="grid grid-cols-3 gap-6">
        <div className="col-span-1 animate-fade-up rounded-2xl p-6" style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <h2 className="text-white font-bold text-lg mb-4" style={{ fontFamily: "'Clash Display', sans-serif" }}>Pay a Merchant</h2>
          {msg && <div className="px-3 py-2 rounded-lg text-xs mb-3" style={{ background: 'rgba(16,185,129,0.1)', color: '#10b981' }}>{msg}</div>}
          {err && <div className="px-3 py-2 rounded-lg text-xs mb-3" style={{ background: 'rgba(244,63,94,0.1)', color: '#f43f5e' }}>{err}</div>}
          <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Merchant Code</label>
          <input value={code} onChange={e => setCode(e.target.value)} placeholder="MERCH-XXXX" className="input-field w-full px-4 py-3 text-sm mb-3" />
          <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Amount</label>
          <input value={amount} onChange={e => setAmount(e.target.value)} type="number" placeholder="XAF" className="input-field w-full px-4 py-3 text-sm mb-3" />
          <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Description</label>
          <input value={desc} onChange={e => setDesc(e.target.value)} placeholder="Optional" className="input-field w-full px-4 py-3 text-sm mb-4" />
          <button onClick={pay} disabled={busy} className="btn-primary w-full py-3 text-sm">{busy ? 'Paying...' : 'Pay Now →'}</button>
        </div>

        <div className="col-span-2 animate-fade-up rounded-2xl p-6" style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <h2 className="text-white font-bold text-lg mb-4" style={{ fontFamily: "'Clash Display', sans-serif" }}>My Payments</h2>
          {payments.length === 0 ? <p className="text-sm py-8 text-center" style={{ color: '#404060' }}>No payments yet</p> : (
            <div className="space-y-1">
              {payments.map(p => (
                <div key={p.id} className="flex items-center gap-4 py-3 px-3 rounded-xl" style={{ borderBottom: '1px solid rgba(255,255,255,0.03)' }}>
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center text-lg" style={{ background: 'rgba(255,255,255,0.04)' }}>◉</div>
                  <div className="flex-1 min-w-0">
                    <p className="text-white text-sm font-semibold truncate">{p.businessName ?? p.merchantCode}</p>
                    <p className="text-xs" style={{ color: '#404060' }}>{p.referenceCode} · {p.status}</p>
                  </div>
                  <p className="font-bold text-sm" style={{ color: '#f0f0f8' }}>-XAF {fmt(p.amount)}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

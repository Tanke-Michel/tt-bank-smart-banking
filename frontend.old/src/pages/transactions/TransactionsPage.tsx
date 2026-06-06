import { useEffect, useState } from 'react';
import { transactionApi } from '../../api/transactions';
import { useAuthStore } from '../../store/auth.store';
import { getErrorMessage } from '../../api/client';

const fmt = (n: number) => new Intl.NumberFormat('fr-CM').format(n ?? 0);

export function TransactionsPage() {
  const { auth } = useAuthStore();
  const [txs, setTxs] = useState<any[]>([]);
  const [tab, setTab] = useState<'all' | 'sent' | 'received'>('all');
  const [recipient, setRecipient] = useState('');
  const [amount, setAmount] = useState('');
  const [desc, setDesc] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');

  const load = async () => {
    try {
      const r = tab === 'sent' ? await transactionApi.getSent(0, 20)
              : tab === 'received' ? await transactionApi.getReceived(0, 20)
              : await transactionApi.getHistory(0, 20);
      setTxs(r.data?.content ?? []);
    } catch { setTxs([]); }
  };
  useEffect(() => { load(); }, [tab]);

  const send = async () => {
    const amt = Number(amount);
    if (!recipient || !amt) { setErr('Enter recipient and amount'); return; }
    setBusy(true); setErr(''); setMsg('');
    try {
      await transactionApi.transfer(recipient, amt, desc || 'Transfer');
      setMsg(`Sent XAF ${fmt(amt)} to ${recipient}`);
      setRecipient(''); setAmount(''); setDesc(''); await load();
    } catch (e) { setErr(getErrorMessage(e)); } finally { setBusy(false); }
  };

  return (
    <div className="space-y-8">
      <div className="animate-fade-up">
        <h1 className="text-3xl font-bold text-white" style={{ fontFamily: "'Clash Display', sans-serif" }}>Transfers</h1>
        <p className="text-sm mt-1" style={{ color: '#8080a8' }}>Send money and view your transaction history</p>
      </div>

      <div className="grid grid-cols-3 gap-6">
        {/* Send money */}
        <div className="col-span-1 animate-fade-up rounded-2xl p-6" style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <h2 className="text-white font-bold text-lg mb-4" style={{ fontFamily: "'Clash Display', sans-serif" }}>Send Money</h2>
          {msg && <div className="px-3 py-2 rounded-lg text-xs mb-3" style={{ background: 'rgba(16,185,129,0.1)', color: '#10b981' }}>{msg}</div>}
          {err && <div className="px-3 py-2 rounded-lg text-xs mb-3" style={{ background: 'rgba(244,63,94,0.1)', color: '#f43f5e' }}>{err}</div>}
          <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Recipient Email</label>
          <input value={recipient} onChange={e => setRecipient(e.target.value)} placeholder="recipient@email.com" className="input-field w-full px-4 py-3 text-sm mb-3" />
          <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Amount</label>
          <input value={amount} onChange={e => setAmount(e.target.value)} type="number" placeholder="XAF" className="input-field w-full px-4 py-3 text-sm mb-3" />
          <label className="block mb-2 text-xs font-semibold uppercase tracking-widest" style={{ color: '#8080a8' }}>Note</label>
          <input value={desc} onChange={e => setDesc(e.target.value)} placeholder="What's it for?" className="input-field w-full px-4 py-3 text-sm mb-4" />
          <button onClick={send} disabled={busy} className="btn-primary w-full py-3 text-sm">{busy ? 'Sending...' : 'Send Transfer →'}</button>
        </div>

        {/* History */}
        <div className="col-span-2 animate-fade-up rounded-2xl p-6" style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-white font-bold text-lg" style={{ fontFamily: "'Clash Display', sans-serif" }}>History</h2>
            <div className="flex gap-1">
              {(['all', 'sent', 'received'] as const).map(t => (
                <button key={t} onClick={() => setTab(t)} className="px-3 py-1.5 rounded-lg text-xs font-semibold capitalize transition-all"
                  style={{ background: tab === t ? 'rgba(16,185,129,0.15)' : 'rgba(255,255,255,0.03)',
                           border: tab === t ? '1px solid rgba(16,185,129,0.3)' : '1px solid rgba(255,255,255,0.06)',
                           color: tab === t ? '#10b981' : '#8080a8' }}>{t}</button>
              ))}
            </div>
          </div>
          {txs.length === 0 ? <p className="text-sm py-8 text-center" style={{ color: '#404060' }}>No transactions</p> : (
            <div className="space-y-1">
              {txs.map(t => {
                const sent = t.senderEmail === auth?.email;
                return (
                  <div key={t.id} className="flex items-center gap-4 py-3 px-3 rounded-xl" style={{ borderBottom: '1px solid rgba(255,255,255,0.03)' }}>
                    <div className="w-10 h-10 rounded-xl flex items-center justify-center text-lg" style={{ background: 'rgba(255,255,255,0.04)' }}>{sent ? '↑' : '↓'}</div>
                    <div className="flex-1 min-w-0">
                      <p className="text-white text-sm font-semibold truncate">{sent ? `To ${t.receiverEmail}` : `From ${t.senderEmail}`}</p>
                      <p className="text-xs" style={{ color: '#404060' }}>{t.referenceCode} · {t.status}</p>
                    </div>
                    <p className="font-bold text-sm" style={{ color: sent ? '#f0f0f8' : '#10b981' }}>{sent ? '-' : '+'}XAF {fmt(t.amount)}</p>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

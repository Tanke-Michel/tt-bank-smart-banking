import { useEffect, useState } from 'react';
import { walletApi } from '../../api/wallet';
import { useAuthStore } from '../../store/auth.store';
import { getErrorMessage } from '../../api/client';

const fmt = (n: number) => new Intl.NumberFormat('fr-CM').format(n ?? 0);

export function WalletPage() {
  const { auth, setWalletNumber } = useAuthStore();
  const [wallet, setWallet] = useState<any>(null);
  const [txs, setTxs] = useState<any[]>([]);
  const [amount, setAmount] = useState('');
  const [desc, setDesc] = useState('');
  const [mode, setMode] = useState<'deposit' | 'withdraw'>('deposit');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');

  const load = async () => {
    try {
      const w = await walletApi.getMyWallet();
      setWallet(w.data);
      setWalletNumber(w.data.walletNumber);
    } catch { setWallet(null); }
    try { const t = await walletApi.getTransactionHistory(0, 10); setTxs(t.data?.content ?? []); } catch {}
  };
  useEffect(() => { load(); }, []);

  const createWallet = async () => {
    setBusy(true); setErr('');
    try { await walletApi.createWallet(auth?.email ?? '', 'XAF'); await load(); setMsg('Wallet created!'); }
    catch (e) { setErr(getErrorMessage(e)); } finally { setBusy(false); }
  };

  const submit = async () => {
    const amt = Number(amount);
    if (!amt || amt <= 0) { setErr('Enter a valid amount'); return; }
    setBusy(true); setErr(''); setMsg('');
    try {
      if (mode === 'deposit') await walletApi.deposit(amt, desc || 'Deposit');
      else await walletApi.withdraw(amt, desc || 'Withdrawal');
      setMsg(`${mode === 'deposit' ? 'Deposited' : 'Withdrew'} XAF ${fmt(amt)}`);
      setAmount(''); setDesc(''); await load();
    } catch (e) { setErr(getErrorMessage(e)); } finally { setBusy(false); }
  };

  if (!wallet) {
    return (
      <div className="max-w-md mx-auto mt-20 text-center">
        <div className="rounded-2xl p-8" style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <div className="text-5xl mb-4">◈</div>
          <h2 className="text-white text-xl font-bold mb-2" style={{ fontFamily: "'Clash Display', sans-serif" }}>No wallet yet</h2>
          <p className="text-sm mb-6" style={{ color: '#8080a8' }}>Create your SmartBank wallet to start sending and receiving money.</p>
          {err && <p className="text-sm mb-4" style={{ color: '#f43f5e' }}>{err}</p>}
          <button onClick={createWallet} disabled={busy} className="btn-primary w-full py-3.5 text-sm">
            {busy ? 'Creating...' : 'Create Wallet'}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="animate-fade-up">
        <h1 className="text-3xl font-bold text-white" style={{ fontFamily: "'Clash Display', sans-serif" }}>Wallet</h1>
        <p className="text-sm mt-1" style={{ color: '#8080a8' }}>Manage your balance and funds</p>
      </div>

      {/* Balance card */}
      <div className="animate-fade-up rounded-2xl p-8 card-shine relative overflow-hidden"
        style={{ background: 'linear-gradient(135deg, #064e3b, #10b981, #059669)', boxShadow: '0 20px 60px rgba(16,185,129,0.25)' }}>
        <p className="text-xs uppercase tracking-widest" style={{ color: 'rgba(255,255,255,0.7)' }}>Total Balance</p>
        <p className="text-white text-4xl font-bold mt-2" style={{ fontFamily: "'Clash Display', sans-serif" }}>XAF {fmt(wallet.balance)}</p>
        <div className="flex justify-between items-end mt-6">
          <div>
            <p className="text-xs" style={{ color: 'rgba(255,255,255,0.6)' }}>Wallet Number</p>
            <p className="text-white font-mono text-sm mt-0.5">{wallet.walletNumber}</p>
          </div>
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full" style={{ background: 'rgba(255,255,255,0.15)', color: 'white' }}>{wallet.status}</span>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-6">
        {/* Deposit / Withdraw */}
        <div className="col-span-1 animate-fade-up rounded-2xl p-6" style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <h2 className="text-white font-bold text-lg mb-4" style={{ fontFamily: "'Clash Display', sans-serif" }}>Add / Remove Funds</h2>
          <div className="flex gap-2 mb-4">
            {(['deposit', 'withdraw'] as const).map(m => (
              <button key={m} onClick={() => setMode(m)} className="flex-1 py-2 rounded-lg text-xs font-semibold transition-all capitalize"
                style={{ background: mode === m ? 'rgba(16,185,129,0.15)' : 'rgba(255,255,255,0.03)',
                         border: mode === m ? '1px solid rgba(16,185,129,0.3)' : '1px solid rgba(255,255,255,0.06)',
                         color: mode === m ? '#10b981' : '#8080a8' }}>{m}</button>
            ))}
          </div>
          {msg && <div className="px-3 py-2 rounded-lg text-xs mb-3" style={{ background: 'rgba(16,185,129,0.1)', color: '#10b981' }}>{msg}</div>}
          {err && <div className="px-3 py-2 rounded-lg text-xs mb-3" style={{ background: 'rgba(244,63,94,0.1)', color: '#f43f5e' }}>{err}</div>}
          <input value={amount} onChange={e => setAmount(e.target.value)} type="number" placeholder="Amount (XAF)"
            className="input-field w-full px-4 py-3 text-sm mb-3" />
          <input value={desc} onChange={e => setDesc(e.target.value)} placeholder="Description (optional)"
            className="input-field w-full px-4 py-3 text-sm mb-4" />
          <button onClick={submit} disabled={busy} className="btn-primary w-full py-3 text-sm">
            {busy ? 'Processing...' : (mode === 'deposit' ? 'Deposit' : 'Withdraw')}
          </button>
        </div>

        {/* Transaction history */}
        <div className="col-span-2 animate-fade-up rounded-2xl p-6" style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <h2 className="text-white font-bold text-lg mb-4" style={{ fontFamily: "'Clash Display', sans-serif" }}>Wallet Activity</h2>
          {txs.length === 0 ? <p className="text-sm py-8 text-center" style={{ color: '#404060' }}>No activity yet</p> : (
            <div className="space-y-1">
              {txs.map(t => {
                const credit = t.type === 'DEPOSIT' || t.type === 'CREDIT';
                return (
                  <div key={t.id} className="flex items-center gap-4 py-3 px-3 rounded-xl" style={{ borderBottom: '1px solid rgba(255,255,255,0.03)' }}>
                    <div className="w-10 h-10 rounded-xl flex items-center justify-center text-lg" style={{ background: 'rgba(255,255,255,0.04)' }}>{credit ? '↓' : '↑'}</div>
                    <div className="flex-1 min-w-0">
                      <p className="text-white text-sm font-semibold truncate">{t.description}</p>
                      <p className="text-xs" style={{ color: '#404060' }}>{t.referenceCode} · {new Date(t.createdAt).toLocaleDateString()}</p>
                    </div>
                    <p className="font-bold text-sm" style={{ color: credit ? '#10b981' : '#f0f0f8' }}>{credit ? '+' : '-'}XAF {fmt(t.amount)}</p>
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

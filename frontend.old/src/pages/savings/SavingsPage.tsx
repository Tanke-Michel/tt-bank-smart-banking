import { useEffect, useState } from 'react';
import { savingsApi } from '../../api/savings';
import { useAuthStore } from '../../store/auth.store';
import { getErrorMessage } from '../../api/client';

const fmt = (n: number) => new Intl.NumberFormat('fr-CM').format(n ?? 0);

export function SavingsPage() {
  const { walletNumber } = useAuthStore();
  const [groups, setGroups] = useState<any[]>([]);
  const [tab, setTab] = useState<'all' | 'mine'>('all');
  const [showCreate, setShowCreate] = useState(false);
  const [err, setErr] = useState('');
  const [msg, setMsg] = useState('');
  const [form, setForm] = useState({ name: '', description: '', contributionAmount: '', payoutCycle: 'MONTHLY', maxMembers: '5', startDate: '' });

  const load = async () => {
    try {
      const r = tab === 'mine' ? await savingsApi.getMyGroups(0) : await savingsApi.listGroups(undefined, 0);
      setGroups(r.data?.content ?? []);
    } catch { setGroups([]); }
  };
  useEffect(() => { load(); }, [tab]);

  const create = async () => {
    setErr(''); setMsg('');
    try {
      await savingsApi.createGroup({
        name: form.name, description: form.description,
        contributionAmount: Number(form.contributionAmount),
        payoutCycle: form.payoutCycle, maxMembers: Number(form.maxMembers),
        startDate: form.startDate || new Date().toISOString().slice(0, 10),
        walletNumber: walletNumber ?? '',
      });
      setMsg('Group created!'); setShowCreate(false); await load();
    } catch (e) { setErr(getErrorMessage(e)); }
  };

  const join = async (id: number) => {
    setErr(''); setMsg('');
    try { await savingsApi.joinGroup(id, walletNumber ?? ''); setMsg('Joined group!'); await load(); }
    catch (e) { setErr(getErrorMessage(e)); }
  };

  return (
    <div className="space-y-8">
      <div className="animate-fade-up flex items-start justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white" style={{ fontFamily: "'Clash Display', sans-serif" }}>Savings Groups</h1>
          <p className="text-sm mt-1" style={{ color: '#8080a8' }}>Rotating savings circles (njangi / tontine)</p>
        </div>
        <button onClick={() => setShowCreate(!showCreate)} className="btn-primary px-5 py-2.5 text-sm">+ Create Group</button>
      </div>

      {msg && <div className="px-4 py-3 rounded-xl text-sm" style={{ background: 'rgba(16,185,129,0.1)', color: '#10b981' }}>{msg}</div>}
      {err && <div className="px-4 py-3 rounded-xl text-sm" style={{ background: 'rgba(244,63,94,0.1)', color: '#f43f5e' }}>{err}</div>}

      {showCreate && (
        <div className="animate-fade-in rounded-2xl p-6" style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <h2 className="text-white font-bold text-lg mb-4" style={{ fontFamily: "'Clash Display', sans-serif" }}>New Savings Group</h2>
          <div className="grid grid-cols-2 gap-4">
            <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="Group name" className="input-field px-4 py-3 text-sm" />
            <input value={form.contributionAmount} onChange={e => setForm({ ...form, contributionAmount: e.target.value })} type="number" placeholder="Contribution (XAF)" className="input-field px-4 py-3 text-sm" />
            <select value={form.payoutCycle} onChange={e => setForm({ ...form, payoutCycle: e.target.value })} className="input-field px-4 py-3 text-sm">
              <option value="WEEKLY">Weekly</option><option value="MONTHLY">Monthly</option>
            </select>
            <input value={form.maxMembers} onChange={e => setForm({ ...form, maxMembers: e.target.value })} type="number" placeholder="Max members" className="input-field px-4 py-3 text-sm" />
            <input value={form.startDate} onChange={e => setForm({ ...form, startDate: e.target.value })} type="date" className="input-field px-4 py-3 text-sm" />
            <input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Description" className="input-field px-4 py-3 text-sm" />
          </div>
          <button onClick={create} className="btn-primary w-full py-3 text-sm mt-4">Create Group</button>
        </div>
      )}

      <div className="flex gap-1">
        {(['all', 'mine'] as const).map(t => (
          <button key={t} onClick={() => setTab(t)} className="px-4 py-2 rounded-lg text-xs font-semibold capitalize transition-all"
            style={{ background: tab === t ? 'rgba(16,185,129,0.15)' : 'rgba(255,255,255,0.03)',
                     border: tab === t ? '1px solid rgba(16,185,129,0.3)' : '1px solid rgba(255,255,255,0.06)',
                     color: tab === t ? '#10b981' : '#8080a8' }}>{t === 'all' ? 'All Groups' : 'My Groups'}</button>
        ))}
      </div>

      <div className="grid grid-cols-3 gap-4">
        {groups.length === 0 ? <p className="col-span-3 text-sm py-8 text-center" style={{ color: '#404060' }}>No savings groups yet</p> :
          groups.map(g => (
            <div key={g.id} className="rounded-2xl p-6 transition-all hover:border-emerald-500/30"
              style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
              <div className="flex justify-between items-start mb-2">
                <p className="text-white font-bold" style={{ fontFamily: "'Clash Display', sans-serif" }}>{g.name}</p>
                <span className="text-xs font-semibold px-2 py-1 rounded-full"
                  style={{ background: 'rgba(245,158,11,0.1)', color: '#f59e0b', border: '1px solid rgba(245,158,11,0.2)' }}>{g.status}</span>
              </div>
              <p className="text-sm mb-3" style={{ color: '#8080a8' }}>{g.description || 'Rotating savings group'}</p>
              <div className="space-y-1 text-xs" style={{ color: '#8080a8' }}>
                <div className="flex justify-between"><span>💰 Contribution</span><span className="text-white">XAF {fmt(g.contributionAmount)}</span></div>
                <div className="flex justify-between"><span>👥 Members</span><span className="text-white">{g.currentMemberCount ?? 0}/{g.maxMembers}</span></div>
                <div className="flex justify-between"><span>🔄 Cycle</span><span className="text-white">{g.payoutCycle}</span></div>
              </div>
              {tab === 'all' && g.status === 'FORMING' && (
                <button onClick={() => join(g.id)} className="btn-primary w-full py-2 text-xs mt-4">Join Group</button>
              )}
            </div>
          ))
        }
      </div>
    </div>
  );
}

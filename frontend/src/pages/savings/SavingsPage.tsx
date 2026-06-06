import { useEffect, useState } from 'react';
import { savingsApi } from '../../api/savings';
import { useAuthStore } from '../../store/auth.store';
import type { GroupResponse, MemberResponse, ContributionResponse, PayoutResponse } from '../../types';
import { formatCurrency, formatDate, formatDateTime } from '../../utils/format';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Spinner } from '../../components/common/Spinner';
import { EmptyState } from '../../components/common/EmptyState';
import { getErrorMessage } from '../../api/client';

export function SavingsPage() {
  const { auth, walletNumber } = useAuthStore();
  const [groups, setGroups] = useState<GroupResponse[]>([]);
  const [myGroups, setMyGroups] = useState<GroupResponse[]>([]);
  const [selected, setSelected] = useState<GroupResponse | null>(null);
  const [members, setMembers] = useState<MemberResponse[]>([]);
  const [contribs, setContribs] = useState<ContributionResponse[]>([]);
  const [payouts, setPayouts] = useState<PayoutResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [showJoin, setShowJoin] = useState<GroupResponse | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');
  const [tab, setTab] = useState<'browse' | 'mine'>('mine');
  const [detailTab, setDetailTab] = useState<'members' | 'contributions' | 'payouts'>('members');

  const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate() + 1);
  const [createForm, setCreateForm] = useState({
    name: '', description: '', contributionAmount: '',
    payoutCycle: 'MONTHLY', maxMembers: '5',
    startDate: tomorrow.toISOString().split('T')[0],
    walletNumber: walletNumber ?? '',
  });

  useEffect(() => { loadAll(); }, []);

  const loadAll = async () => {
    setLoading(true);
    try {
      const [all, mine] = await Promise.all([
        savingsApi.listGroups(undefined, 0),
        savingsApi.getMyGroups(0),
      ]);
      setGroups(all.data.content);
      setMyGroups(mine.data.content);
    } finally { setLoading(false); }
  };

  const openDetail = async (g: GroupResponse) => {
    setSelected(g); setDetailLoading(true);
    try {
      const [m, c, p] = await Promise.all([
        savingsApi.getMembers(g.id),
        savingsApi.getContributions(g.id, 0),
        savingsApi.getPayouts(g.id),
      ]);
      setMembers(m.data);
      setContribs(c.data.content);
      setPayouts(p.data);
    } finally { setDetailLoading(false); }
  };

  const handleCreate = async () => {
    setActionLoading(true); setError('');
    try {
      await savingsApi.createGroup({
        ...createForm,
        contributionAmount: Number(createForm.contributionAmount),
        maxMembers: Number(createForm.maxMembers),
        walletNumber: walletNumber ?? createForm.walletNumber,
      });
      setShowCreate(false); await loadAll();
    } catch (e) { setError(getErrorMessage(e)); }
    finally { setActionLoading(false); }
  };

  const handleJoin = async () => {
    if (!showJoin) return;
    setActionLoading(true); setError('');
    try {
      await savingsApi.joinGroup(showJoin.id, walletNumber ?? '');
      setShowJoin(null); await loadAll();
    } catch (e) { setError(getErrorMessage(e)); }
    finally { setActionLoading(false); }
  };

  const handleContribute = async (g: GroupResponse) => {
    setActionLoading(true); setError('');
    try {
      await savingsApi.contribute(g.id, walletNumber ?? '');
      await openDetail(g);
    } catch (e) { setError(getErrorMessage(e)); }
    finally { setActionLoading(false); }
  };

  const handlePayout = async (g: GroupResponse) => {
    setActionLoading(true); setError('');
    try {
      await savingsApi.processRoundPayout(g.id);
      await openDetail(g);
    } catch (e) { setError(getErrorMessage(e)); }
    finally { setActionLoading(false); }
  };

  const displayGroups = tab === 'mine' ? myGroups : groups;

  if (loading) return <div className="flex h-full items-center justify-center"><Spinner size="lg" /></div>;

  return (
    <div className="p-8 space-y-6 max-w-5xl">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-black text-[var(--text)]">Savings Groups</h1>
        <Button onClick={() => { setError(''); setShowCreate(true); }}>+ Create group</Button>
      </div>

      {/* Tab */}
      <div className="flex gap-1 bg-[var(--card)] rounded-xl p-1 border border-[var(--border)] w-fit">
        {(['mine','browse'] as const).map(t => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-4 py-1.5 rounded-lg text-sm font-medium capitalize transition-all
              ${tab === t ? 'bg-[var(--accent)] text-white' : 'text-[var(--muted)] hover:text-[var(--text)]'}`}>
            {t === 'mine' ? 'My groups' : 'Browse all'}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {displayGroups.length === 0 ? (
          <div className="col-span-2"><EmptyState icon="◎" message={tab === 'mine' ? "You haven't joined any groups yet" : "No groups available"} /></div>
        ) : displayGroups.map(g => (
          <Card key={g.id} className="cursor-pointer hover:border-[var(--accent)] transition-all"
            onClick={() => openDetail(g)}>
            <div className="flex justify-between items-start mb-2">
              <p className="font-bold text-[var(--text)]">{g.name}</p>
              <Badge label={g.status} status={g.status} />
            </div>
            <p className="text-sm text-[var(--muted)] mb-3">{g.description}</p>
            <div className="grid grid-cols-2 gap-2 text-xs text-[var(--muted)]">
              <span>💰 {formatCurrency(g.contributionAmount, g.currency)}/{g.payoutCycle.toLowerCase()}</span>
              <span>👥 {g.currentMemberCount}/{g.maxMembers} members</span>
              <span>🔄 Round {g.currentRound}/{g.totalRounds}</span>
              <span>📅 {formatDate(g.startDate)}</span>
            </div>
            {tab === 'browse' && g.status === 'FORMING' && g.creatorUserId !== auth?.userId && (
              <Button size="sm" className="mt-3" onClick={e => { e.stopPropagation(); setError(''); setShowJoin(g); }}>
                Join group
              </Button>
            )}
          </Card>
        ))}
      </div>

      {/* Detail panel */}
      <Modal open={!!selected} onClose={() => setSelected(null)} title={selected?.name ?? ''}>
        {detailLoading ? (
          <div className="flex justify-center py-8"><Spinner /></div>
        ) : selected && (
          <div className="space-y-4 max-h-[70vh] overflow-y-auto pr-1">
            {error && <p className="text-sm text-red-400">{error}</p>}
            <div className="flex gap-2 flex-wrap">
              <Badge label={selected.status} status={selected.status} />
              <span className="text-sm text-[var(--muted)]">Round {selected.currentRound}/{selected.totalRounds}</span>
              <span className="text-sm text-[var(--muted)]">{formatCurrency(selected.contributionAmount, selected.currency)}/{selected.payoutCycle.toLowerCase()}</span>
            </div>
            {/* Action buttons */}
            <div className="flex gap-2 flex-wrap">
              {selected.status === 'ACTIVE' && (
                <Button size="sm" loading={actionLoading} onClick={() => handleContribute(selected)}>
                  Pay contribution
                </Button>
              )}
              {selected.status === 'ACTIVE' && selected.creatorUserId === auth?.userId && (
                <Button size="sm" variant="secondary" loading={actionLoading} onClick={() => handlePayout(selected)}>
                  Trigger payout
                </Button>
              )}
            </div>
            {/* Detail tabs */}
            <div className="flex gap-1 bg-[var(--bg)] rounded-xl p-1 border border-[var(--border)]">
              {(['members','contributions','payouts'] as const).map(t => (
                <button key={t} onClick={() => setDetailTab(t)}
                  className={`flex-1 py-1.5 rounded-lg text-xs font-medium capitalize transition-all
                    ${detailTab === t ? 'bg-[var(--accent)] text-white' : 'text-[var(--muted)]'}`}>
                  {t}
                </button>
              ))}
            </div>
            {detailTab === 'members' && (
              <div className="space-y-2">
                {members.map(m => (
                  <div key={m.id} className="flex justify-between text-sm">
                    <span className="text-[var(--text)]">#{m.payoutOrder} {m.userEmail}</span>
                    <span className={m.hasReceivedPayout ? 'text-emerald-400' : 'text-[var(--muted)]'}>
                      {m.hasReceivedPayout ? '✓ paid out' : 'pending'}
                    </span>
                  </div>
                ))}
              </div>
            )}
            {detailTab === 'contributions' && (
              <div className="space-y-2">
                {contribs.length === 0 ? <EmptyState message="No contributions yet" /> :
                  contribs.map(c => (
                    <div key={c.id} className="flex justify-between text-sm">
                      <span className="text-[var(--text)]">{c.memberEmail} · R{c.roundNumber}</span>
                      <div className="flex items-center gap-2">
                        <span className="text-[var(--muted)]">{formatCurrency(c.amount, c.currency)}</span>
                        <Badge label={c.status} status={c.status} />
                      </div>
                    </div>
                  ))}
              </div>
            )}
            {detailTab === 'payouts' && (
              <div className="space-y-2">
                {payouts.length === 0 ? <EmptyState message="No payouts yet" /> :
                  payouts.map(p => (
                    <div key={p.id} className="flex justify-between text-sm">
                      <span className="text-[var(--text)]">{p.recipientEmail} · R{p.roundNumber}</span>
                      <div className="flex items-center gap-2">
                        <span className="text-emerald-400">{formatCurrency(p.amount, p.currency)}</span>
                        <Badge label={p.status} status={p.status} />
                      </div>
                    </div>
                  ))}
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* Create modal */}
      <Modal open={showCreate} onClose={() => setShowCreate(false)} title="Create savings group">
        <div className="space-y-3 max-h-[70vh] overflow-y-auto pr-1">
          {error && <p className="text-sm text-red-400">{error}</p>}
          <Input label="Group name" value={createForm.name}
            onChange={e => setCreateForm(f => ({ ...f, name: e.target.value }))} />
          <Input label="Description (optional)" value={createForm.description}
            onChange={e => setCreateForm(f => ({ ...f, description: e.target.value }))} />
          <Input label="Contribution amount" type="number" min="100"
            value={createForm.contributionAmount}
            onChange={e => setCreateForm(f => ({ ...f, contributionAmount: e.target.value }))} />
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[var(--text-secondary)]">Payout cycle</label>
            <select className="rounded-xl border border-[var(--border)] bg-[var(--input)] px-4 py-2.5 text-[var(--text)]"
              value={createForm.payoutCycle} onChange={e => setCreateForm(f => ({ ...f, payoutCycle: e.target.value }))}>
              {['WEEKLY','BIWEEKLY','MONTHLY'].map(c => <option key={c}>{c}</option>)}
            </select>
          </div>
          <Input label="Max members (= total rounds)" type="number" min="2" max="50"
            value={createForm.maxMembers}
            onChange={e => setCreateForm(f => ({ ...f, maxMembers: e.target.value }))} />
          <Input label="Start date" type="date" value={createForm.startDate}
            onChange={e => setCreateForm(f => ({ ...f, startDate: e.target.value }))} />
          <Input label="Your wallet number" value={walletNumber ?? createForm.walletNumber} readOnly onChange={() => {}} />
          <Button onClick={handleCreate} loading={actionLoading} className="w-full">Create group</Button>
        </div>
      </Modal>

      {/* Join modal */}
      <Modal open={!!showJoin} onClose={() => setShowJoin(null)} title={`Join: ${showJoin?.name}`}>
        <div className="space-y-4">
          {error && <p className="text-sm text-red-400">{error}</p>}
          <p className="text-sm text-[var(--muted)]">
            Monthly contribution: <strong>{formatCurrency(showJoin?.contributionAmount ?? 0, showJoin?.currency)}</strong>
          </p>
          <p className="text-sm text-[var(--muted)]">
            Wallet to use: <strong>{walletNumber ?? '—'}</strong>
          </p>
          <Button onClick={handleJoin} loading={actionLoading} className="w-full">Join group</Button>
        </div>
      </Modal>
    </div>
  );
}

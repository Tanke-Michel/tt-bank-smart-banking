import { useEffect, useState } from 'react';
import { walletApi } from '../../api/wallet';
import { useAuthStore } from '../../store/auth.store';
import type { WalletResponse, WalletTransactionResponse, PagedResponse } from '../../types';
import { formatCurrency, formatDateTime } from '../../utils/format';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Spinner } from '../../components/common/Spinner';
import { EmptyState } from '../../components/common/EmptyState';
import { getErrorMessage } from '../../api/client';

export function WalletPage() {
  const { walletNumber, setWalletNumber } = useAuthStore();
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [txns, setTxns] = useState<WalletTransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [noWallet, setNoWallet] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [showDeposit, setShowDeposit] = useState(false);
  const [showWithdraw, setShowWithdraw] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState('');

  const [createForm, setCreateForm] = useState({ phoneNumber: '', currency: 'XAF' });
  const [depositForm, setDepositForm] = useState({ amount: '', description: '' });
  const [withdrawForm, setWithdrawForm] = useState({ amount: '', description: '' });

  const loadWallet = async () => {
    try {
      const { data } = await walletApi.getMyWallet();
      setWallet(data);
      if (!walletNumber) setWalletNumber(data.walletNumber);
      const t = await walletApi.getTransactionHistory(0, 20);
      setTxns(t.data.content);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } }).response?.status;
      if (status === 404) setNoWallet(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadWallet(); }, []);

  const handleCreate = async () => {
    setActionLoading(true); setActionError('');
    try {
      const { data } = await walletApi.createWallet(createForm.phoneNumber, createForm.currency);
      setWallet(data); setWalletNumber(data.walletNumber);
      setNoWallet(false); setShowCreate(false);
    } catch (err) { setActionError(getErrorMessage(err)); }
    finally { setActionLoading(false); }
  };

  const handleDeposit = async () => {
    setActionLoading(true); setActionError('');
    try {
      await walletApi.deposit(Number(depositForm.amount), depositForm.description || 'Deposit');
      setShowDeposit(false);
      await loadWallet();
    } catch (err) { setActionError(getErrorMessage(err)); }
    finally { setActionLoading(false); }
  };

  const handleWithdraw = async () => {
    setActionLoading(true); setActionError('');
    try {
      await walletApi.withdraw(Number(withdrawForm.amount), withdrawForm.description || 'Withdrawal');
      setShowWithdraw(false);
      await loadWallet();
    } catch (err) { setActionError(getErrorMessage(err)); }
    finally { setActionLoading(false); }
  };

  if (loading) return <div className="flex h-full items-center justify-center"><Spinner size="lg" /></div>;

  return (
    <div className="p-8 space-y-8 max-w-3xl">
      <h1 className="text-2xl font-black text-[var(--text)]">My Wallet</h1>

      {noWallet ? (
        <Card className="text-center py-12 space-y-4">
          <div className="text-5xl">◈</div>
          <h2 className="font-bold text-[var(--text)]">No wallet yet</h2>
          <p className="text-sm text-[var(--muted)]">Create your digital wallet to get started.</p>
          <Button onClick={() => setShowCreate(true)}>Create wallet</Button>
        </Card>
      ) : wallet && (
        <>
          <Card className="bg-gradient-to-br from-[var(--accent)] to-[var(--accent-2)] border-0 text-white">
            <p className="text-sm opacity-75">Available balance</p>
            <p className="text-4xl font-black mt-1">{formatCurrency(wallet.balance, wallet.currency)}</p>
            <div className="flex flex-wrap gap-4 mt-4 text-xs opacity-75">
              <span>{wallet.walletNumber}</span>
              <span>{wallet.phoneNumber}</span>
              <Badge label={wallet.status} status={wallet.status} />
            </div>
          </Card>

          <div className="flex gap-3">
            <Button onClick={() => { setActionError(''); setShowDeposit(true); }}>+ Deposit</Button>
            <Button variant="secondary" onClick={() => { setActionError(''); setShowWithdraw(true); }}>Withdraw</Button>
          </div>

          <div>
            <h2 className="font-bold text-[var(--text)] mb-4">Wallet transactions</h2>
            <Card className="p-0 divide-y divide-[var(--border)] overflow-hidden">
              {txns.length === 0 ? (
                <EmptyState message="No wallet transactions yet" />
              ) : txns.map(t => (
                <div key={t.id} className="flex items-center justify-between px-5 py-3.5">
                  <div>
                    <p className="text-sm font-medium text-[var(--text)]">{t.description || t.type}</p>
                    <p className="text-xs text-[var(--muted)]">{formatDateTime(t.createdAt)} • {t.referenceCode}</p>
                  </div>
                  <div className="text-right">
                    <p className={`text-sm font-bold ${['DEPOSIT','CREDIT'].includes(t.type) ? 'text-emerald-400' : 'text-red-400'}`}>
                      {['DEPOSIT','CREDIT'].includes(t.type) ? '+' : '−'}{formatCurrency(t.amount, t.currency)}
                    </p>
                    <p className="text-xs text-[var(--muted)]">Balance: {formatCurrency(t.balanceAfter, t.currency)}</p>
                  </div>
                </div>
              ))}
            </Card>
          </div>
        </>
      )}

      {/* Create wallet modal */}
      <Modal open={showCreate} onClose={() => setShowCreate(false)} title="Create wallet">
        <div className="space-y-4">
          {actionError && <p className="text-sm text-red-400">{actionError}</p>}
          <Input label="Phone number" placeholder="+237600000000" value={createForm.phoneNumber}
            onChange={e => setCreateForm(f => ({ ...f, phoneNumber: e.target.value }))} />
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[var(--text-secondary)]">Currency</label>
            <select className="rounded-xl border border-[var(--border)] bg-[var(--input)] px-4 py-2.5 text-[var(--text)]"
              value={createForm.currency} onChange={e => setCreateForm(f => ({ ...f, currency: e.target.value }))}>
              {['XAF','USD','EUR','GBP'].map(c => <option key={c}>{c}</option>)}
            </select>
          </div>
          <Button onClick={handleCreate} loading={actionLoading} className="w-full">Create wallet</Button>
        </div>
      </Modal>

      {/* Deposit modal */}
      <Modal open={showDeposit} onClose={() => setShowDeposit(false)} title="Deposit funds">
        <div className="space-y-4">
          {actionError && <p className="text-sm text-red-400">{actionError}</p>}
          <Input label="Amount" type="number" min="1" placeholder="5000" value={depositForm.amount}
            onChange={e => setDepositForm(f => ({ ...f, amount: e.target.value }))} />
          <Input label="Description (optional)" placeholder="Mobile money" value={depositForm.description}
            onChange={e => setDepositForm(f => ({ ...f, description: e.target.value }))} />
          <Button onClick={handleDeposit} loading={actionLoading} className="w-full">Deposit</Button>
        </div>
      </Modal>

      {/* Withdraw modal */}
      <Modal open={showWithdraw} onClose={() => setShowWithdraw(false)} title="Withdraw funds">
        <div className="space-y-4">
          {actionError && <p className="text-sm text-red-400">{actionError}</p>}
          <Input label="Amount" type="number" min="1" placeholder="5000" value={withdrawForm.amount}
            onChange={e => setWithdrawForm(f => ({ ...f, amount: e.target.value }))} />
          <Input label="Description (optional)" placeholder="ATM" value={withdrawForm.description}
            onChange={e => setWithdrawForm(f => ({ ...f, description: e.target.value }))} />
          <Button onClick={handleWithdraw} loading={actionLoading} className="w-full">Withdraw</Button>
        </div>
      </Modal>
    </div>
  );
}

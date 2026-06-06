import { useEffect, useState } from 'react';
import { usePolling } from '../../hooks/usePolling';
import { Link } from 'react-router-dom';
import { walletApi } from '../../api/wallet';
import { transactionApi } from '../../api/transactions';
import { useAuthStore } from '../../store/auth.store';
import type { WalletResponse, TransactionResponse, PagedResponse } from '../../types';
import { formatCurrency, formatDateTime, statusColour } from '../../utils/format';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Spinner } from '../../components/common/Spinner';
import { Button } from '../../components/common/Button';

export function DashboardPage() {
  const { auth, walletNumber, setWalletNumber } = useAuthStore();
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [txns, setTxns] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [noWallet, setNoWallet] = useState(false);

  const loadDashboard = async () => {
    try {
      const { data } = await walletApi.getMyWallet();
      setWallet(data);
      setNoWallet(false);
      if (!walletNumber) setWalletNumber(data.walletNumber);
      const t = await transactionApi.getHistory(0, 5);
      setTxns(t.data.content);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } }).response?.status;
      if (status === 404) setNoWallet(true);
    } finally {
      setLoading(false);
    }
  };

  // real-time: refresh every 5s (and on tab focus)
  usePolling(loadDashboard, 5000);

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (noWallet) {
    return (
      <div className="p-8 flex flex-col items-center justify-center gap-6 h-full">
        <div className="text-6xl">◈</div>
        <h2 className="text-2xl font-bold text-[var(--text)]">No wallet yet</h2>
        <p className="text-[var(--muted)] text-center max-w-sm">
          Create your digital wallet to start sending money, paying merchants, and joining savings groups.
        </p>
        <Link to="/wallet">
          <Button size="lg">Create my wallet</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="p-8 space-y-8 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-black text-[var(--text)]">
          Good day, {auth?.fullName?.split(' ')[0]} 👋
        </h1>
        <p className="text-sm text-[var(--muted)] mt-1">Here's your financial overview</p>
      </div>

      {/* Balance card */}
      {wallet && (
        <Card className="bg-gradient-to-br from-[var(--accent)] to-[var(--accent-2)] border-0 text-white">
          <p className="text-sm opacity-75 font-medium">Available balance</p>
          <p className="text-4xl font-black mt-1">
            {formatCurrency(wallet.balance, wallet.currency)}
          </p>
          <div className="flex items-center gap-4 mt-4 text-xs opacity-75">
            <span>{wallet.walletNumber}</span>
            <span>•</span>
            <span>{wallet.currency}</span>
            <span>•</span>
            <Badge label={wallet.status} status={wallet.status} />
          </div>
        </Card>
      )}

      {/* Quick actions */}
      <div className="grid grid-cols-3 gap-4">
        {[
          { to: '/transactions', icon: '⇄', label: 'Transfer' },
          { to: '/merchants',    icon: '◉', label: 'Pay' },
          { to: '/savings',      icon: '◎', label: 'Save' },
        ].map(a => (
          <Link key={a.to} to={a.to}>
            <Card className="text-center hover:border-[var(--accent)] hover:bg-[var(--accent)]/5 cursor-pointer transition-all">
              <div className="text-2xl mb-2">{a.icon}</div>
              <p className="text-sm font-semibold text-[var(--text)]">{a.label}</p>
            </Card>
          </Link>
        ))}
      </div>

      {/* Recent transactions */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-bold text-[var(--text)]">Recent transfers</h2>
          <Link to="/transactions" className="text-xs text-[var(--accent)] hover:underline">View all</Link>
        </div>
        <Card className="divide-y divide-[var(--border)] p-0 overflow-hidden">
          {txns.length === 0 ? (
            <p className="text-center py-8 text-sm text-[var(--muted)]">No transfers yet</p>
          ) : txns.map(t => (
            <div key={t.id} className="flex items-center justify-between px-5 py-3.5">
              <div className="min-w-0">
                <p className="text-sm font-medium text-[var(--text)] truncate">
                  {t.senderEmail === auth?.email ? `→ ${t.receiverEmail}` : `← ${t.senderEmail}`}
                </p>
                <p className="text-xs text-[var(--muted)]">{formatDateTime(t.createdAt)}</p>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                <span className={`text-sm font-bold ${t.senderEmail === auth?.email ? 'text-red-400' : 'text-emerald-400'}`}>
                  {t.senderEmail === auth?.email ? '−' : '+'}{formatCurrency(t.amount, t.currency)}
                </span>
                <Badge label={t.status} status={t.status} />
              </div>
            </div>
          ))}
        </Card>
      </div>
    </div>
  );
}

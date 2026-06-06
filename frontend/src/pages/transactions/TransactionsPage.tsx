import { useEffect, useState } from 'react';
import { transactionApi } from '../../api/transactions';
import { walletApi } from '../../api/wallet';
import { useAuthStore } from '../../store/auth.store';
import type { TransactionResponse, PagedResponse } from '../../types';
import { formatCurrency, formatDateTime } from '../../utils/format';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Spinner } from '../../components/common/Spinner';
import { EmptyState } from '../../components/common/EmptyState';
import { getErrorMessage } from '../../api/client';

export function TransactionsPage() {
  const { auth, walletNumber, setWalletNumber } = useAuthStore();
  const [txns, setTxns] = useState<TransactionResponse[]>([]);
  const [meta, setMeta] = useState<Omit<PagedResponse<unknown>, 'content'>>({ page: 0, size: 20, totalElements: 0, totalPages: 0, last: true });
  const [loading, setLoading] = useState(true);
  const [showTransfer, setShowTransfer] = useState(false);
  const [transferForm, setTransferForm] = useState({ recipientEmail: '', amount: '', description: '' });
  const [transferLoading, setTransferLoading] = useState(false);
  const [transferError, setTransferError] = useState('');
  const [tab, setTab] = useState<'all' | 'sent' | 'received'>('all');

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const fn = tab === 'sent'     ? transactionApi.getSent
               : tab === 'received' ? transactionApi.getReceived
               : transactionApi.getHistory;
      const { data } = await fn(page, 20);
      setTxns(data.content);
      setMeta({ page: data.page, size: data.size, totalElements: data.totalElements, totalPages: data.totalPages, last: data.last });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(0); }, [tab]);

  // Ensure the sender's wallet number is known so the X-Auth-User-Wallet
  // header is always attached to transfer requests.
  useEffect(() => {
    if (!walletNumber) {
      walletApi.getMyWallet()
        .then(r => setWalletNumber(r.data.walletNumber))
        .catch(() => {});
    }
  }, []);

  const handleTransfer = async () => {
    setTransferLoading(true); setTransferError('');
    try {
      // Make sure our wallet number is loaded (needed for the transfer header)
      if (!walletNumber) {
        try { const w = await walletApi.getMyWallet(); setWalletNumber(w.data.walletNumber); }
        catch { throw new Error('Please create your wallet first (go to the Wallet page).'); }
      }
      await transactionApi.transfer(transferForm.recipientEmail, Number(transferForm.amount), transferForm.description || undefined);
      setShowTransfer(false);
      setTransferForm({ recipientEmail: '', amount: '', description: '' });
      await load(0);
    } catch (err) { setTransferError(getErrorMessage(err)); }
    finally { setTransferLoading(false); }
  };

  return (
    <div className="p-8 space-y-6 max-w-3xl">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-black text-[var(--text)]">Transfers</h1>
        <Button onClick={() => { setTransferError(''); setShowTransfer(true); }}>
          + Send money
        </Button>
      </div>

      {/* Tab bar */}
      <div className="flex gap-1 bg-[var(--card)] rounded-xl p-1 border border-[var(--border)] w-fit">
        {(['all', 'sent', 'received'] as const).map(t => (
          <button key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-1.5 rounded-lg text-sm font-medium capitalize transition-all
              ${tab === t ? 'bg-[var(--accent)] text-white' : 'text-[var(--muted)] hover:text-[var(--text)]'}`}>
            {t}
          </button>
        ))}
      </div>

      <Card className="p-0 divide-y divide-[var(--border)] overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-12"><Spinner /></div>
        ) : txns.length === 0 ? (
          <EmptyState icon="⇄" message="No transfers yet" />
        ) : txns.map(t => {
          const isOut = t.senderEmail === auth?.email;
          return (
            <div key={t.id} className="flex items-center justify-between px-5 py-4">
              <div className="flex items-center gap-3 min-w-0">
                <div className={`h-9 w-9 rounded-full flex items-center justify-center text-sm shrink-0
                  ${isOut ? 'bg-red-500/15 text-red-400' : 'bg-emerald-500/15 text-emerald-400'}`}>
                  {isOut ? '↑' : '↓'}
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-[var(--text)] truncate">
                    {isOut ? t.receiverEmail : t.senderEmail}
                  </p>
                  <p className="text-xs text-[var(--muted)]">{formatDateTime(t.createdAt)}</p>
                  {t.description && <p className="text-xs text-[var(--muted)] truncate">{t.description}</p>}
                </div>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                <span className={`text-sm font-bold ${isOut ? 'text-red-400' : 'text-emerald-400'}`}>
                  {isOut ? '−' : '+'}{formatCurrency(t.amount, t.currency)}
                </span>
                <Badge label={t.status} status={t.status} />
              </div>
            </div>
          );
        })}
      </Card>

      {/* Pagination */}
      {!loading && meta.totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <Button size="sm" variant="secondary" disabled={meta.page === 0} onClick={() => load(meta.page - 1)}>← Prev</Button>
          <span className="text-sm text-[var(--muted)] flex items-center px-3">
            {meta.page + 1} / {meta.totalPages}
          </span>
          <Button size="sm" variant="secondary" disabled={meta.last} onClick={() => load(meta.page + 1)}>Next →</Button>
        </div>
      )}

      {/* Transfer modal */}
      <Modal open={showTransfer} onClose={() => setShowTransfer(false)} title="Send money">
        <div className="space-y-4">
          {transferError && <p className="text-sm text-red-400 bg-red-500/10 rounded-lg px-3 py-2">{transferError}</p>}
          <Input label="Recipient email" type="email" placeholder="recipient@example.com"
            value={transferForm.recipientEmail}
            onChange={e => setTransferForm(f => ({ ...f, recipientEmail: e.target.value }))} />
          <Input label="Amount" type="number" min="1" placeholder="5000"
            value={transferForm.amount}
            onChange={e => setTransferForm(f => ({ ...f, amount: e.target.value }))} />
          <Input label="Note (optional)" placeholder="Rent for November"
            value={transferForm.description}
            onChange={e => setTransferForm(f => ({ ...f, description: e.target.value }))} />
          <Button onClick={handleTransfer} loading={transferLoading} className="w-full">Send</Button>
        </div>
      </Modal>
    </div>
  );
}

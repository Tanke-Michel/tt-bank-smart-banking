import { useEffect, useState } from 'react';
import { merchantApi } from '../../api/merchants';
import { useAuthStore } from '../../store/auth.store';
import type { MerchantResponse, MerchantPaymentResponse, MerchantDashboardResponse } from '../../types';
import { formatCurrency, formatDateTime } from '../../utils/format';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Spinner } from '../../components/common/Spinner';
import { EmptyState } from '../../components/common/EmptyState';
import { getErrorMessage } from '../../api/client';

const CATEGORIES = ['RETAIL','FOOD_AND_BEVERAGE','HEALTHCARE','EDUCATION','TRANSPORT',
  'TELECOMMUNICATIONS','UTILITIES','ENTERTAINMENT','SERVICES','OTHER'];

export function MerchantsPage() {
  const { walletNumber } = useAuthStore();
  const [merchant, setMerchant] = useState<MerchantResponse | null>(null);
  const [dashboard, setDashboard] = useState<MerchantDashboardResponse | null>(null);
  const [payments, setPayments] = useState<MerchantPaymentResponse[]>([]);
  const [myPayments, setMyPayments] = useState<MerchantPaymentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showRegister, setShowRegister] = useState(false);
  const [showPay, setShowPay] = useState(false);
  const [regForm, setRegForm] = useState({ businessName:'', businessEmail:'', businessPhone:'',
    businessAddress:'', businessCategory:'RETAIL', description:'', walletNumber: walletNumber ?? '' });
  const [payForm, setPayForm] = useState({ merchantCode:'', amount:'' , description:'' });
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');
  const [tab, setTab] = useState<'dashboard' | 'received' | 'made'>('dashboard');

  useEffect(() => { load(); }, []);

  const load = async () => {
    setLoading(true);
    try {
      const { data: m } = await merchantApi.getMyMerchant();
      setMerchant(m);
      if (m.status === 'ACTIVE') {
        const [d, p] = await Promise.all([
          merchantApi.getDashboard(),
          merchantApi.getMerchantPayments(0, 10),
        ]);
        setDashboard(d.data);
        setPayments(p.data.content);
      }
    } catch { /* no merchant yet */ }
    // always load own payments made
    try {
      const { data } = await merchantApi.getMyPayments(0, 10);
      setMyPayments(data.content);
    } catch { /**/ }
    setLoading(false);
  };

  const handleRegister = async () => {
    setActionLoading(true); setError('');
    try {
      const { data } = await merchantApi.register({ ...regForm, walletNumber: walletNumber ?? regForm.walletNumber });
      setMerchant(data); setShowRegister(false);
    } catch (e) { setError(getErrorMessage(e)); }
    finally { setActionLoading(false); }
  };

  const handlePay = async () => {
    setActionLoading(true); setError('');
    try {
      await merchantApi.pay(payForm.merchantCode, Number(payForm.amount), payForm.description || undefined);
      setShowPay(false); setPayForm({ merchantCode:'', amount:'', description:'' });
      await load();
    } catch (e) { setError(getErrorMessage(e)); }
    finally { setActionLoading(false); }
  };

  if (loading) return <div className="flex h-full items-center justify-center"><Spinner size="lg" /></div>;

  return (
    <div className="p-8 space-y-6 max-w-4xl">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-black text-[var(--text)]">Merchants</h1>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => { setError(''); setShowPay(true); }}>Pay merchant</Button>
          {!merchant && <Button onClick={() => setShowRegister(true)}>Register merchant</Button>}
        </div>
      </div>

      {merchant && (
        <>
          {/* Merchant card */}
          <Card>
            <div className="flex items-start justify-between">
              <div>
                <p className="font-bold text-[var(--text)] text-lg">{merchant.businessName}</p>
                <p className="text-sm text-[var(--muted)]">{merchant.merchantCode}</p>
                <p className="text-xs text-[var(--muted)] mt-1">{merchant.businessCategory}</p>
              </div>
              <Badge label={merchant.status} status={merchant.status} />
            </div>
          </Card>

          {merchant.status === 'ACTIVE' && (
            <>
              <div className="flex gap-1 bg-[var(--card)] rounded-xl p-1 border border-[var(--border)] w-fit">
                {(['dashboard','received','made'] as const).map(t => (
                  <button key={t} onClick={() => setTab(t)}
                    className={`px-4 py-1.5 rounded-lg text-sm font-medium capitalize transition-all
                      ${tab === t ? 'bg-[var(--accent)] text-white' : 'text-[var(--muted)] hover:text-[var(--text)]'}`}>
                    {t}
                  </button>
                ))}
              </div>

              {tab === 'dashboard' && dashboard && (
                <div className="grid grid-cols-3 gap-4">
                  {[
                    { label:'Today revenue',   value: formatCurrency(dashboard.todayRevenue),  count: dashboard.todayTransactionCount },
                    { label:'Month revenue',   value: formatCurrency(dashboard.monthRevenue),  count: dashboard.monthTransactionCount },
                    { label:'Total revenue',   value: formatCurrency(dashboard.totalRevenue),  count: dashboard.totalTransactionCount },
                  ].map(s => (
                    <Card key={s.label}>
                      <p className="text-xs text-[var(--muted)]">{s.label}</p>
                      <p className="text-xl font-black text-[var(--text)] mt-1">{s.value}</p>
                      <p className="text-xs text-[var(--muted)] mt-1">{s.count} payments</p>
                    </Card>
                  ))}
                </div>
              )}

              {tab === 'received' && (
                <Card className="p-0 divide-y divide-[var(--border)] overflow-hidden">
                  {payments.length === 0 ? <EmptyState message="No payments received yet" /> :
                    payments.map(p => (
                      <div key={p.id} className="flex justify-between px-5 py-3.5">
                        <div>
                          <p className="text-sm font-medium text-[var(--text)]">{p.customerEmail}</p>
                          <p className="text-xs text-[var(--muted)]">{formatDateTime(p.createdAt)}</p>
                        </div>
                        <div className="text-right">
                          <p className="text-sm font-bold text-emerald-400">+{formatCurrency(p.amount, p.currency)}</p>
                          <Badge label={p.status} status={p.status} />
                        </div>
                      </div>
                    ))}
                </Card>
              )}
            </>
          )}

          {merchant.status === 'PENDING' && (
            <Card>
              <p className="text-sm text-[var(--muted)]">
                Your merchant account is under review. You'll be notified when approved.
              </p>
            </Card>
          )}
        </>
      )}

      {/* My payments made */}
      {tab !== 'received' && (
        <div>
          <h2 className="font-bold text-[var(--text)] mb-3">My payments to merchants</h2>
          <Card className="p-0 divide-y divide-[var(--border)] overflow-hidden">
            {myPayments.length === 0 ? <EmptyState message="No payments made yet" /> :
              myPayments.map(p => (
                <div key={p.id} className="flex justify-between px-5 py-3.5">
                  <div>
                    <p className="text-sm font-medium text-[var(--text)]">{p.businessName}</p>
                    <p className="text-xs text-[var(--muted)]">{formatDateTime(p.createdAt)}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-bold text-red-400">−{formatCurrency(p.amount, p.currency)}</p>
                    <Badge label={p.status} status={p.status} />
                  </div>
                </div>
              ))}
          </Card>
        </div>
      )}

      {/* Register modal */}
      <Modal open={showRegister} onClose={() => setShowRegister(false)} title="Register merchant">
        <div className="space-y-3 max-h-[70vh] overflow-y-auto pr-1">
          {error && <p className="text-sm text-red-400">{error}</p>}
          <Input label="Business name" value={regForm.businessName} onChange={e => setRegForm(f => ({ ...f, businessName: e.target.value }))} />
          <Input label="Business email" type="email" value={regForm.businessEmail} onChange={e => setRegForm(f => ({ ...f, businessEmail: e.target.value }))} />
          <Input label="Business phone" value={regForm.businessPhone} onChange={e => setRegForm(f => ({ ...f, businessPhone: e.target.value }))} />
          <Input label="Business address" value={regForm.businessAddress} onChange={e => setRegForm(f => ({ ...f, businessAddress: e.target.value }))} />
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[var(--text-secondary)]">Category</label>
            <select className="rounded-xl border border-[var(--border)] bg-[var(--input)] px-4 py-2.5 text-[var(--text)]"
              value={regForm.businessCategory} onChange={e => setRegForm(f => ({ ...f, businessCategory: e.target.value }))}>
              {CATEGORIES.map(c => <option key={c}>{c}</option>)}
            </select>
          </div>
          <Input label="Description (optional)" value={regForm.description} onChange={e => setRegForm(f => ({ ...f, description: e.target.value }))} />
          <Input label="Wallet number" value={walletNumber ?? regForm.walletNumber} readOnly onChange={() => {}} />
          <Button onClick={handleRegister} loading={actionLoading} className="w-full">Register</Button>
        </div>
      </Modal>

      {/* Pay modal */}
      <Modal open={showPay} onClose={() => setShowPay(false)} title="Pay merchant">
        <div className="space-y-4">
          {error && <p className="text-sm text-red-400">{error}</p>}
          <Input label="Merchant code (from QR)" placeholder="MCH-20240101-ABCD1234"
            value={payForm.merchantCode} onChange={e => setPayForm(f => ({ ...f, merchantCode: e.target.value }))} />
          <Input label="Amount" type="number" min="1" placeholder="5000"
            value={payForm.amount} onChange={e => setPayForm(f => ({ ...f, amount: e.target.value }))} />
          <Input label="Note (optional)" value={payForm.description}
            onChange={e => setPayForm(f => ({ ...f, description: e.target.value }))} />
          <Button onClick={handlePay} loading={actionLoading} className="w-full">Pay</Button>
        </div>
      </Modal>
    </div>
  );
}

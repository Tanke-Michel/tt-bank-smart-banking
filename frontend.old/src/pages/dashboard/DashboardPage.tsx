import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/auth.store';
import { walletApi } from '../../api/wallet';
import { transactionApi } from '../../api/transactions';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";

const fmt = (n: number) => new Intl.NumberFormat('fr-CM').format(n);

const chartData = [
  { month: 'Jan', income: 450000, expense: 320000 },
  { month: 'Feb', income: 520000, expense: 280000 },
  { month: 'Mar', income: 480000, expense: 350000 },
  { month: 'Apr', income: 600000, expense: 290000 },
  { month: 'May', income: 750000, expense: 425000 },
  { month: 'Jun', income: 580000, expense: 310000 },
];

const spendingCategories = [
  { name: 'Transfers',  value: 45, color: '#10b981' },
  { name: 'Merchants',  value: 22, color: '#8b5cf6' },
  { name: 'Savings',    value: 17, color: '#f59e0b' },
  { name: 'Other',      value: 16, color: '#f43f5e' },
];

const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload?.length) return (
    <div className="rounded-xl px-4 py-3 text-xs"
      style={{ background: 'rgba(13,13,36,0.95)', border: '1px solid rgba(255,255,255,0.1)', backdropFilter: 'blur(20px)' }}>
      <p className="font-semibold mb-2" style={{ color: '#8080a8' }}>{label}</p>
      {payload.map((p: any) => (
        <p key={p.name} className="font-bold" style={{ color: p.color }}>
          {p.name === 'income' ? '+' : '-'}XAF {fmt(p.value)}
        </p>
      ))}
    </div>
  );
  return null;
};

export default function DashboardPage() {
  const { auth } = useAuthStore();
  const navigate = useNavigate();
  const [wallet, setWallet] = useState<any>(null);
  const [txs, setTxs] = useState<any[]>([]);
  const firstName = (auth?.fullName ?? 'User').split(' ')[0];
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';

  useEffect(() => {
    walletApi.getMyWallet().then(r => setWallet(r.data)).catch(() => {});
    transactionApi.getHistory(0, 5).then(r => setTxs(r.data?.content ?? [])).catch(() => {});
  }, []);

  const stats = [
    { title: 'Total Balance',   value: `XAF ${fmt(wallet?.balance ?? 0)}`,        icon: '💰', color: 'green',  trend: 12.5 },
    { title: 'Daily Limit',     value: `XAF ${fmt(wallet?.dailyTransactionLimit ?? 0)}`, icon: '🏦', color: 'violet', trend: 0 },
    { title: 'Wallet Number',   value: wallet?.walletNumber ?? '—',                icon: '◈',  color: 'amber',  trend: null },
    { title: 'Account Status',  value: wallet?.status ?? 'ACTIVE',                 icon: '✓',  color: 'green',  trend: null },
  ];

  const colorMap: Record<string, { border: string; glow: string; text: string }> = {
    green:  { border: 'rgba(16,185,129,0.2)',  glow: 'rgba(16,185,129,0.15)',  text: '#10b981' },
    violet: { border: 'rgba(139,92,246,0.2)',  glow: 'rgba(139,92,246,0.15)',  text: '#8b5cf6' },
    amber:  { border: 'rgba(245,158,11,0.2)',  glow: 'rgba(245,158,11,0.15)',  text: '#f59e0b' },
    rose:   { border: 'rgba(244,63,94,0.2)',   glow: 'rgba(244,63,94,0.15)',   text: '#f43f5e' },
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="animate-fade-up flex items-start justify-between">
        <div>
          <p className="text-sm" style={{ color: '#8080a8' }}>{greeting} 👋</p>
          <h1 className="text-3xl font-bold text-white mt-0.5" style={{ fontFamily: "'Clash Display', sans-serif" }}>{firstName}</h1>
        </div>
        <div className="px-4 py-2 rounded-xl text-xs font-bold"
          style={{ background: 'rgba(16,185,129,0.1)', border: '1px solid rgba(16,185,129,0.2)', color: '#10b981' }}>
          {auth?.role} ⭐
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
        {stats.map((s, i) => {
          const c = colorMap[s.color];
          return (
            <div key={s.title} className="animate-fade-up rounded-2xl p-6 relative overflow-hidden transition-all duration-300 cursor-default"
              style={{ animationDelay: `${i * 0.1}s`, background: 'linear-gradient(135deg, rgba(13,13,36,0.9) 0%, rgba(8,8,24,0.9) 100%)',
                       border: `1px solid ${c.border}`, boxShadow: `0 4px 40px ${c.glow}` }}>
              <div className="absolute top-0 right-0 w-24 h-24 rounded-full opacity-20"
                style={{ background: `radial-gradient(circle, ${c.text}, transparent)`, transform: 'translate(30%, -30%)' }} />
              <div className="flex items-start justify-between relative z-10">
                <div>
                  <p className="text-xs font-semibold tracking-widest uppercase mb-2" style={{ color: '#8080a8' }}>{s.title}</p>
                  <p className="font-bold text-xl leading-tight" style={{ fontFamily: "'Clash Display', sans-serif", color: c.text }}>{s.value}</p>
                </div>
                <div className="w-11 h-11 rounded-xl flex items-center justify-center text-xl flex-shrink-0"
                  style={{ background: c.glow, border: `1px solid ${c.border}` }}>{s.icon}</div>
              </div>
              {s.trend != null && (
                <div className="mt-4 flex items-center gap-2">
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full"
                    style={{ color: s.trend >= 0 ? '#10b981' : '#f43f5e', background: s.trend >= 0 ? 'rgba(16,185,129,0.1)' : 'rgba(244,63,94,0.1)' }}>
                    {s.trend >= 0 ? '+' : ''}{s.trend}%
                  </span>
                  <span className="text-xs" style={{ color: '#404060' }}>vs last month</span>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Main grid */}
      <div className="grid grid-cols-3 gap-6">
        {/* Left col */}
        <div className="col-span-2 space-y-6">
          {/* Chart */}
          <div className="animate-fade-up rounded-2xl p-6 stagger-2"
            style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-white font-bold text-lg" style={{ fontFamily: "'Clash Display', sans-serif" }}>Cash Flow</h2>
                <p className="text-xs mt-0.5" style={{ color: '#8080a8' }}>Jan – Jun 2025</p>
              </div>
              <div className="flex items-center gap-4 text-xs">
                <div className="flex items-center gap-1.5"><div className="w-3 h-3 rounded-full" style={{ background: '#10b981' }} /><span style={{ color: '#8080a8' }}>Income</span></div>
                <div className="flex items-center gap-1.5"><div className="w-3 h-3 rounded-full" style={{ background: '#8b5cf6' }} /><span style={{ color: '#8080a8' }}>Expense</span></div>
              </div>
            </div>
            <ResponsiveContainer width="100%" height={200}>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="incomeGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="expenseGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="month" tick={{ fill: '#505070', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis hide />
                <Tooltip content={<CustomTooltip />} />
                <Area type="monotone" dataKey="income" stroke="#10b981" strokeWidth={2} fill="url(#incomeGrad)" dot={false} />
                <Area type="monotone" dataKey="expense" stroke="#8b5cf6" strokeWidth={2} fill="url(#expenseGrad)" dot={false} />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          {/* Recent transactions */}
          <div className="animate-fade-up rounded-2xl p-6 stagger-3"
            style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-white text-lg font-bold" style={{ fontFamily: "'Clash Display', sans-serif" }}>Recent Transactions</h2>
              <button onClick={() => navigate('/transactions')} className="text-sm font-medium transition-colors" style={{ color: '#10b981' }}>View All →</button>
            </div>
            {txs.length === 0 ? (
              <div className="py-8 text-center" style={{ color: '#404060' }}>No transactions yet</div>
            ) : (
              <div className="space-y-1">
                {txs.map((tx: any, i: number) => {
                  const isCredit = tx.type === 'CREDIT';
                  return (
                    <div key={tx.id} className="flex items-center gap-4 py-3.5 px-4 rounded-xl transition-all duration-200 cursor-pointer"
                      style={{ animationDelay: `${i * 0.05}s` }}
                      onMouseEnter={e => (e.currentTarget.style.background = 'rgba(255,255,255,0.03)')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
                      <div className="w-10 h-10 rounded-xl flex items-center justify-center text-lg flex-shrink-0"
                        style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)' }}>
                        {isCredit ? '📈' : '📉'}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-white text-sm font-semibold truncate">{tx.description ?? (isCredit ? 'Credit' : 'Debit')}</p>
                        <p className="text-xs mt-0.5" style={{ color: '#404060' }}>{new Date(tx.createdAt ?? Date.now()).toLocaleDateString()}</p>
                      </div>
                      <div className="text-right flex-shrink-0">
                        <p className="font-bold text-sm" style={{ color: isCredit ? '#10b981' : '#f0f0f8' }}>
                          {isCredit ? '+' : '-'}XAF {fmt(tx.amount ?? 0)}
                        </p>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Right col */}
        <div className="space-y-6">
          {/* Quick actions */}
          <div className="animate-fade-up rounded-2xl p-6 stagger-1"
            style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <h2 className="text-white text-lg font-bold mb-5" style={{ fontFamily: "'Clash Display', sans-serif" }}>Quick Actions</h2>
            <div className="grid grid-cols-2 gap-3">
              {[
                { icon: '↑', label: 'Send',      color: 'green',  to: '/transactions' },
                { icon: '↓', label: 'Receive',   color: 'violet', to: '/wallet' },
                { icon: '+', label: 'Top Up',    color: 'amber',  to: '/wallet' },
                { icon: '◉', label: 'Merchants', color: 'rose',   to: '/merchants' },
              ].map(({ icon, label, color, to }) => {
                const c = colorMap[color];
                return (
                  <button key={label} onClick={() => navigate(to)}
                    className="flex flex-col items-center gap-2 py-4 rounded-xl transition-all hover:scale-105 active:scale-95"
                    style={{ background: c.glow, border: `1px solid ${c.border}` }}>
                    <span className="text-xl font-bold" style={{ color: c.text }}>{icon}</span>
                    <span className="text-xs font-semibold" style={{ color: c.text }}>{label}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Spending breakdown */}
          <div className="animate-fade-up rounded-2xl p-6 stagger-2"
            style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <h2 className="text-white text-lg font-bold mb-4" style={{ fontFamily: "'Clash Display', sans-serif" }}>Spending</h2>
            <div className="flex justify-center mb-4">
              <PieChart width={120} height={120}>
                <Pie data={spendingCategories} cx={55} cy={55} innerRadius={35} outerRadius={55} paddingAngle={3} dataKey="value">
                  {spendingCategories.map((entry, i) => <Cell key={i} fill={entry.color} />)}
                </Pie>
              </PieChart>
            </div>
            <div className="space-y-2">
              {spendingCategories.map(cat => (
                <div key={cat.name} className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: cat.color }} />
                  <span className="text-xs flex-1" style={{ color: '#8080a8' }}>{cat.name}</span>
                  <span className="text-xs" style={{ color: '#404060' }}>{cat.value}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

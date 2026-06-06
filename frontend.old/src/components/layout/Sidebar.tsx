import { NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/auth.store';
import { authApi } from '../../api/auth';
import { useState } from 'react';

const nav = [
  { to: '/dashboard',    emoji: '⬡', label: 'Dashboard' },
  { to: '/wallet',       emoji: '◈', label: 'Wallet' },
  { to: '/transactions', emoji: '⇄', label: 'Transfers' },
  { to: '/merchants',    emoji: '◉', label: 'Merchants' },
  { to: '/savings',      emoji: '◎', label: 'Savings' },
];
const adminNav = [{ to: '/admin', emoji: '▣', label: 'Audit Log' }];

export function Sidebar() {
  const { auth, logout } = useAuthStore();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);

  const handleLogout = async () => {
    try { if (auth?.refreshToken) await authApi.logout(auth.refreshToken); } finally {
      logout(); navigate('/login');
    }
  };

  const items = auth?.role === 'ADMIN' ? [...nav, ...adminNav] : nav;
  const initials = (auth?.fullName ?? '?').split(' ').map((n: string) => n[0]).join('').slice(0, 2).toUpperCase();

  return (
    <aside className={`fixed left-0 top-0 h-full z-50 flex flex-col transition-all duration-300 ${collapsed ? 'w-20' : 'w-64'}`}
      style={{ background: 'linear-gradient(180deg, #0a0a1e 0%, #060614 100%)', borderRight: '1px solid rgba(255,255,255,0.05)' }}>

      {/* Logo */}
      <div className="flex items-center gap-3 px-5 py-6" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
        <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 text-white font-black text-lg"
          style={{ background: 'linear-gradient(135deg, #10b981, #059669)', boxShadow: '0 0 20px rgba(16,185,129,0.3)' }}>S</div>
        {!collapsed && (
          <div>
            <p className="font-bold text-white leading-tight" style={{ fontFamily: "'Clash Display', sans-serif" }}>SmartBank</p>
            <p className="text-xs tracking-widest font-semibold" style={{ color: '#10b981' }}>DIGITAL WALLET</p>
          </div>
        )}
      </div>

      {/* Toggle */}
      <button onClick={() => setCollapsed(!collapsed)}
        className="absolute -right-3 top-8 w-6 h-6 rounded-full flex items-center justify-center z-10 text-white text-xs font-bold transition-all hover:scale-110"
        style={{ background: '#10b981', boxShadow: '0 0 10px rgba(16,185,129,0.4)' }}>
        {collapsed ? '›' : '‹'}
      </button>

      {/* Nav */}
      <nav className="flex-1 px-3 py-6 space-y-1 overflow-y-auto">
        {items.map((item) => (
          <NavLink key={item.to} to={item.to}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-medium transition-all duration-200 relative group
               ${collapsed ? 'justify-center' : ''}
               ${isActive ? 'text-emerald-400' : 'text-gray-500 hover:text-gray-300'}`}
            style={({ isActive }) => isActive ? {
              background: 'linear-gradient(135deg, rgba(16,185,129,0.12) 0%, rgba(16,185,129,0.04) 100%)',
              borderLeft: collapsed ? 'none' : '2px solid #10b981',
            } : {}}>
            <span className="text-base w-5 text-center flex-shrink-0">{item.emoji}</span>
            {!collapsed && <span>{item.label}</span>}
            {collapsed && (
              <div className="absolute left-full ml-2 px-2 py-1 rounded-md text-xs text-white opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50"
                style={{ background: '#1a1a3e' }}>{item.label}</div>
            )}
          </NavLink>
        ))}
      </nav>

      {/* Footer */}
      <div className="px-3 py-4" style={{ borderTop: '1px solid rgba(255,255,255,0.05)' }}>
        <div className={`flex items-center gap-3 px-2 py-2 rounded-xl ${collapsed ? 'justify-center' : ''}`}>
          <div className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 text-white font-bold text-sm"
            style={{ background: 'linear-gradient(135deg, #8b5cf6, #6d28d9)' }}>{initials}</div>
          {!collapsed && (
            <>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-white truncate">{(auth?.fullName ?? '').split(' ')[0]}</p>
                <p className="text-xs truncate" style={{ color: '#8080a8' }}>{auth?.role}</p>
              </div>
              <button onClick={handleLogout} className="text-gray-600 hover:text-red-400 transition-colors text-xs">✕</button>
            </>
          )}
        </div>
      </div>
    </aside>
  );
}

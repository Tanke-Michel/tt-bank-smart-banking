import { NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/auth.store';
import { authApi } from '../../api/auth';

const nav = [
  { to: '/dashboard',     icon: '⬡', label: 'Dashboard' },
  { to: '/wallet',        icon: '◈', label: 'Wallet' },
  { to: '/transactions',  icon: '⇄', label: 'Transfers' },
  { to: '/merchants',     icon: '◉', label: 'Merchants' },
  { to: '/savings',       icon: '◎', label: 'Savings' },
];

const adminNav = [
  { to: '/admin',         icon: '▣', label: 'Audit Log' },
];

export function Sidebar() {
  const { auth, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      if (auth?.refreshToken) await authApi.logout(auth.refreshToken);
    } finally {
      logout();
      navigate('/login');
    }
  };

  const items = auth?.role === 'ADMIN'
    ? [...nav, ...adminNav]
    : nav;

  return (
    <aside className="flex h-screen w-64 flex-col bg-[var(--sidebar)] border-r border-[var(--border)]">
      {/* Logo */}
      <div className="flex items-center gap-3 px-6 py-6 border-b border-[var(--border)]">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[var(--accent)] text-white font-black text-lg">S</div>
        <div>
          <p className="font-bold text-[var(--text)] leading-tight text-sm">SmartBank</p>
          <p className="text-xs text-[var(--muted)]">Digital Wallet</p>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all
               ${isActive
                 ? 'bg-[var(--accent)]/15 text-[var(--accent)]'
                 : 'text-[var(--muted)] hover:bg-[var(--border)] hover:text-[var(--text)]'}`
            }
          >
            <span className="text-base w-5 text-center">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>

      {/* User footer */}
      <div className="border-t border-[var(--border)] p-4">
        <div className="flex items-center gap-3 mb-3">
          <div className="h-8 w-8 rounded-full bg-[var(--accent)]/20 flex items-center justify-center text-[var(--accent)] text-sm font-bold">
            {auth?.fullName?.[0]?.toUpperCase() ?? '?'}
          </div>
          <div className="min-w-0">
            <p className="text-sm font-medium text-[var(--text)] truncate">{auth?.fullName}</p>
            <p className="text-xs text-[var(--muted)] truncate">{auth?.role}</p>
          </div>
        </div>
        <button
          onClick={handleLogout}
          className="w-full text-left text-xs text-[var(--muted)] hover:text-red-400 transition-colors px-1"
        >
          Sign out →
        </button>
      </div>
    </aside>
  );
}

import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';

export function AppLayout() {
  return (
    <div className="min-h-screen flex" style={{ background: '#04040e' }}>
      <div className="orb w-96 h-96 opacity-20"
        style={{ background: 'radial-gradient(circle, #10b981, transparent)', top: '-10%', right: '15%' }} />
      <div className="orb w-80 h-80 opacity-10"
        style={{ background: 'radial-gradient(circle, #8b5cf6, transparent)', bottom: '10%', left: '20%' }} />
      <Sidebar />
      <main className="flex-1 ml-64 transition-all duration-300 min-h-screen relative z-10">
        <div className="max-w-7xl mx-auto px-8 py-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

import { useEffect, useState } from 'react';
import { auditApi } from '../../api/audit';

export function AdminPage() {
  const [events, setEvents] = useState<any[]>([]);
  const [stats, setStats] = useState<any>(null);
  const [domain, setDomain] = useState('');

  const load = async () => {
    try { const r = await auditApi.searchEvents({ domain: domain || undefined, page: 0, size: 50 }); setEvents(r.data?.content ?? []); } catch { setEvents([]); }
    try { const s = await auditApi.getStats(); setStats(s.data); } catch {}
  };
  useEffect(() => { load(); }, [domain]);

  const domains = ['', 'AUTH', 'WALLET', 'TRANSACTION', 'MERCHANT', 'SAVINGS'];

  return (
    <div className="space-y-8">
      <div className="animate-fade-up">
        <h1 className="text-3xl font-bold text-white" style={{ fontFamily: "'Clash Display', sans-serif" }}>Audit Log</h1>
        <p className="text-sm mt-1" style={{ color: '#8080a8' }}>System-wide event audit trail (admin only)</p>
      </div>

      {stats && (
        <div className="grid grid-cols-4 gap-4">
          {[
            { label: 'Total Events', value: stats.totalEvents ?? 0, color: '#10b981' },
            { label: 'Today', value: stats.eventsToday ?? 0, color: '#8b5cf6' },
            { label: 'This Week', value: stats.eventsThisWeek ?? 0, color: '#f59e0b' },
            { label: 'Domains', value: stats.domainCounts ? Object.keys(stats.domainCounts).length : 0, color: '#f43f5e' },
          ].map(s => (
            <div key={s.label} className="rounded-2xl p-5" style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
              <p className="text-xs uppercase tracking-widest mb-1" style={{ color: '#8080a8' }}>{s.label}</p>
              <p className="text-2xl font-bold" style={{ fontFamily: "'Clash Display', sans-serif", color: s.color }}>{s.value}</p>
            </div>
          ))}
        </div>
      )}

      <div className="flex gap-2 flex-wrap">
        {domains.map(d => (
          <button key={d || 'all'} onClick={() => setDomain(d)} className="px-3 py-1.5 rounded-full text-xs font-medium transition-all"
            style={{ background: domain === d ? 'rgba(16,185,129,0.15)' : 'rgba(255,255,255,0.03)',
                     border: domain === d ? '1px solid rgba(16,185,129,0.3)' : '1px solid rgba(255,255,255,0.05)',
                     color: domain === d ? '#10b981' : '#8080a8' }}>{d || 'All'}</button>
        ))}
      </div>

      <div className="rounded-2xl overflow-hidden" style={{ background: 'rgba(13,13,36,0.8)', border: '1px solid rgba(255,255,255,0.06)' }}>
        <div className="grid px-6 py-3 text-xs font-semibold uppercase tracking-widest" style={{ gridTemplateColumns: '1fr 1fr 2fr 1fr', borderBottom: '1px solid rgba(255,255,255,0.04)', color: '#404060' }}>
          <span>Domain</span><span>Event</span><span>Actor</span><span className="text-right">Time</span>
        </div>
        {events.length === 0 ? <p className="py-12 text-center text-sm" style={{ color: '#404060' }}>No audit events</p> :
          events.map(e => (
            <div key={e.id} className="grid px-6 py-3.5 items-center" style={{ gridTemplateColumns: '1fr 1fr 2fr 1fr', borderBottom: '1px solid rgba(255,255,255,0.03)' }}>
              <span className="text-xs font-medium px-2 py-1 rounded-full w-fit" style={{ background: 'rgba(139,92,246,0.1)', color: '#8b5cf6' }}>{e.domain}</span>
              <span className="text-white text-sm">{e.eventType}</span>
              <span className="text-sm truncate" style={{ color: '#8080a8' }}>{e.actorEmail ?? '—'}</span>
              <span className="text-right text-xs" style={{ color: '#404060' }}>{e.createdAt ? new Date(e.createdAt).toLocaleString() : '—'}</span>
            </div>
          ))
        }
      </div>
    </div>
  );
}

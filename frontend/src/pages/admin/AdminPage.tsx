import { useEffect, useState } from 'react';
import { auditApi } from '../../api/audit';
import { useAuthStore } from '../../store/auth.store';
import { Navigate } from 'react-router-dom';
import type { AuditEventResponse, AuditStatsResponse } from '../../types';
import { formatDateTime } from '../../utils/format';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Spinner } from '../../components/common/Spinner';
import { EmptyState } from '../../components/common/EmptyState';
import { Badge } from '../../components/common/Badge';

const DOMAINS = ['WALLET','TRANSACTION','MERCHANT','SAVINGS'];

export function AdminPage() {
  const { auth } = useAuthStore();
  const [stats, setStats] = useState<AuditStatsResponse | null>(null);
  const [events, setEvents] = useState<AuditEventResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<AuditEventResponse | null>(null);
  const [filters, setFilters] = useState({ domain: '', eventType: '', actorEmail: '' });

  if (auth?.role !== 'ADMIN') return <Navigate to="/dashboard" replace />;

  useEffect(() => { loadStats(); search(0); }, []);

  const loadStats = async () => {
    try {
      const { data } = await auditApi.getStats();
      setStats(data);
    } catch { /**/ }
  };

  const search = async (page = 0) => {
    setSearching(true);
    try {
      const { data } = await auditApi.searchEvents({
        domain: filters.domain || undefined,
        eventType: filters.eventType || undefined,
        actorEmail: filters.actorEmail || undefined,
        page, size: 50,
      });
      setEvents(data.content);
      setTotalPages(data.totalPages);
      setCurrentPage(data.page);
    } finally {
      setSearching(false);
      setLoading(false);
    }
  };

  const domainBadge: Record<string, string> = {
    WALLET: 'bg-blue-500/15 text-blue-400',
    TRANSACTION: 'bg-violet-500/15 text-violet-400',
    MERCHANT: 'bg-amber-500/15 text-amber-400',
    SAVINGS: 'bg-emerald-500/15 text-emerald-400',
  };

  return (
    <div className="p-8 space-y-8 max-w-6xl">
      <h1 className="text-2xl font-black text-[var(--text)]">Audit Log</h1>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Card>
            <p className="text-xs text-[var(--muted)]">Total events</p>
            <p className="text-2xl font-black text-[var(--text)]">{stats.totalEvents.toLocaleString()}</p>
          </Card>
          <Card>
            <p className="text-xs text-[var(--muted)]">Today</p>
            <p className="text-2xl font-black text-[var(--text)]">{stats.todayEvents.toLocaleString()}</p>
          </Card>
          {Object.entries(stats.byDomain).slice(0, 2).map(([d, n]) => (
            <Card key={d}>
              <p className="text-xs text-[var(--muted)]">{d}</p>
              <p className="text-2xl font-black text-[var(--text)]">{Number(n).toLocaleString()}</p>
            </Card>
          ))}
        </div>
      )}

      {/* Filters */}
      <Card>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[var(--text-secondary)]">Domain</label>
            <select className="rounded-xl border border-[var(--border)] bg-[var(--input)] px-4 py-2.5 text-[var(--text)]"
              value={filters.domain} onChange={e => setFilters(f => ({ ...f, domain: e.target.value }))}>
              <option value="">All domains</option>
              {DOMAINS.map(d => <option key={d}>{d}</option>)}
            </select>
          </div>
          <Input label="Event type" placeholder="TRANSACTION_COMPLETED"
            value={filters.eventType} onChange={e => setFilters(f => ({ ...f, eventType: e.target.value }))} />
          <Input label="Actor email" placeholder="user@example.com" type="email"
            value={filters.actorEmail} onChange={e => setFilters(f => ({ ...f, actorEmail: e.target.value }))} />
        </div>
        <div className="flex gap-2 mt-4">
          <Button onClick={() => search(0)} loading={searching}>Search</Button>
          <Button variant="secondary" onClick={() => { setFilters({ domain:'', eventType:'', actorEmail:'' }); search(0); }}>
            Reset
          </Button>
        </div>
      </Card>

      {/* Event list */}
      <Card className="p-0 overflow-hidden">
        {loading || searching ? (
          <div className="flex justify-center py-12"><Spinner /></div>
        ) : events.length === 0 ? (
          <EmptyState icon="▣" message="No audit events found" />
        ) : (
          <div className="divide-y divide-[var(--border)]">
            {events.map(e => (
              <div key={e.id}
                className="flex items-start gap-4 px-5 py-3.5 hover:bg-[var(--border)]/30 cursor-pointer transition-colors"
                onClick={() => setSelectedEvent(e)}>
                <div className="shrink-0 pt-0.5">
                  <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-semibold ${domainBadge[e.eventDomain] ?? ''}`}>
                    {e.eventDomain}
                  </span>
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-[var(--text)] truncate">{e.summary}</p>
                  <p className="text-xs text-[var(--muted)]">{e.actorEmail} · {formatDateTime(e.receivedAt)}</p>
                </div>
                <div className="shrink-0 text-xs text-[var(--muted)] font-mono">{e.referenceCode ?? `#${e.id}`}</div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <Button size="sm" variant="secondary" disabled={currentPage === 0} onClick={() => search(currentPage - 1)}>← Prev</Button>
          <span className="text-sm text-[var(--muted)] flex items-center px-3">{currentPage + 1} / {totalPages}</span>
          <Button size="sm" variant="secondary" disabled={currentPage >= totalPages - 1} onClick={() => search(currentPage + 1)}>Next →</Button>
        </div>
      )}

      {/* Event detail modal */}
      {selectedEvent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setSelectedEvent(null)} />
          <div className="relative z-10 w-full max-w-2xl rounded-2xl bg-[var(--card)] border border-[var(--border)] p-6 shadow-2xl max-h-[80vh] overflow-y-auto">
            <div className="flex justify-between items-start mb-4">
              <h2 className="font-bold text-[var(--text)]">Event #{selectedEvent.id}</h2>
              <button onClick={() => setSelectedEvent(null)} className="text-[var(--muted)] hover:text-[var(--text)] text-xl">✕</button>
            </div>
            <div className="space-y-3 text-sm">
              <div className="grid grid-cols-2 gap-3">
                <div><p className="text-[var(--muted)] text-xs">Domain</p><p className="text-[var(--text)] font-medium">{selectedEvent.eventDomain}</p></div>
                <div><p className="text-[var(--muted)] text-xs">Type</p><p className="text-[var(--text)] font-medium">{selectedEvent.eventType}</p></div>
                <div><p className="text-[var(--muted)] text-xs">Reference</p><p className="text-[var(--text)] font-mono text-xs">{selectedEvent.referenceCode ?? '—'}</p></div>
                <div><p className="text-[var(--muted)] text-xs">Actor</p><p className="text-[var(--text)]">{selectedEvent.actorEmail ?? '—'}</p></div>
                <div><p className="text-[var(--muted)] text-xs">Received at</p><p className="text-[var(--text)]">{formatDateTime(selectedEvent.receivedAt)}</p></div>
              </div>
              <div>
                <p className="text-[var(--muted)] text-xs mb-1">Summary</p>
                <p className="text-[var(--text)]">{selectedEvent.summary}</p>
              </div>
              <div>
                <p className="text-[var(--muted)] text-xs mb-1">Raw payload</p>
                <pre className="rounded-xl bg-[var(--bg)] p-3 text-xs text-[var(--muted)] overflow-x-auto whitespace-pre-wrap">
                  {(() => { try { return JSON.stringify(JSON.parse(selectedEvent.rawPayload), null, 2); } catch { return selectedEvent.rawPayload; } })()}
                </pre>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

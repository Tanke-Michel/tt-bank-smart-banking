import { api } from './client';
import type { AuditEventResponse, AuditStatsResponse, PagedResponse } from '../types';

export const auditApi = {
  searchEvents: (params: {
    domain?: string; eventType?: string; actorEmail?: string;
    from?: string; to?: string; page?: number; size?: number;
  }) => {
    const q = new URLSearchParams();
    if (params.domain)     q.set('domain', params.domain);
    if (params.eventType)  q.set('eventType', params.eventType);
    if (params.actorEmail) q.set('actorEmail', params.actorEmail);
    if (params.from)       q.set('from', params.from);
    if (params.to)         q.set('to', params.to);
    q.set('page',  String(params.page  ?? 0));
    q.set('size',  String(params.size  ?? 50));
    return api.get<PagedResponse<AuditEventResponse>>(`/api/v1/admin/audit/events?${q}`);
  },

  getById: (id: number) =>
    api.get<AuditEventResponse>(`/api/v1/admin/audit/events/${id}`),

  getByRef: (ref: string) =>
    api.get<AuditEventResponse>(`/api/v1/admin/audit/ref/${ref}`),

  getByDomain: (domain: string, page = 0) =>
    api.get<PagedResponse<AuditEventResponse>>(
      `/api/v1/admin/audit/domain/${domain}?page=${page}`
    ),

  getByActor: (email: string, page = 0) =>
    api.get<PagedResponse<AuditEventResponse>>(
      `/api/v1/admin/audit/actor/${encodeURIComponent(email)}?page=${page}`
    ),

  getStats: () =>
    api.get<AuditStatsResponse>('/api/v1/admin/audit/stats'),
};

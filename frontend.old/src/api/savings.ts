import { api } from './client';
import type {
  GroupResponse, MemberResponse, ContributionResponse,
  PayoutResponse, PagedResponse
} from '../types';

export const savingsApi = {
  createGroup: (data: {
    name: string; description?: string; contributionAmount: number;
    payoutCycle: string; maxMembers: number; startDate: string; walletNumber: string;
  }) => api.post<GroupResponse>('/api/v1/savings/groups', data),

  listGroups: (status?: string, page = 0) =>
    api.get<PagedResponse<GroupResponse>>(
      `/api/v1/savings/groups?page=${page}${status ? `&status=${status}` : ''}`
    ),

  getMyGroups: (page = 0) =>
    api.get<PagedResponse<GroupResponse>>(`/api/v1/savings/groups/mine?page=${page}`),

  getGroup: (groupId: number) =>
    api.get<GroupResponse>(`/api/v1/savings/groups/${groupId}`),

  joinGroup: (groupId: number, walletNumber: string) =>
    api.post<MemberResponse>(`/api/v1/savings/groups/${groupId}/join`, { walletNumber }),

  getMembers: (groupId: number) =>
    api.get<MemberResponse[]>(`/api/v1/savings/groups/${groupId}/members`),

  contribute: (groupId: number, walletNumber: string) =>
    api.post<ContributionResponse>(`/api/v1/savings/groups/${groupId}/contribute`, {
      walletNumber,
    }),

  processRoundPayout: (groupId: number) =>
    api.post<PayoutResponse>(`/api/v1/savings/groups/${groupId}/payout`),

  getContributions: (groupId: number, page = 0) =>
    api.get<PagedResponse<ContributionResponse>>(
      `/api/v1/savings/groups/${groupId}/contributions?page=${page}`
    ),

  getMyContributions: (groupId: number, page = 0) =>
    api.get<PagedResponse<ContributionResponse>>(
      `/api/v1/savings/groups/${groupId}/contributions/mine?page=${page}`
    ),

  getPayouts: (groupId: number) =>
    api.get<PayoutResponse[]>(`/api/v1/savings/groups/${groupId}/payouts`),
};

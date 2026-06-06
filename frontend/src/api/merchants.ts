import { api } from './client';
import type {
  MerchantResponse, MerchantPaymentResponse,
  MerchantDashboardResponse, PagedResponse
} from '../types';

export const merchantApi = {
  register: (data: {
    businessName: string; businessEmail: string; businessPhone: string;
    businessAddress: string; businessCategory: string;
    description?: string; walletNumber: string;
  }) => api.post<MerchantResponse>('/api/v1/merchants/register', data),

  getMyMerchant: () =>
    api.get<MerchantResponse>('/api/v1/merchants/me'),

  getQrCode: () =>
    api.get<{ message: string }>('/api/v1/merchants/me/qr-code'),

  getDashboard: () =>
    api.get<MerchantDashboardResponse>('/api/v1/merchants/me/dashboard'),

  getMerchantPayments: (page = 0, size = 20) =>
    api.get<PagedResponse<MerchantPaymentResponse>>(
      `/api/v1/merchants/me/payments?page=${page}&size=${size}`
    ),

  getByCode: (merchantCode: string) =>
    api.get<MerchantResponse>(`/api/v1/merchants/${merchantCode}`),

  pay: (merchantCode: string, amount: number, description?: string) =>
    api.post<MerchantPaymentResponse>('/api/v1/merchants/pay', {
      merchantCode, amount, description,
    }),

  getMyPayments: (page = 0, size = 20) =>
    api.get<PagedResponse<MerchantPaymentResponse>>(
      `/api/v1/merchants/my-payments?page=${page}&size=${size}`
    ),

  // Admin
  listAll: (status?: string, page = 0) =>
    api.get<PagedResponse<MerchantResponse>>(
      `/api/v1/merchants/admin/all?page=${page}${status ? `&status=${status}` : ''}`
    ),

  updateStatus: (merchantId: number, status: string, reason?: string) =>
    api.put<MerchantResponse>(`/api/v1/merchants/admin/${merchantId}/status`, {
      status, reason,
    }),
};

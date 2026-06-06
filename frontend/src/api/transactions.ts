import { api } from './client';
import type { TransactionResponse, PagedResponse } from '../types';

export const transactionApi = {
  transfer: (recipientEmail: string, amount: number, description?: string) =>
    api.post<TransactionResponse>('/api/v1/transactions/transfer', {
      recipientEmail, amount, description,
    }),

  getHistory: (page = 0, size = 20) =>
    api.get<PagedResponse<TransactionResponse>>(
      `/api/v1/transactions/history?page=${page}&size=${size}`
    ),

  getSent: (page = 0, size = 20) =>
    api.get<PagedResponse<TransactionResponse>>(
      `/api/v1/transactions/sent?page=${page}&size=${size}`
    ),

  getReceived: (page = 0, size = 20) =>
    api.get<PagedResponse<TransactionResponse>>(
      `/api/v1/transactions/received?page=${page}&size=${size}`
    ),

  getByReference: (ref: string) =>
    api.get<TransactionResponse>(`/api/v1/transactions/${ref}`),
};

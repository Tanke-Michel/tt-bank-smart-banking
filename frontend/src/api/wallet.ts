import { api } from './client';
import type { WalletResponse, WalletTransactionResponse, PagedResponse } from '../types';

export const walletApi = {
  createWallet: (phoneNumber: string, currency: string) =>
    api.post<WalletResponse>('/api/v1/wallet/create', { phoneNumber, currency }),

  getMyWallet: () =>
    api.get<WalletResponse>('/api/v1/wallet/me'),

  deposit: (amount: number, description: string) =>
    api.post<WalletTransactionResponse>('/api/v1/wallet/deposit', { amount, description }),

  withdraw: (amount: number, description: string) =>
    api.post<WalletTransactionResponse>('/api/v1/wallet/withdraw', { amount, description }),

  getTransactionHistory: (page = 0, size = 20) =>
    api.get<PagedResponse<WalletTransactionResponse>>(
      `/api/v1/wallet/transactions?page=${page}&size=${size}`
    ),
};

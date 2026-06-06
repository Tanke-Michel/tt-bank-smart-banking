import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ErrorResponse } from '../types';

const BASE_URL = '';

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// ---------- Request interceptor: attach JWT + wallet header ----------
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const raw = localStorage.getItem('auth');
  if (raw) {
    const { accessToken, walletNumber } = JSON.parse(raw) as {
      accessToken: string;
      walletNumber?: string;
    };
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    if (walletNumber) {
      config.headers['X-Auth-User-Wallet'] = walletNumber;
    }
  }
  return config;
});

// ---------- Response interceptor: normalise errors ----------
api.interceptors.response.use(
  (res) => res,
  (error: AxiosError<ErrorResponse>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export function getErrorMessage(error: unknown): string {
  const axiosErr = error as AxiosError<ErrorResponse>;
  if (axiosErr.response?.data?.message) return axiosErr.response.data.message;
  if (axiosErr.response?.data?.error)   return axiosErr.response.data.error;
  if (axiosErr.message) return axiosErr.message;
  return 'An unexpected error occurred';
}

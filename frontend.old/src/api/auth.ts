import { api } from './client';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types';

export const authApi = {
  register: (data: RegisterRequest) =>
    api.post<AuthResponse>('/api/v1/auth/register', data),

  login: (data: LoginRequest) =>
    api.post<AuthResponse>('/api/v1/auth/login', data),

  logout: (refreshToken: string) =>
    api.post('/api/v1/auth/logout', { refreshToken }),

  verifyEmail: (email: string, otp: string) =>
    api.post('/api/v1/auth/verify-email', { email, otp }),

  forgotPassword: (email: string) =>
    api.post('/api/v1/auth/forgot-password', { email }),

  resetPassword: (email: string, otp: string, newPassword: string) =>
    api.post('/api/v1/auth/reset-password', { email, otp, newPassword }),

  changePassword: (currentPassword: string, newPassword: string) =>
    api.post('/api/v1/auth/change-password', { currentPassword, newPassword }),
};

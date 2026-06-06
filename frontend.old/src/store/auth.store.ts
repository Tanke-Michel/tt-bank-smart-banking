import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthResponse } from '../types';

interface AuthState {
  auth: AuthResponse | null;
  walletNumber: string | null;
  isAuthenticated: boolean;
  setAuth: (auth: AuthResponse) => void;
  setWalletNumber: (wn: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      auth: null,
      walletNumber: null,
      isAuthenticated: false,

      setAuth: (auth) => {
        // Also write a compact version to 'auth' key for the axios interceptor
        localStorage.setItem('auth', JSON.stringify({
          accessToken: auth.accessToken,
          walletNumber: undefined, // updated separately when wallet is fetched
        }));
        set({ auth, isAuthenticated: true });
      },

      setWalletNumber: (walletNumber) => {
        // Update the axios interceptor key so wallet header is sent automatically
        const raw = localStorage.getItem('auth');
        if (raw) {
          const parsed = JSON.parse(raw);
          localStorage.setItem('auth', JSON.stringify({ ...parsed, walletNumber }));
        }
        set({ walletNumber });
      },

      logout: () => {
        localStorage.removeItem('auth');
        set({ auth: null, walletNumber: null, isAuthenticated: false });
      },
    }),
    {
      name: 'smartbank-auth',
      partialize: (state) => ({
        auth: state.auth,
        walletNumber: state.walletNumber,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);

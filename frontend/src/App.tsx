import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { WalletPage } from './pages/wallet/WalletPage';
import { TransactionsPage } from './pages/transactions/TransactionsPage';
import { MerchantsPage } from './pages/merchants/MerchantsPage';
import { SavingsPage } from './pages/savings/SavingsPage';
import { AdminPage } from './pages/admin/AdminPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login"    element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Protected — AppLayout checks auth and redirects to /login if not authed */}
        <Route element={<AppLayout />}>
          <Route path="/dashboard"    element={<DashboardPage />} />
          <Route path="/wallet"       element={<WalletPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/merchants"    element={<MerchantsPage />} />
          <Route path="/savings"      element={<SavingsPage />} />
          <Route path="/admin"        element={<AdminPage />} />
        </Route>

        {/* Default redirect */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

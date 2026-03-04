import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { useShop } from '@/hooks/useShop'
import Layout from '@/components/Layout'
import LoginPage from '@/pages/LoginPage'
import ShopSetupPage from '@/pages/ShopSetupPage'
import DashboardPage from '@/pages/DashboardPage'
import DiagnosticPage from '@/pages/DiagnosticPage'
import CasesPage from '@/pages/CasesPage'
import CaseDetailPage from '@/pages/CaseDetailPage'
import SettingsPage from '@/pages/SettingsPage'
import BillingPage from '@/pages/BillingPage'
import BillingSuccessPage from '@/pages/BillingSuccessPage'
import LeadsPage from '@/pages/LeadsPage'

function AuthGuard({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="flex items-center justify-center h-screen text-gray-500">Loading…</div>
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

function ShopGuard({ children }: { children: React.ReactNode }) {
  const { shop, loading, isNotRegistered } = useShop()
  if (loading) return <div className="flex items-center justify-center h-screen text-gray-500">Loading…</div>
  if (isNotRegistered) return <Navigate to="/setup" replace />
  if (!shop) return (
    <div className="flex items-center justify-center h-screen">
      <div className="text-center">
        <p className="text-red-500 text-sm">Could not load shop profile. Check the API connection.</p>
        <button onClick={() => window.location.reload()} className="mt-3 text-blue-600 text-sm hover:underline">
          Retry
        </button>
      </div>
    </div>
  )
  return <>{children}</>
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />

        {/* Needs auth but no shop yet */}
        <Route path="/setup" element={<AuthGuard><ShopSetupPage /></AuthGuard>} />

        {/* Fully protected — needs auth + shop */}
        <Route element={<AuthGuard><ShopGuard><Layout /></ShopGuard></AuthGuard>}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/diagnostic" element={<DiagnosticPage />} />
          <Route path="/cases" element={<CasesPage />} />
          <Route path="/cases/:id" element={<CaseDetailPage />} />
          <Route path="/leads" element={<LeadsPage />} />
          <Route path="/billing" element={<BillingPage />} />
          <Route path="/billing/success" element={<BillingSuccessPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

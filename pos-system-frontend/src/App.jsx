import { useState } from 'react'
import { clearSession, loadSession } from './api'
import AppShell from './components/AppShell'
import LoginScreen from './features/auth/LoginScreen'
import AdminWorkspace from './features/admin/AdminWorkspace'
import CashierWorkspace from './features/cashier/CashierWorkspace'
import CashierShell from './features/cashier/CashierShell'
import { normalizeRole } from './utils/format'
import './App.css'

function App() {
  const [session, setSession] = useState(() => loadSession())
  const [activePage, setActivePage] = useState('Dashboard')
  const [selectedSaleId, setSelectedSaleId] = useState(null)

  function handleLogout() {
    clearSession()
    setSession(null)
    setActivePage('Dashboard')
  }

  if (!session?.token) {
    return <LoginScreen onLogin={setSession} />
  }

  const role = normalizeRole(session.role)
  const cashierPages = ['Checkout', 'Sales History', 'Receipt']
  const page = role === 'ADMIN'
    ? activePage
    : cashierPages.includes(activePage) ? activePage : 'Checkout'

  function handleReceiptOpen(saleId) {
    setSelectedSaleId(saleId)
    setActivePage('Receipt')
  }

  if (role === 'ADMIN') {
    return (
      <AppShell
        activePage={page}
        session={session}
        role={role}
        onLogout={handleLogout}
        onNavigate={setActivePage}
      >
        <AdminWorkspace
          page={page}
          selectedSaleId={selectedSaleId}
          onNavigate={setActivePage}
          onReceiptOpen={handleReceiptOpen}
        />
      </AppShell>
    )
  }

  return (
    <CashierShell
      activePage={page}
      session={session}
      onLogout={handleLogout}
      onNavigate={setActivePage}
    >
        <CashierWorkspace page={page} selectedSaleId={selectedSaleId} onReceiptOpen={handleReceiptOpen} />
    </CashierShell>
  )
}

export default App

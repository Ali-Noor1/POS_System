import ProductManagement from '../products/ProductManagement'
import CategoryManagement from '../categories/CategoryManagement'
import InventoryManagement from '../inventory/InventoryManagement'
import CustomerManagement from '../customers/CustomerManagement'
import CashierManagement from '../cashiers/CashierManagement'
import ReportsManagement from '../reports/ReportsManagement'
import SalesHistoryPage from '../sales/SalesHistoryPage'
import ReceiptPage from '../sales/ReceiptPage'
import SettingsManagement from '../settings/SettingsManagement'
import SupplierManagement from '../suppliers/SupplierManagement'
import AdminDashboard from './AdminDashboard'

export default function AdminWorkspace({ page, selectedSaleId, onNavigate, onReceiptOpen }) {
  if (page === 'Products') {
    return <ProductManagement />
  }

  if (page === 'Categories') {
    return <CategoryManagement />
  }

  if (page === 'Inventory') {
    return <InventoryManagement />
  }

  if (page === 'Suppliers') {
    return <SupplierManagement />
  }

  if (page === 'Customers') {
    return <CustomerManagement />
  }

  if (page === 'Cashiers') {
    return <CashierManagement />
  }

  if (page === 'Reports') {
    return <ReportsManagement />
  }

  if (page === 'Sales History') {
    return <SalesHistoryPage role="ADMIN" onReceiptOpen={onReceiptOpen} />
  }

  if (page === 'Receipt') {
    return <ReceiptPage saleId={selectedSaleId} />
  }

  if (page === 'Dashboard') {
    return <AdminDashboard onNavigate={onNavigate} />
  }

  if (page === 'Settings') {
    return <SettingsManagement />
  }

  return <AdminPlaceholder page={page} />
}


function AdminPlaceholder({ page }) {
  return (
    <section className="panel placeholder-panel">
      <p className="eyebrow">Coming next</p>
      <h2>{page}</h2>
      <p className="empty-state">This admin screen is still on the frontend roadmap.</p>
    </section>
  )
}


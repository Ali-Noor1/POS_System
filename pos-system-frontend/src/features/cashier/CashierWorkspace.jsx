import SalesHistoryPage from '../sales/SalesHistoryPage'
import ReceiptPage from '../sales/ReceiptPage'
import CashierCheckout from './CashierCheckout'

export default function CashierWorkspace({ page, selectedSaleId, onReceiptOpen }) {
  if (page === 'Sales History') {
    return <SalesHistoryPage role="CASHIER" onReceiptOpen={onReceiptOpen} />
  }

  if (page === 'Receipt') {
    return <ReceiptPage saleId={selectedSaleId} />
  }

  return <CashierCheckout onReceiptOpen={onReceiptOpen} />
}


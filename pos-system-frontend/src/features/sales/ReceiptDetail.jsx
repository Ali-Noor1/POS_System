import StatusPill from '../../components/StatusPill'
import { formatDateTime, formatEnum, money } from '../../utils/format'

export function ReceiptDetail({ sale, full = false }) {
  const items = sale.items || []

  return (
    <div className={full ? 'receipt-detail full' : 'receipt-detail'}>
      <div className="receipt-header">
        <div>
          <strong>Retail POS</strong>
          <span>{sale.receiptNumber}</span>
        </div>
        <StatusPill status={sale.saleStatus} />
      </div>

      <div className="receipt-meta">
        <span>Customer</span>
        <strong>{sale.customerName || 'Walk-in customer'}</strong>
        <span>Cashier</span>
        <strong>{sale.cashierUsername}</strong>
        <span>Date</span>
        <strong>{formatDateTime(sale.completedAt)}</strong>
      </div>

      <div className="receipt-items">
        {items.map((item) => (
          <div className="receipt-item" key={item.id}>
            <div>
              <strong>{item.productName}</strong>
              <span>{item.productSku} / {money(item.quantity)} x Rs {money(item.unitPrice)}</span>
            </div>
            <strong>Rs {money(item.lineTotal)}</strong>
          </div>
        ))}
      </div>

      <div className="receipt-totals">
        <div><span>Subtotal</span><strong>Rs {money(sale.subtotal)}</strong></div>
        <div><span>Discount</span><strong>Rs {money(sale.discountAmount)}</strong></div>
        <div><span>Total</span><strong>Rs {money(sale.totalAmount)}</strong></div>
        <div><span>Received</span><strong>Rs {money(sale.amountReceived)}</strong></div>
        <div><span>Change</span><strong>Rs {money(sale.changeAmount)}</strong></div>
        <div><span>Payment</span><strong>{formatEnum(sale.payment?.paymentMethod)}</strong></div>
        <div><span>Reference</span><strong>{sale.payment?.referenceNumber || '-'}</strong></div>
      </div>
    </div>
  )
}


import { useEffect, useMemo, useState } from 'react'
import { cancelSale, getSaleDetails, getSalesHistory } from '../../api'
import PaginationControls from '../../components/PaginationControls'
import StatusPill from '../../components/StatusPill'
import { usePagination } from '../../hooks/usePagination'
import { formatDateTime, formatEnum, money } from '../../utils/format'
import { ReceiptDetail } from './ReceiptDetail'

export default function SalesHistoryPage({ role, onReceiptOpen }) {
  const [sales, setSales] = useState([])
  const [selectedSale, setSelectedSale] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  const [filter, setFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    loadSales()
  }, [])

  async function loadSales() {
    setLoading(true)
    setError('')

    try {
      const nextSales = await getSalesHistory()
      setSales(nextSales)
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  const filterText = filter.trim().toLowerCase()
  const filteredSales = useMemo(
    () =>
      sales.filter((sale) => {
        const haystack = [
          sale.receiptNumber,
          sale.customerName,
          sale.cashierUsername,
          sale.saleStatus,
          sale.paymentMethod,
        ].join(' ').toLowerCase()
        return haystack.includes(filterText)
      }),
    [filterText, sales],
  )
  const salePagination = usePagination(filteredSales, 10, filterText)

  async function openDetails(saleId) {
    setDetailLoading(true)
    setError('')
    setSuccess('')
    setCancelReason('')

    try {
      setSelectedSale(await getSaleDetails(saleId))
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setDetailLoading(false)
    }
  }

  async function handleCancelSale(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!selectedSale) {
      setError('Select a sale before cancelling.')
      return
    }

    if (!cancelReason.trim()) {
      setError('Cancellation reason is required.')
      return
    }

    setCancelling(true)
    try {
      const cancelledSale = await cancelSale(selectedSale.saleId, cancelReason.trim())
      setSelectedSale(cancelledSale)
      setCancelReason('')
      setSuccess(`${cancelledSale.receiptNumber} cancelled and stock restored.`)
      await loadSales()
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setCancelling(false)
    }
  }

  return (
    <div className="management-layout">
      <section className="data-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Sales ledger</p>
            <h2>Sales History</h2>
          </div>
          <button type="button" onClick={loadSales} disabled={loading}>Refresh</button>
        </div>

        <div className="table-toolbar">
          <input
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            placeholder="Search receipt, customer, cashier, status"
          />
          <span>{filteredSales.length} sales</span>
        </div>

        {error ? <p className="form-error">{error}</p> : null}
        {success ? <p className="form-success">{success}</p> : null}

        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Receipt</th>
                <th>Customer</th>
                <th>Cashier</th>
                <th>Payment</th>
                <th>Status</th>
                <th>Total</th>
                <th>Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="8">Loading sales...</td>
                </tr>
              ) : filteredSales.length ? (
                salePagination.pageItems.map((sale) => (
                  <tr key={sale.saleId}>
                    <td><strong>{sale.receiptNumber}</strong></td>
                    <td>{sale.customerName || 'Walk-in customer'}</td>
                    <td>{sale.cashierUsername}</td>
                    <td>{formatEnum(sale.paymentMethod)}</td>
                    <td><StatusPill status={sale.saleStatus} /></td>
                    <td>Rs {money(sale.totalAmount)}</td>
                    <td>{formatDateTime(sale.createdAt)}</td>
                    <td>
                      <div className="action-row">
                        <button type="button" onClick={() => openDetails(sale.saleId)}>Details</button>
                        <button type="button" onClick={() => onReceiptOpen(sale.saleId)}>Receipt</button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="8">No sales found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <PaginationControls {...salePagination} />
      </section>

      <aside className="form-panel receipt-side">
        <div className="panel-heading">
          <h2>Receipt Detail</h2>
        </div>

        {detailLoading ? (
          <p className="empty-state">Loading receipt...</p>
        ) : selectedSale ? (
          <>
            <ReceiptDetail sale={selectedSale} />

            {role === 'ADMIN' && selectedSale.saleStatus !== 'CANCELLED' ? (
              <form className="management-form cancel-form" onSubmit={handleCancelSale}>
                <label>
                  Cancellation Reason
                  <textarea value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} />
                </label>
                <button className="danger-button" type="submit" disabled={cancelling}>
                  {cancelling ? 'Cancelling...' : 'Cancel Sale'}
                </button>
              </form>
            ) : null}
          </>
        ) : (
          <p className="empty-state">Select Details on a sale row.</p>
        )}
      </aside>
    </div>
  )
}


import { useEffect, useState } from 'react'
import { getSaleDetails } from '../../api'
import { ReceiptDetail } from './ReceiptDetail'

export default function ReceiptPage({ saleId }) {
  const [lookupSaleId, setLookupSaleId] = useState(saleId ? String(saleId) : '')
  const [sale, setSale] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (saleId) {
      loadReceipt(saleId)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [saleId])

  async function loadReceipt(nextSaleId = lookupSaleId) {
    setError('')

    if (!nextSaleId) {
      setError('Enter a sale ID or open a receipt from sales history.')
      return
    }

    setLoading(true)
    try {
      setSale(await getSaleDetails(nextSaleId))
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="report-layout">
      <section className="data-panel receipt-page">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Printable sale detail</p>
            <h2>Receipt</h2>
          </div>
        </div>

        <form className="receipt-lookup" onSubmit={(event) => {
          event.preventDefault()
          loadReceipt()
        }}>
          <input
            value={lookupSaleId}
            onChange={(event) => setLookupSaleId(event.target.value)}
            placeholder="Enter sale ID"
          />
          <button type="submit" disabled={loading}>{loading ? 'Loading...' : 'Load Receipt'}</button>
          <button type="button" onClick={() => window.print()} disabled={!sale}>Print</button>
        </form>

        {error ? <p className="form-error">{error}</p> : null}

        {sale ? <ReceiptDetail sale={sale} full /> : <p className="empty-state">No receipt loaded.</p>}
      </section>
    </div>
  )
}


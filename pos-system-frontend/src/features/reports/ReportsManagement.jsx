import { useState } from 'react'
import { getInventoryMovementReport, getProductSalesReport, getSalesReport } from '../../api'
import StatusPill from '../../components/StatusPill'
import TransactionTypeLabel from '../../components/TransactionTypeLabel'
import { defaultStartDate, formatDateTime, money, todayDate } from '../../utils/format'

export default function ReportsManagement() {
  const [activeReport, setActiveReport] = useState('sales')
  const [startDate, setStartDate] = useState(() => defaultStartDate())
  const [endDate, setEndDate] = useState(() => todayDate())
  const [salesReport, setSalesReport] = useState(null)
  const [productSalesReport, setProductSalesReport] = useState(null)
  const [inventoryReport, setInventoryReport] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function loadReport() {
    setError('')

    if (!startDate || !endDate) {
      setError('Start date and end date are required.')
      return
    }

    if (startDate > endDate) {
      setError('Start date cannot be after end date.')
      return
    }

    setLoading(true)
    try {
      if (activeReport === 'sales') {
        setSalesReport(await getSalesReport(startDate, endDate))
      } else if (activeReport === 'product-sales') {
        setProductSalesReport(await getProductSalesReport(startDate, endDate))
      } else {
        setInventoryReport(await getInventoryMovementReport(startDate, endDate))
      }
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  function switchReport(report) {
    setActiveReport(report)
    setError('')
  }

  const reportTabs = [
    ['sales', 'Sales'],
    ['product-sales', 'Product Sales'],
    ['inventory', 'Inventory Movement'],
  ]

  return (
    <div className="report-layout">
      <section className="data-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Business reporting</p>
            <h2>Reports</h2>
          </div>
          <button type="button" onClick={loadReport} disabled={loading}>
            {loading ? 'Loading...' : 'Run Report'}
          </button>
        </div>

        <div className="report-controls">
          <div className="segmented-control">
            {reportTabs.map(([value, label]) => (
              <button
                className={activeReport === value ? 'active' : ''}
                key={value}
                type="button"
                onClick={() => switchReport(value)}
              >
                {label}
              </button>
            ))}
          </div>

          <label>
            Start Date
            <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
          </label>

          <label>
            End Date
            <input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
          </label>
        </div>

        {error ? <p className="form-error">{error}</p> : null}

        {activeReport === 'sales' ? (
          <SalesReportView report={salesReport} loading={loading} />
        ) : activeReport === 'product-sales' ? (
          <ProductSalesReportView report={productSalesReport} loading={loading} />
        ) : (
          <InventoryMovementReportView report={inventoryReport} loading={loading} />
        )}
      </section>
    </div>
  )
}


function SalesReportView({ report, loading }) {
  const sales = report?.sales || []

  return (
    <>
      <section className="metric-grid report-metrics">
        <article className="metric-card">
          <span>Gross Sales</span>
          <strong>Rs {money(report?.grossSalesTotal)}</strong>
        </article>
        <article className="metric-card">
          <span>Completed Sales</span>
          <strong>{report?.completedSaleCount || 0}</strong>
        </article>
        <article className="metric-card">
          <span>Cancelled Sales</span>
          <strong>{report?.cancelledSaleCount || 0}</strong>
        </article>
      </section>

      <div className="table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Receipt</th>
              <th>Customer</th>
              <th>Cashier</th>
              <th>Status</th>
              <th>Total</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="6">Loading sales report...</td>
              </tr>
            ) : sales.length ? (
              sales.map((sale) => (
                <tr key={sale.saleId}>
                  <td><strong>{sale.receiptNumber}</strong></td>
                  <td>{sale.customerName || 'Walk-in customer'}</td>
                  <td>{sale.cashierUsername}</td>
                  <td><StatusPill status={sale.saleStatus} /></td>
                  <td>Rs {money(sale.totalAmount)}</td>
                  <td>{formatDateTime(sale.createdAt)}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="6">Run the report to view sales.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}


function ProductSalesReportView({ report, loading }) {
  const products = report?.products || []

  return (
    <>
      <section className="metric-grid report-metrics">
        <article className="metric-card">
          <span>Total Revenue</span>
          <strong>Rs {money(report?.totalRevenue)}</strong>
        </article>
        <article className="metric-card">
          <span>Quantity Sold</span>
          <strong>{money(report?.totalQuantitySold)}</strong>
        </article>
        <article className="metric-card">
          <span>Products</span>
          <strong>{products.length}</strong>
        </article>
      </section>

      <div className="table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Product</th>
              <th>SKU</th>
              <th>Quantity Sold</th>
              <th>Revenue</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="4">Loading product sales report...</td>
              </tr>
            ) : products.length ? (
              products.map((product) => (
                <tr key={product.productId}>
                  <td><strong>{product.productName}</strong></td>
                  <td>{product.productSku}</td>
                  <td>{money(product.quantitySold)}</td>
                  <td>Rs {money(product.revenue)}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="4">Run the report to view product sales.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}


function InventoryMovementReportView({ report, loading }) {
  const movements = report?.movements || []

  return (
    <>
      <section className="metric-grid report-metrics">
        <article className="metric-card">
          <span>Total In</span>
          <strong>{money(report?.totalInQuantity)}</strong>
        </article>
        <article className="metric-card">
          <span>Total Out</span>
          <strong>{money(report?.totalOutQuantity)}</strong>
        </article>
        <article className="metric-card">
          <span>Net Change</span>
          <strong>{money(report?.netQuantityChange)}</strong>
        </article>
      </section>

      <div className="table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Product</th>
              <th>Type</th>
              <th>Quantity</th>
              <th>Before</th>
              <th>After</th>
              <th>User</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="7">Loading inventory movement report...</td>
              </tr>
            ) : movements.length ? (
              movements.map((movement) => (
                <tr key={movement.id}>
                  <td><strong>{movement.productName}</strong></td>
                  <td><TransactionTypeLabel type={movement.transactionType} /></td>
                  <td>{money(movement.quantityChange)}</td>
                  <td>{money(movement.stockBefore)}</td>
                  <td>{money(movement.stockAfter)}</td>
                  <td>{movement.createdByFullName || movement.createdByUsername || 'System'}</td>
                  <td>{formatDateTime(movement.createdAt)}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="7">Run the report to view inventory movements.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}


import { useEffect, useState } from 'react'
import { adjustInventory, getLowStockProducts, getProductInventoryTransactions, getProducts } from '../../api'
import PaginationControls from '../../components/PaginationControls'
import TransactionTypeLabel from '../../components/TransactionTypeLabel'
import { usePagination } from '../../hooks/usePagination'
import { blankInventoryForm } from '../../utils/formDefaults'
import { formatDateTime, money } from '../../utils/format'

export default function InventoryManagement() {
  const [products, setProducts] = useState([])
  const [lowStockProducts, setLowStockProducts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [form, setForm] = useState(blankInventoryForm)
  const [loading, setLoading] = useState(true)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const selectedProduct = products.find((product) => String(product.id) === String(form.productId))
  const transactionPagination = usePagination(transactions, 10, form.productId)

  useEffect(() => {
    loadInventoryData()
  }, [])

  useEffect(() => {
    if (!form.productId) {
      return
    }

    loadTransactions(form.productId)
  }, [form.productId])

  async function loadInventoryData() {
    setLoading(true)
    setError('')

    try {
      const [nextProducts, nextLowStockProducts] = await Promise.all([
        getProducts(),
        getLowStockProducts(),
      ])
      setProducts(nextProducts)
      setLowStockProducts(nextLowStockProducts)
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  async function loadTransactions(productId) {
    setHistoryLoading(true)
    setError('')

    try {
      const nextTransactions = await getProductInventoryTransactions(productId)
      setTransactions(nextTransactions)
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setHistoryLoading(false)
    }
  }

  function updateInventoryField(field, value) {
    setForm((currentForm) => ({ ...currentForm, [field]: value }))
    if (field === 'productId' && !value) {
      setTransactions([])
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!form.productId) {
      setError('Select a product before adjusting stock.')
      return
    }

    if (!form.quantity || Number(form.quantity) <= 0) {
      setError('Quantity must be greater than zero.')
      return
    }

    setSaving(true)
    try {
      const adjustment = await adjustInventory({
        productId: Number(form.productId),
        transactionType: form.transactionType,
        quantity: Number(form.quantity).toFixed(3),
        note: form.note.trim() || null,
      })

      setSuccess(`${adjustment.productName} stock updated from ${money(adjustment.stockBefore)} to ${money(adjustment.stockAfter)}.`)
      setForm((currentForm) => ({ ...currentForm, quantity: '', note: '' }))
      await loadInventoryData()
      await loadTransactions(form.productId)
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving(false)
    }
  }

  function selectProduct(productId) {
    setForm((currentForm) => ({ ...currentForm, productId: String(productId) }))
    setSuccess('')
  }

  return (
    <div className="management-layout">
      <section className="data-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Stock control</p>
            <h2>Inventory History</h2>
          </div>
          <button type="button" onClick={loadInventoryData} disabled={loading}>Refresh</button>
        </div>

        {error ? <p className="form-error">{error}</p> : null}
        {success ? <p className="form-success">{success}</p> : null}

        <div className="inventory-focus">
          <div>
            <span>Selected Product</span>
            <strong>{selectedProduct?.name || 'No product selected'}</strong>
          </div>
          <div>
            <span>Current Stock</span>
            <strong>{selectedProduct ? money(selectedProduct.currentStock) : '0.000'}</strong>
          </div>
          <div>
            <span>Reorder Level</span>
            <strong>{selectedProduct ? money(selectedProduct.reorderLevel) : '0.000'}</strong>
          </div>
        </div>

        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Quantity</th>
                <th>Before</th>
                <th>After</th>
                <th>User</th>
                <th>Date</th>
                <th>Note</th>
              </tr>
            </thead>
            <tbody>
              {historyLoading ? (
                <tr>
                  <td colSpan="7">Loading transactions...</td>
                </tr>
              ) : transactions.length ? (
                transactionPagination.pageItems.map((transaction) => (
                  <tr key={transaction.id}>
                    <td><TransactionTypeLabel type={transaction.transactionType} /></td>
                    <td>{money(transaction.quantityChange)}</td>
                    <td>{money(transaction.stockBefore)}</td>
                    <td>{money(transaction.stockAfter)}</td>
                    <td>{transaction.createdByFullName || transaction.createdByUsername || 'System'}</td>
                    <td>{formatDateTime(transaction.createdAt)}</td>
                    <td>{transaction.note || 'No note'}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="7">
                    {form.productId ? 'No inventory transactions found.' : 'Select a product to view history.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <PaginationControls {...transactionPagination} />
      </section>

      <aside className="form-panel">
        <div className="panel-heading">
          <h2>Adjust Stock</h2>
        </div>

        <form className="management-form" onSubmit={handleSubmit}>
          <label>
            Product
            <select value={form.productId} onChange={(event) => updateInventoryField('productId', event.target.value)}>
              <option value="">Select product</option>
              {products.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name} / {product.sku}
                </option>
              ))}
            </select>
          </label>

          <label>
            Adjustment Type
            <select
              value={form.transactionType}
              onChange={(event) => updateInventoryField('transactionType', event.target.value)}
            >
              <option value="ADJUSTMENT_IN">Stock In</option>
              <option value="ADJUSTMENT_OUT">Stock Out</option>
            </select>
          </label>

          <label>
            Quantity
            <input
              min="0.001"
              step="0.001"
              type="number"
              value={form.quantity}
              onChange={(event) => updateInventoryField('quantity', event.target.value)}
            />
          </label>

          <label>
            Note
            <textarea value={form.note} onChange={(event) => updateInventoryField('note', event.target.value)} />
          </label>

          <button className="primary-button" type="submit" disabled={saving || loading}>
            {saving ? 'Saving...' : 'Save Adjustment'}
          </button>
        </form>

        <div className="side-section">
          <div className="panel-heading">
            <h2>Low Stock</h2>
            <span>{lowStockProducts.length}</span>
          </div>
          <div className="low-stock-list">
            {loading ? (
              <p className="empty-state">Loading low stock...</p>
            ) : lowStockProducts.length ? (
              lowStockProducts.map((product) => (
                <button key={product.productId} type="button" onClick={() => selectProduct(product.productId)}>
                  <strong>{product.name}</strong>
                  <span>{product.sku} / need {money(product.shortageQuantity)}</span>
                </button>
              ))
            ) : (
              <p className="empty-state">No low-stock products.</p>
            )}
          </div>
        </div>
      </aside>
    </div>
  )
}


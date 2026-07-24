import { useEffect, useMemo, useState } from 'react'
import {
  createPurchase,
  createSupplier,
  createSupplierPayment,
  getProducts,
  getPurchases,
  getSupplierPayments,
  getSuppliers,
  updateSupplier,
  updateSupplierStatus,
} from '../../api'
import PaginationControls from '../../components/PaginationControls'
import StatusPill from '../../components/StatusPill'
import { usePagination } from '../../hooks/usePagination'
import { formatDateTime, formatEnum, money } from '../../utils/format'

const blankSupplierForm = {
  name: '',
  companyName: '',
  phone: '',
  email: '',
  address: '',
}

const blankPurchaseForm = {
  supplierId: '',
  productId: '',
  quantity: '',
  unitCost: '',
  paidAmount: '',
  discountAmount: '',
  note: '',
}

const blankPaymentForm = {
  supplierId: '',
  purchaseId: '',
  paymentMethod: 'CASH',
  amount: '',
  referenceNumber: '',
  note: '',
}

export default function SupplierManagement() {
  const [activeTab, setActiveTab] = useState('Suppliers')
  const [suppliers, setSuppliers] = useState([])
  const [products, setProducts] = useState([])
  const [purchases, setPurchases] = useState([])
  const [payments, setPayments] = useState([])
  const [supplierForm, setSupplierForm] = useState(blankSupplierForm)
  const [purchaseForm, setPurchaseForm] = useState(blankPurchaseForm)
  const [paymentForm, setPaymentForm] = useState(blankPaymentForm)
  const [editingSupplier, setEditingSupplier] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const activeSuppliers = useMemo(
    () => suppliers.filter((supplier) => supplier.status === 'ACTIVE'),
    [suppliers],
  )
  const supplierPagination = usePagination(suppliers, 10, suppliers.length)
  const purchasePagination = usePagination(purchases, 10, purchases.length)
  const paymentPagination = usePagination(payments, 10, payments.length)
  const selectedPurchaseProduct = products.find(
    (product) => String(product.id) === String(purchaseForm.productId),
  )

  useEffect(() => {
    loadSupplierModule()
  }, [])

  async function loadSupplierModule() {
    setLoading(true)
    setError('')

    try {
      const [nextSuppliers, nextProducts, nextPurchases, nextPayments] = await Promise.all([
        getSuppliers(),
        getProducts(),
        getPurchases(),
        getSupplierPayments(),
      ])
      setSuppliers(nextSuppliers)
      setProducts(nextProducts)
      setPurchases(nextPurchases)
      setPayments(nextPayments)
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  function resetSupplierForm() {
    setSupplierForm(blankSupplierForm)
    setEditingSupplier(null)
  }

  function startEditSupplier(supplier) {
    setEditingSupplier(supplier)
    setError('')
    setSuccess('')
    setSupplierForm({
      name: supplier.name || '',
      companyName: supplier.companyName || '',
      phone: supplier.phone || '',
      email: supplier.email || '',
      address: supplier.address || '',
    })
  }

  async function handleSupplierSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!supplierForm.name.trim()) {
      setError('Supplier name is required.')
      return
    }

    const payload = {
      name: supplierForm.name.trim(),
      companyName: supplierForm.companyName.trim() || null,
      phone: supplierForm.phone.trim() || null,
      email: supplierForm.email.trim() || null,
      address: supplierForm.address.trim() || null,
    }

    setSaving(true)
    try {
      if (editingSupplier) {
        await updateSupplier(editingSupplier.id, payload)
        setSuccess('Supplier updated.')
      } else {
        await createSupplier(payload)
        setSuccess('Supplier created.')
      }
      resetSupplierForm()
      await loadSupplierModule()
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleSupplierStatusToggle(supplier) {
    setError('')
    setSuccess('')
    const nextStatus = supplier.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'

    try {
      await updateSupplierStatus(supplier.id, nextStatus)
      setSuccess(`${supplier.name} marked ${nextStatus.toLowerCase()}.`)
      await loadSupplierModule()
    } catch (apiError) {
      setError(apiError.message)
    }
  }

  async function handlePurchaseSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!purchaseForm.supplierId || !purchaseForm.productId) {
      setError('Select supplier and product before creating purchase.')
      return
    }

    if (!purchaseForm.quantity || Number(purchaseForm.quantity) <= 0) {
      setError('Purchase quantity must be greater than zero.')
      return
    }

    if (!purchaseForm.unitCost || Number(purchaseForm.unitCost) < 0) {
      setError('Unit cost is required and cannot be negative.')
      return
    }

    setSaving(true)
    try {
      const purchase = await createPurchase({
        supplierId: Number(purchaseForm.supplierId),
        discountAmount: Number(purchaseForm.discountAmount || 0).toFixed(2),
        paidAmount: Number(purchaseForm.paidAmount || 0).toFixed(2),
        note: purchaseForm.note.trim() || null,
        items: [
          {
            productId: Number(purchaseForm.productId),
            quantity: Number(purchaseForm.quantity).toFixed(3),
            unitCost: Number(purchaseForm.unitCost).toFixed(2),
          },
        ],
      })

      setSuccess(`${purchase.purchaseNumber} created and stock updated.`)
      setPurchaseForm(blankPurchaseForm)
      await loadSupplierModule()
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving(false)
    }
  }

  async function handlePaymentSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!paymentForm.supplierId) {
      setError('Select supplier before recording payment.')
      return
    }

    if (!paymentForm.amount || Number(paymentForm.amount) <= 0) {
      setError('Payment amount must be greater than zero.')
      return
    }

    setSaving(true)
    try {
      await createSupplierPayment({
        supplierId: Number(paymentForm.supplierId),
        purchaseId: paymentForm.purchaseId ? Number(paymentForm.purchaseId) : null,
        paymentMethod: paymentForm.paymentMethod,
        amount: Number(paymentForm.amount).toFixed(2),
        referenceNumber: paymentForm.referenceNumber.trim() || null,
        note: paymentForm.note.trim() || null,
      })

      setSuccess('Supplier payment recorded.')
      setPaymentForm(blankPaymentForm)
      await loadSupplierModule()
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="supplier-workspace">
      <section className="data-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Procurement</p>
            <h2>Supplier Management</h2>
          </div>
          <button type="button" onClick={loadSupplierModule} disabled={loading}>Refresh</button>
        </div>

        <div className="settings-tabs">
          {['Suppliers', 'Purchases', 'Payments'].map((tab) => (
            <button
              className={activeTab === tab ? 'active' : ''}
              key={tab}
              type="button"
              onClick={() => {
                setActiveTab(tab)
                setError('')
                setSuccess('')
              }}
            >
              {tab}
            </button>
          ))}
        </div>

        {error ? <p className="form-error">{error}</p> : null}
        {success ? <p className="form-success">{success}</p> : null}

        {activeTab === 'Suppliers' ? (
          <SupplierTable
            loading={loading}
            onEdit={startEditSupplier}
            onStatusToggle={handleSupplierStatusToggle}
            pagination={supplierPagination}
          />
        ) : null}

        {activeTab === 'Purchases' ? (
          <PurchaseTable loading={loading} pagination={purchasePagination} />
        ) : null}

        {activeTab === 'Payments' ? (
          <PaymentTable loading={loading} pagination={paymentPagination} />
        ) : null}
      </section>

      <aside className="form-panel">
        {activeTab === 'Suppliers' ? (
          <>
            <div className="panel-heading">
              <h2>{editingSupplier ? 'Edit Supplier' : 'Add Supplier'}</h2>
              {editingSupplier ? <button type="button" onClick={resetSupplierForm}>Cancel</button> : null}
            </div>
            <form className="management-form" onSubmit={handleSupplierSubmit}>
              <label>Supplier Name<input value={supplierForm.name} onChange={(event) => setSupplierForm({ ...supplierForm, name: event.target.value })} /></label>
              <label>Company Name<input value={supplierForm.companyName} onChange={(event) => setSupplierForm({ ...supplierForm, companyName: event.target.value })} /></label>
              <label>Phone<input value={supplierForm.phone} onChange={(event) => setSupplierForm({ ...supplierForm, phone: event.target.value })} /></label>
              <label>Email<input type="email" value={supplierForm.email} onChange={(event) => setSupplierForm({ ...supplierForm, email: event.target.value })} /></label>
              <label>Address<textarea value={supplierForm.address} onChange={(event) => setSupplierForm({ ...supplierForm, address: event.target.value })} /></label>
              <button className="primary-button" type="submit" disabled={saving}>
                {saving ? 'Saving...' : editingSupplier ? 'Save Supplier' : 'Create Supplier'}
              </button>
            </form>
          </>
        ) : null}

        {activeTab === 'Purchases' ? (
          <>
            <div className="panel-heading"><h2>Create Purchase</h2></div>
            <form className="management-form" onSubmit={handlePurchaseSubmit}>
              <label>
                Supplier
                <select value={purchaseForm.supplierId} onChange={(event) => setPurchaseForm({ ...purchaseForm, supplierId: event.target.value })}>
                  <option value="">Select supplier</option>
                  {activeSuppliers.map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.name}</option>)}
                </select>
              </label>
              <label>
                Product
                <select
                  value={purchaseForm.productId}
                  onChange={(event) => {
                    const product = products.find((item) => String(item.id) === event.target.value)
                    setPurchaseForm({
                      ...purchaseForm,
                      productId: event.target.value,
                      unitCost: product?.costPrice ? String(product.costPrice) : purchaseForm.unitCost,
                    })
                  }}
                >
                  <option value="">Select product</option>
                  {products.map((product) => <option key={product.id} value={product.id}>{product.name} / {product.sku}</option>)}
                </select>
              </label>
              <div className="supplier-summary">
                <span>Current Stock</span>
                <strong>{selectedPurchaseProduct ? money(selectedPurchaseProduct.currentStock) : '0.000'}</strong>
              </div>
              <div className="form-grid-two">
                <label>Quantity<input min="0.001" step="0.001" type="number" value={purchaseForm.quantity} onChange={(event) => setPurchaseForm({ ...purchaseForm, quantity: event.target.value })} /></label>
                <label>Unit Cost<input min="0" step="0.01" type="number" value={purchaseForm.unitCost} onChange={(event) => setPurchaseForm({ ...purchaseForm, unitCost: event.target.value })} /></label>
              </div>
              <div className="form-grid-two">
                <label>Paid Amount<input min="0" step="0.01" type="number" value={purchaseForm.paidAmount} onChange={(event) => setPurchaseForm({ ...purchaseForm, paidAmount: event.target.value })} /></label>
                <label>Discount<input min="0" step="0.01" type="number" value={purchaseForm.discountAmount} onChange={(event) => setPurchaseForm({ ...purchaseForm, discountAmount: event.target.value })} /></label>
              </div>
              <label>Note<textarea value={purchaseForm.note} onChange={(event) => setPurchaseForm({ ...purchaseForm, note: event.target.value })} /></label>
              <button className="primary-button" type="submit" disabled={saving || loading}>
                {saving ? 'Saving...' : 'Create Purchase'}
              </button>
            </form>
          </>
        ) : null}

        {activeTab === 'Payments' ? (
          <>
            <div className="panel-heading"><h2>Record Payment</h2></div>
            <form className="management-form" onSubmit={handlePaymentSubmit}>
              <label>
                Supplier
                <select value={paymentForm.supplierId} onChange={(event) => setPaymentForm({ ...paymentForm, supplierId: event.target.value, purchaseId: '' })}>
                  <option value="">Select supplier</option>
                  {activeSuppliers.map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.name}</option>)}
                </select>
              </label>
              <label>
                Purchase
                <select value={paymentForm.purchaseId} onChange={(event) => setPaymentForm({ ...paymentForm, purchaseId: event.target.value })}>
                  <option value="">No specific purchase</option>
                  {purchases
                    .filter((purchase) => !paymentForm.supplierId || String(purchase.supplierId) === String(paymentForm.supplierId))
                    .map((purchase) => <option key={purchase.id} value={purchase.id}>{purchase.purchaseNumber} / Due Rs {money(purchase.dueAmount)}</option>)}
                </select>
              </label>
              <label>
                Payment Method
                <select value={paymentForm.paymentMethod} onChange={(event) => setPaymentForm({ ...paymentForm, paymentMethod: event.target.value })}>
                  <option value="CASH">Cash</option>
                  <option value="CARD">Card</option>
                  <option value="BANK_TRANSFER">Bank Transfer</option>
                  <option value="MOBILE_WALLET">Mobile Wallet</option>
                </select>
              </label>
              <label>Amount<input min="0.01" step="0.01" type="number" value={paymentForm.amount} onChange={(event) => setPaymentForm({ ...paymentForm, amount: event.target.value })} /></label>
              <label>Reference Number<input value={paymentForm.referenceNumber} onChange={(event) => setPaymentForm({ ...paymentForm, referenceNumber: event.target.value })} /></label>
              <label>Note<textarea value={paymentForm.note} onChange={(event) => setPaymentForm({ ...paymentForm, note: event.target.value })} /></label>
              <button className="primary-button" type="submit" disabled={saving || loading}>
                {saving ? 'Saving...' : 'Record Payment'}
              </button>
            </form>
          </>
        ) : null}
      </aside>
    </div>
  )
}

function SupplierTable({ loading, onEdit, onStatusToggle, pagination }) {
  return (
    <>
      <div className="table-wrap">
        <table className="admin-table">
          <thead><tr><th>Supplier</th><th>Contact</th><th>Address</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="5">Loading suppliers...</td></tr>
            ) : pagination.pageItems.length ? (
              pagination.pageItems.map((supplier) => (
                <tr key={supplier.id}>
                  <td><strong>{supplier.name}</strong><span>{supplier.companyName || 'No company name'}</span></td>
                  <td><strong>{supplier.phone || '-'}</strong><span>{supplier.email || '-'}</span></td>
                  <td>{supplier.address || 'No address'}</td>
                  <td><StatusPill status={supplier.status} /></td>
                  <td>
                    <div className="action-row">
                      <button type="button" onClick={() => onEdit(supplier)}>Edit</button>
                      <button type="button" onClick={() => onStatusToggle(supplier)}>{supplier.status === 'ACTIVE' ? 'Disable' : 'Enable'}</button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan="5">No suppliers found.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      <PaginationControls {...pagination} />
    </>
  )
}

function PurchaseTable({ loading, pagination }) {
  return (
    <>
      <div className="table-wrap">
        <table className="admin-table">
          <thead><tr><th>Purchase</th><th>Supplier</th><th>Total</th><th>Paid / Due</th><th>Status</th><th>Date</th></tr></thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="6">Loading purchases...</td></tr>
            ) : pagination.pageItems.length ? (
              pagination.pageItems.map((purchase) => (
                <tr key={purchase.id}>
                  <td><strong>{purchase.purchaseNumber}</strong><span>{purchase.items?.length || 0} item(s)</span></td>
                  <td>{purchase.supplierName}</td>
                  <td>Rs {money(purchase.totalAmount)}</td>
                  <td><strong>Paid Rs {money(purchase.paidAmount)}</strong><span>Due Rs {money(purchase.dueAmount)}</span></td>
                  <td><StatusPill status={purchase.status} /></td>
                  <td>{formatDateTime(purchase.createdAt)}</td>
                </tr>
              ))
            ) : (
              <tr><td colSpan="6">No purchases found.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      <PaginationControls {...pagination} />
    </>
  )
}

function PaymentTable({ loading, pagination }) {
  return (
    <>
      <div className="table-wrap">
        <table className="admin-table">
          <thead><tr><th>Supplier</th><th>Purchase</th><th>Method</th><th>Amount</th><th>Reference</th><th>Date</th></tr></thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="6">Loading supplier payments...</td></tr>
            ) : pagination.pageItems.length ? (
              pagination.pageItems.map((payment) => (
                <tr key={payment.id}>
                  <td>{payment.supplierName}</td>
                  <td>{payment.purchaseNumber || '-'}</td>
                  <td>{formatEnum(payment.paymentMethod)}</td>
                  <td>Rs {money(payment.amount)}</td>
                  <td>{payment.referenceNumber || '-'}</td>
                  <td>{formatDateTime(payment.paidAt)}</td>
                </tr>
              ))
            ) : (
              <tr><td colSpan="6">No supplier payments found.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      <PaginationControls {...pagination} />
    </>
  )
}

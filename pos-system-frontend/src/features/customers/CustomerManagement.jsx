import { useEffect, useMemo, useState } from 'react'
import { createCustomer, getCustomers, updateCustomer, updateCustomerStatus } from '../../api'
import PaginationControls from '../../components/PaginationControls'
import StatusPill from '../../components/StatusPill'
import { usePagination } from '../../hooks/usePagination'
import { blankCustomerForm } from '../../utils/formDefaults'

export default function CustomerManagement() {
  const [customers, setCustomers] = useState([])
  const [form, setForm] = useState(blankCustomerForm)
  const [editingCustomer, setEditingCustomer] = useState(null)
  const [filter, setFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    loadCustomerData()
  }, [])

  async function loadCustomerData() {
    setLoading(true)
    setError('')

    try {
      const nextCustomers = await getCustomers()
      setCustomers(nextCustomers)
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  const filterText = filter.trim().toLowerCase()
  const filteredCustomers = useMemo(
    () =>
      customers.filter((customer) => {
        const haystack = [
          customer.fullName,
          customer.phone,
          customer.email,
          customer.address,
          customer.status,
        ].join(' ').toLowerCase()
        return haystack.includes(filterText)
      }),
    [customers, filterText],
  )
  const customerPagination = usePagination(filteredCustomers, 10, filterText)

  function updateCustomerField(field, value) {
    setForm((currentForm) => ({ ...currentForm, [field]: value }))
  }

  function resetForm() {
    setForm(blankCustomerForm)
    setEditingCustomer(null)
    setError('')
    setSuccess('')
  }

  function startEdit(customer) {
    setEditingCustomer(customer)
    setError('')
    setSuccess('')
    setForm({
      fullName: customer.fullName || '',
      phone: customer.phone || '',
      email: customer.email || '',
      address: customer.address || '',
    })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!form.fullName.trim() || !form.phone.trim()) {
      setError('Full name and phone are required.')
      return
    }

    if (form.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      setError('Enter a valid email address.')
      return
    }

    const payload = {
      fullName: form.fullName.trim(),
      phone: form.phone.trim(),
      email: form.email.trim() || null,
      address: form.address.trim() || null,
    }

    setSaving(true)
    try {
      if (editingCustomer) {
        await updateCustomer(editingCustomer.id, payload)
        setSuccess('Customer updated.')
      } else {
        await createCustomer(payload)
        setSuccess('Customer created.')
      }
      setForm(blankCustomerForm)
      setEditingCustomer(null)
      await loadCustomerData()
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleStatusToggle(customer) {
    setError('')
    setSuccess('')
    const nextStatus = customer.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'

    try {
      await updateCustomerStatus(customer.id, nextStatus)
      setSuccess(`${customer.fullName} marked ${nextStatus.toLowerCase()}.`)
      await loadCustomerData()
    } catch (apiError) {
      setError(apiError.message)
    }
  }

  return (
    <div className="management-layout compact">
      <section className="data-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Customer records</p>
            <h2>Customers</h2>
          </div>
          <button type="button" onClick={loadCustomerData} disabled={loading}>Refresh</button>
        </div>

        <div className="table-toolbar">
          <input
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            placeholder="Search name, phone, email, address"
          />
          <span>{filteredCustomers.length} customers</span>
        </div>

        {error ? <p className="form-error">{error}</p> : null}
        {success ? <p className="form-success">{success}</p> : null}

        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Customer</th>
                <th>Phone</th>
                <th>Email</th>
                <th>Address</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6">Loading customers...</td>
                </tr>
              ) : filteredCustomers.length ? (
                customerPagination.pageItems.map((customer) => (
                  <tr key={customer.id}>
                    <td><strong>{customer.fullName}</strong></td>
                    <td>{customer.phone}</td>
                    <td>{customer.email || 'No email'}</td>
                    <td>{customer.address || 'No address'}</td>
                    <td><StatusPill status={customer.status} /></td>
                    <td>
                      <div className="action-row">
                        <button type="button" onClick={() => startEdit(customer)}>Edit</button>
                        <button type="button" onClick={() => handleStatusToggle(customer)}>
                          {customer.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6">No customers found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <PaginationControls {...customerPagination} />
      </section>

      <aside className="form-panel">
        <div className="panel-heading">
          <h2>{editingCustomer ? 'Edit Customer' : 'Add Customer'}</h2>
          {editingCustomer ? <button type="button" onClick={resetForm}>Cancel</button> : null}
        </div>

        <form className="management-form" onSubmit={handleSubmit}>
          <label>
            Full Name
            <input value={form.fullName} onChange={(event) => updateCustomerField('fullName', event.target.value)} />
          </label>

          <label>
            Phone
            <input value={form.phone} onChange={(event) => updateCustomerField('phone', event.target.value)} />
          </label>

          <label>
            Email
            <input
              type="email"
              value={form.email}
              onChange={(event) => updateCustomerField('email', event.target.value)}
            />
          </label>

          <label>
            Address
            <textarea value={form.address} onChange={(event) => updateCustomerField('address', event.target.value)} />
          </label>

          <button className="primary-button" type="submit" disabled={saving}>
            {saving ? 'Saving...' : editingCustomer ? 'Save Changes' : 'Create Customer'}
          </button>
        </form>
      </aside>
    </div>
  )
}


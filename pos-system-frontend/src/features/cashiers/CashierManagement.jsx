import { useEffect, useMemo, useState } from 'react'
import { createCashier, getCashiers, resetCashierPassword, updateCashier, updateCashierStatus } from '../../api'
import PaginationControls from '../../components/PaginationControls'
import StatusPill from '../../components/StatusPill'
import { usePagination } from '../../hooks/usePagination'
import { blankCashierForm } from '../../utils/formDefaults'

export default function CashierManagement() {
  const [cashiers, setCashiers] = useState([])
  const [form, setForm] = useState(blankCashierForm)
  const [editingCashier, setEditingCashier] = useState(null)
  const [passwordCashier, setPasswordCashier] = useState(null)
  const [newPassword, setNewPassword] = useState('')
  const [filter, setFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [resetting, setResetting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    loadCashierData()
  }, [])

  async function loadCashierData() {
    setLoading(true)
    setError('')

    try {
      const nextCashiers = await getCashiers()
      setCashiers(nextCashiers)
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  const filterText = filter.trim().toLowerCase()
  const filteredCashiers = useMemo(
    () =>
      cashiers.filter((cashier) => {
        const haystack = [
          cashier.fullName,
          cashier.username,
          cashier.email,
          cashier.roleName,
          cashier.status,
        ].join(' ').toLowerCase()
        return haystack.includes(filterText)
      }),
    [cashiers, filterText],
  )
  const cashierPagination = usePagination(filteredCashiers, 10, filterText)

  function updateCashierField(field, value) {
    setForm((currentForm) => ({ ...currentForm, [field]: value }))
  }

  function resetForm() {
    setForm(blankCashierForm)
    setEditingCashier(null)
    setError('')
    setSuccess('')
  }

  function startEdit(cashier) {
    setEditingCashier(cashier)
    setPasswordCashier(null)
    setNewPassword('')
    setError('')
    setSuccess('')
    setForm({
      fullName: cashier.fullName || '',
      username: cashier.username || '',
      email: cashier.email || '',
      password: '',
    })
  }

  function startPasswordReset(cashier) {
    setPasswordCashier(cashier)
    setEditingCashier(null)
    setForm(blankCashierForm)
    setNewPassword('')
    setError('')
    setSuccess('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!form.fullName.trim() || !form.username.trim() || !form.email.trim()) {
      setError('Full name, username, and email are required.')
      return
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      setError('Enter a valid email address.')
      return
    }

    if (!editingCashier && form.password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }

    const payload = {
      fullName: form.fullName.trim(),
      username: form.username.trim(),
      email: form.email.trim(),
    }

    setSaving(true)
    try {
      if (editingCashier) {
        await updateCashier(editingCashier.id, payload)
        setSuccess('Cashier updated.')
      } else {
        await createCashier({ ...payload, password: form.password })
        setSuccess('Cashier created.')
      }
      setForm(blankCashierForm)
      setEditingCashier(null)
      await loadCashierData()
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleStatusToggle(cashier) {
    setError('')
    setSuccess('')
    const nextStatus = cashier.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'

    try {
      await updateCashierStatus(cashier.id, nextStatus)
      setSuccess(`${cashier.fullName} marked ${nextStatus.toLowerCase()}.`)
      await loadCashierData()
    } catch (apiError) {
      setError(apiError.message)
    }
  }

  async function handlePasswordReset(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!passwordCashier) {
      setError('Select a cashier before resetting a password.')
      return
    }

    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters.')
      return
    }

    setResetting(true)
    try {
      await resetCashierPassword(passwordCashier.id, newPassword)
      setSuccess(`Password reset for ${passwordCashier.fullName}.`)
      setPasswordCashier(null)
      setNewPassword('')
      await loadCashierData()
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setResetting(false)
    }
  }

  return (
    <div className="management-layout compact">
      <section className="data-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Staff access</p>
            <h2>Cashiers</h2>
          </div>
          <button type="button" onClick={loadCashierData} disabled={loading}>Refresh</button>
        </div>

        <div className="table-toolbar">
          <input
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            placeholder="Search name, username, email, status"
          />
          <span>{filteredCashiers.length} cashiers</span>
        </div>

        {error ? <p className="form-error">{error}</p> : null}
        {success ? <p className="form-success">{success}</p> : null}

        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Cashier</th>
                <th>Username</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6">Loading cashiers...</td>
                </tr>
              ) : filteredCashiers.length ? (
                cashierPagination.pageItems.map((cashier) => (
                  <tr key={cashier.id}>
                    <td><strong>{cashier.fullName}</strong></td>
                    <td>{cashier.username}</td>
                    <td>{cashier.email}</td>
                    <td>{cashier.roleName || 'CASHIER'}</td>
                    <td><StatusPill status={cashier.status} /></td>
                    <td>
                      <div className="action-row">
                        <button type="button" onClick={() => startEdit(cashier)}>Edit</button>
                        <button type="button" onClick={() => startPasswordReset(cashier)}>Password</button>
                        <button type="button" onClick={() => handleStatusToggle(cashier)}>
                          {cashier.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6">No cashiers found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <PaginationControls {...cashierPagination} />
      </section>

      <aside className="form-panel">
        <div className="panel-heading">
          <h2>{editingCashier ? 'Edit Cashier' : 'Add Cashier'}</h2>
          {editingCashier ? <button type="button" onClick={resetForm}>Cancel</button> : null}
        </div>

        <form className="management-form" onSubmit={handleSubmit}>
          <label>
            Full Name
            <input value={form.fullName} onChange={(event) => updateCashierField('fullName', event.target.value)} />
          </label>

          <label>
            Username
            <input value={form.username} onChange={(event) => updateCashierField('username', event.target.value)} />
          </label>

          <label>
            Email
            <input
              type="email"
              value={form.email}
              onChange={(event) => updateCashierField('email', event.target.value)}
            />
          </label>

          {!editingCashier ? (
            <label>
              Password
              <input
                type="password"
                value={form.password}
                onChange={(event) => updateCashierField('password', event.target.value)}
              />
            </label>
          ) : null}

          <button className="primary-button" type="submit" disabled={saving}>
            {saving ? 'Saving...' : editingCashier ? 'Save Changes' : 'Create Cashier'}
          </button>
        </form>

        <div className="side-section">
          <div className="panel-heading">
            <h2>Password Reset</h2>
            {passwordCashier ? <button type="button" onClick={() => setPasswordCashier(null)}>Cancel</button> : null}
          </div>

          {passwordCashier ? (
            <form className="management-form" onSubmit={handlePasswordReset}>
              <p className="empty-state">Reset password for {passwordCashier.fullName}.</p>
              <label>
                New Password
                <input
                  type="password"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                />
              </label>
              <button className="primary-button" type="submit" disabled={resetting}>
                {resetting ? 'Resetting...' : 'Reset Password'}
              </button>
            </form>
          ) : (
            <p className="empty-state">Select Password on a cashier row.</p>
          )}
        </div>
      </aside>
    </div>
  )
}


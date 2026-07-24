import { useEffect, useMemo, useState } from 'react'
import {
  assetUrl,
  changeAdminPassword,
  getSettings,
  updateReceiptSettings,
  updateStoreSettings,
  uploadStoreLogo,
} from '../../api'

const blankStore = {
  storeName: '',
  address: '',
  phone: '',
  email: '',
  logoUrl: '',
}

const blankReceipt = {
  headerText: '',
  footerText: '',
  taxPercentage: '0.00',
  currencySymbol: 'Rs',
  showCashierName: true,
  showCustomerInfo: true,
}

const blankPassword = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
}

export default function SettingsManagement() {
  const [activeTab, setActiveTab] = useState('Store Profile')
  const [storeForm, setStoreForm] = useState(blankStore)
  const [receiptForm, setReceiptForm] = useState(blankReceipt)
  const [passwordForm, setPasswordForm] = useState(blankPassword)
  const [logoFile, setLogoFile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const selectedLogoPreview = useMemo(
    () => (logoFile ? URL.createObjectURL(logoFile) : ''),
    [logoFile],
  )
  const logoPreview = selectedLogoPreview || (storeForm.logoUrl ? assetUrl(storeForm.logoUrl) : '')

  useEffect(() => {
    loadSettings()
  }, [])

  useEffect(() => {
    return () => {
      if (selectedLogoPreview) {
        URL.revokeObjectURL(selectedLogoPreview)
      }
    }
  }, [selectedLogoPreview])

  async function loadSettings() {
    setLoading(true)
    setError('')

    try {
      const settings = await getSettings()
      setStoreForm({ ...blankStore, ...settings.store })
      setReceiptForm({
        ...blankReceipt,
        ...settings.receipt,
        taxPercentage: String(settings.receipt?.taxPercentage ?? '0.00'),
      })
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  function updateStoreField(field, value) {
    setStoreForm((currentForm) => ({ ...currentForm, [field]: value }))
  }

  function updateReceiptField(field, value) {
    setReceiptForm((currentForm) => ({ ...currentForm, [field]: value }))
  }

  function updatePasswordField(field, value) {
    setPasswordForm((currentForm) => ({ ...currentForm, [field]: value }))
  }

  function handleLogoChange(event) {
    const file = event.target.files?.[0] || null

    if (!file) {
      setLogoFile(null)
      return
    }

    if (!file.type.startsWith('image/')) {
      setError('Please choose a valid image file for the store logo.')
      event.target.value = ''
      setLogoFile(null)
      return
    }

    setError('')
    setLogoFile(file)
  }

  async function handleStoreSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!storeForm.storeName.trim()) {
      setError('Store name is required.')
      return
    }

    setSaving('store')
    try {
      let savedStore = await updateStoreSettings({
        storeName: storeForm.storeName.trim(),
        address: storeForm.address.trim(),
        phone: storeForm.phone.trim(),
        email: storeForm.email.trim(),
        logoUrl: storeForm.logoUrl.trim(),
      })

      if (logoFile) {
        savedStore = await uploadStoreLogo(logoFile)
      }

      setStoreForm({ ...blankStore, ...savedStore })
      setLogoFile(null)
      window.dispatchEvent(new CustomEvent('pos-settings-updated'))
      setSuccess('Store profile saved.')
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving('')
    }
  }

  async function handleReceiptSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    const taxPercentage = Number(receiptForm.taxPercentage)

    if (!receiptForm.currencySymbol.trim()) {
      setError('Currency symbol is required.')
      return
    }

    if (Number.isNaN(taxPercentage) || taxPercentage < 0 || taxPercentage > 100) {
      setError('Tax percentage must be between 0 and 100.')
      return
    }

    setSaving('receipt')
    try {
      const savedReceipt = await updateReceiptSettings({
        headerText: receiptForm.headerText.trim(),
        footerText: receiptForm.footerText.trim(),
        taxPercentage,
        currencySymbol: receiptForm.currencySymbol.trim(),
        showCashierName: receiptForm.showCashierName,
        showCustomerInfo: receiptForm.showCustomerInfo,
      })
      setReceiptForm({
        ...blankReceipt,
        ...savedReceipt,
        taxPercentage: String(savedReceipt.taxPercentage ?? '0.00'),
      })
      setSuccess('Receipt settings saved.')
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving('')
    }
  }

  async function handlePasswordSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (passwordForm.newPassword.length < 8) {
      setError('New password must be at least 8 characters.')
      return
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setError('Confirm password does not match.')
      return
    }

    setSaving('password')
    try {
      await changeAdminPassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      })
      setPasswordForm(blankPassword)
      setSuccess('Admin password changed.')
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving('')
    }
  }

  return (
    <div className="settings-page">
      <section className="settings-header">
        <div>
          <p className="eyebrow">System setup</p>
          <h2>Settings</h2>
        </div>
        <button type="button" onClick={loadSettings} disabled={loading}>Refresh</button>
      </section>

      <div className="settings-tabs" role="tablist" aria-label="Settings sections">
        {['Store Profile', 'Receipt', 'Account'].map((tab) => (
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

      {loading ? (
        <section className="panel">
          <p className="empty-state">Loading settings...</p>
        </section>
      ) : null}

      {!loading && activeTab === 'Store Profile' ? (
        <section className="settings-panel">
          <form className="management-form" onSubmit={handleStoreSubmit}>
            <div className="form-grid-two">
              <label>
                Store Name
                <input value={storeForm.storeName} onChange={(event) => updateStoreField('storeName', event.target.value)} />
              </label>
              <label>
                Phone
                <input value={storeForm.phone} onChange={(event) => updateStoreField('phone', event.target.value)} />
              </label>
            </div>

            <label>
              Address
              <textarea value={storeForm.address} onChange={(event) => updateStoreField('address', event.target.value)} />
            </label>

            <div className="form-grid-two">
              <label>
                Email
                <input type="email" value={storeForm.email} onChange={(event) => updateStoreField('email', event.target.value)} />
              </label>
              <label>
                Store Logo
                <input accept="image/*" type="file" onChange={handleLogoChange} />
              </label>
            </div>

            {logoPreview ? (
              <div className="store-logo-preview">
                <img src={logoPreview} alt="Store logo preview" />
                <span>{logoFile ? logoFile.name : 'Current logo'}</span>
              </div>
            ) : null}

            <button className="primary-button" type="submit" disabled={saving === 'store'}>
              {saving === 'store' ? 'Saving...' : 'Save Store Profile'}
            </button>
          </form>
        </section>
      ) : null}

      {!loading && activeTab === 'Receipt' ? (
        <section className="settings-panel">
          <form className="management-form" onSubmit={handleReceiptSubmit}>
            <label>
              Header Text
              <textarea
                value={receiptForm.headerText}
                onChange={(event) => updateReceiptField('headerText', event.target.value)}
              />
            </label>

            <label>
              Footer Text
              <textarea
                value={receiptForm.footerText}
                onChange={(event) => updateReceiptField('footerText', event.target.value)}
              />
            </label>

            <div className="form-grid-two">
              <label>
                Tax Percentage
                <input
                  min="0"
                  max="100"
                  step="0.01"
                  type="number"
                  value={receiptForm.taxPercentage}
                  onChange={(event) => updateReceiptField('taxPercentage', event.target.value)}
                />
              </label>
              <label>
                Currency Symbol
                <input
                  value={receiptForm.currencySymbol}
                  onChange={(event) => updateReceiptField('currencySymbol', event.target.value)}
                />
              </label>
            </div>

            <div className="settings-checks">
              <label>
                <input
                  checked={receiptForm.showCashierName}
                  type="checkbox"
                  onChange={(event) => updateReceiptField('showCashierName', event.target.checked)}
                />
                Show cashier name on receipt
              </label>
              <label>
                <input
                  checked={receiptForm.showCustomerInfo}
                  type="checkbox"
                  onChange={(event) => updateReceiptField('showCustomerInfo', event.target.checked)}
                />
                Show customer info on receipt
              </label>
            </div>

            <button className="primary-button" type="submit" disabled={saving === 'receipt'}>
              {saving === 'receipt' ? 'Saving...' : 'Save Receipt Settings'}
            </button>
          </form>
        </section>
      ) : null}

      {!loading && activeTab === 'Account' ? (
        <section className="settings-panel narrow">
          <form className="management-form" onSubmit={handlePasswordSubmit}>
            <label>
              Current Password
              <input
                type="password"
                value={passwordForm.currentPassword}
                onChange={(event) => updatePasswordField('currentPassword', event.target.value)}
              />
            </label>
            <label>
              New Password
              <input
                type="password"
                value={passwordForm.newPassword}
                onChange={(event) => updatePasswordField('newPassword', event.target.value)}
              />
            </label>
            <label>
              Confirm New Password
              <input
                type="password"
                value={passwordForm.confirmPassword}
                onChange={(event) => updatePasswordField('confirmPassword', event.target.value)}
              />
            </label>
            <button className="primary-button" type="submit" disabled={saving === 'password'}>
              {saving === 'password' ? 'Changing...' : 'Change Password'}
            </button>
          </form>
        </section>
      ) : null}
    </div>
  )
}

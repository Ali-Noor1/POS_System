import { useEffect, useState } from 'react'
import { assetUrl, getSettings } from '../../api'

export default function CashierShell({ activePage, children, onLogout, onNavigate, session }) {
  const items = [
    ['Checkout', 'Checkout', 'cart'],
    ['Sales History', 'History', 'clock'],
    ['Receipt', 'Receipts', 'receipt'],
  ]
  const [storeBrand, setStoreBrand] = useState({ storeName: 'Retail POS', logoUrl: '' })
  const [logoFailed, setLogoFailed] = useState(false)
  const storeName = storeBrand.storeName?.trim() || 'Retail POS'
  const logoUrl = storeBrand.logoUrl?.trim()
  const showLogo = logoUrl && !logoFailed

  useEffect(() => {
    let isMounted = true

    async function loadStoreBrand() {
      try {
        const settings = await getSettings()
        if (isMounted) {
          setStoreBrand({
            storeName: settings.store?.storeName || 'Retail POS',
            logoUrl: settings.store?.logoUrl || '',
          })
          setLogoFailed(false)
        }
      } catch {
        if (isMounted) {
          setStoreBrand({ storeName: 'Retail POS', logoUrl: '' })
          setLogoFailed(false)
        }
      }
    }

    loadStoreBrand()
    window.addEventListener('pos-settings-updated', loadStoreBrand)

    return () => {
      isMounted = false
      window.removeEventListener('pos-settings-updated', loadStoreBrand)
    }
  }, [session?.token])

  return (
    <main className="cashier-shell">
      <aside className="cashier-sidebar">
        <div className="cashier-brand">
          <span className={`cashier-brand-icon${showLogo ? ' has-logo' : ''}`}>
            {showLogo ? (
              <img src={assetUrl(logoUrl)} alt={`${storeName} logo`} onError={() => setLogoFailed(true)} />
            ) : (
              'RP'
            )}
          </span>
          <strong>{storeName}</strong>
        </div>

        <nav className="cashier-nav">
          {items.map(([item, label, icon]) => (
            <button
              className={activePage === item ? 'active' : ''}
              key={item}
              type="button"
              onClick={() => {
                onNavigate(item)
              }}
            >
              <span className="cashier-nav-icon" aria-hidden="true">
                {icon === 'cart' ? (
                  <svg viewBox="0 0 24 24">
                    <path d="M4 5h2l2.1 9.2a2 2 0 0 0 2 1.6h6.8a2 2 0 0 0 1.9-1.4L20 9H8" />
                    <circle cx="10" cy="20" r="1.4" />
                    <circle cx="17" cy="20" r="1.4" />
                  </svg>
                ) : icon === 'clock' ? (
                  <svg viewBox="0 0 24 24">
                    <circle cx="12" cy="12" r="8" />
                    <path d="M12 8v5l3 2" />
                  </svg>
                ) : (
                  <svg viewBox="0 0 24 24">
                    <path d="M7 4h10v16l-2-1-2 1-2-1-2 1-2-1V4z" />
                    <path d="M9 8h6M9 12h6M9 16h4" />
                  </svg>
                )}
              </span>
              <span>{label}</span>
            </button>
          ))}
        </nav>

        <div className="cashier-terminal">
          <span>Terminal: POS-01</span>
          <span>Date: {new Date().toLocaleDateString()}</span>
          <span>Time: {new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
          <strong>Online</strong>
        </div>
      </aside>

      <section className="cashier-main">
        <header className="cashier-topbar">
          <button className="icon-button" type="button" aria-label="Menu">=</button>
          <h1>{activePage}</h1>
          <div className="cashier-topbar-actions">
            <span className="shift-badge">Shift Open</span>
            <span className="cashier-time">{new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
            <span className="cashier-avatar">{(session.fullName || session.username || 'C').slice(0, 1)}</span>
            <span className="cashier-name">
              <small>Cashier</small>
              <strong>{session.fullName || session.username}</strong>
            </span>
            <button className="icon-button" type="button" onClick={onLogout} aria-label="Logout">Logout</button>
          </div>
        </header>

        {children}
      </section>
    </main>
  )
}

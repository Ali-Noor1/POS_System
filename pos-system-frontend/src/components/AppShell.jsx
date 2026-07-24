
import { useEffect, useState } from 'react'
import { assetUrl, getSettings } from '../api'

export default function AppShell({ activePage, children, onLogout, onNavigate, role, session }) {
  const adminItems = ['Dashboard', 'Products', 'Categories', 'Inventory', 'Suppliers', 'Customers', 'Sales History', 'Reports', 'Cashiers']
  const cashierItems = ['Checkout', 'Sales History', 'Receipt']
  const items = role === 'ADMIN' ? adminItems : cashierItems
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
    <main className={`app-shell ${role === 'ADMIN' ? 'admin-shell' : ''}`}>
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className={`brand-mark${showLogo ? ' has-logo' : ''}`}>
            {showLogo ? (
              <img src={assetUrl(logoUrl)} alt={`${storeName} logo`} onError={() => setLogoFailed(true)} />
            ) : (
              'RP'
            )}
          </span>
          <strong>{storeName}</strong>
        </div>
        <nav className="sidebar-nav">
          {items.map((item) => (
            <button
              className={activePage === item ? 'active' : ''}
              key={item}
              type="button"
              onClick={() => onNavigate(item)}
            >
              <NavIcon name={item} />
              <strong>{item}</strong>
            </button>
          ))}
        </nav>
        {role === 'ADMIN' ? (
          <div className="sidebar-footer">
            <button
              className={activePage === 'Settings' ? 'active' : ''}
              type="button"
              onClick={() => onNavigate('Settings')}
            >
              <NavIcon name="Settings" />
              <strong>Settings</strong>
            </button>
            <button type="button" onClick={onLogout}>
              <NavIcon name="Logout" />
              <strong>Logout</strong>
            </button>
          </div>
        ) : null}
      </aside>

      <section className="workspace">
        <header className="topbar">
          <button className="menu-button" type="button" aria-label="Menu">=</button>
          <h1>{activePage}</h1>
          {role === 'ADMIN' ? (
            <label className="admin-top-search">
              <span>Search</span>
              <input placeholder="Search customers, products..." />
            </label>
          ) : null}
          <div className="user-chip">
            <i>{String(session.fullName || session.username || 'U').slice(0, 1).toUpperCase()}</i>
            <span>
              <strong>{session.fullName || session.username}</strong>
              <small>{role === 'ADMIN' ? 'Admin' : 'Cashier'}</small>
            </span>
            {role !== 'ADMIN' ? <button type="button" onClick={onLogout}>Logout</button> : null}
          </div>
        </header>
        {children}
      </section>
    </main>
  )
}

function NavIcon({ name }) {
  const paths = {
    Dashboard: (
      <>
        <path d="M3 10.5 12 3l9 7.5" />
        <path d="M5 9.5V21h14V9.5" />
        <path d="M9.5 21v-6h5v6" />
      </>
    ),
    Products: (
      <>
        <path d="M20 10 12 20 4 10l8-7 8 7Z" />
        <path d="M8.5 10h7" />
      </>
    ),
    Categories: (
      <>
        <path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z" />
        <path d="M12 12 4.5 7.8" />
        <path d="M12 12v8.5" />
        <path d="m12 12 7.5-4.2" />
      </>
    ),
    Inventory: (
      <>
        <path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z" />
        <path d="M8 10h8" />
        <path d="M8 14h8" />
      </>
    ),
    Customers: (
      <>
        <path d="M8 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
        <path d="M16 12a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z" />
        <path d="M3 20a5 5 0 0 1 10 0" />
        <path d="M13.5 18.5A4 4 0 0 1 21 20" />
      </>
    ),
    Suppliers: (
      <>
        <path d="M4 7h10v10H4z" />
        <path d="M14 10h3l3 3v4h-6z" />
        <path d="M7 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z" />
        <path d="M17 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z" />
      </>
    ),
    'Sales History': (
      <>
        <path d="M4 5h3l2 11h9l2-8H8" />
        <path d="M10 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z" />
        <path d="M17 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z" />
      </>
    ),
    Reports: (
      <>
        <path d="M5 20V10" />
        <path d="M12 20V4" />
        <path d="M19 20v-7" />
      </>
    ),
    Cashiers: (
      <>
        <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
        <path d="M5 21a7 7 0 0 1 14 0" />
      </>
    ),
    Settings: (
      <>
        <path d="M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z" />
        <path d="M19 12a7 7 0 0 0-.1-1.2l2-1.5-2-3.4-2.4 1a7 7 0 0 0-2-1.2L14 3h-4l-.5 2.7a7 7 0 0 0-2 1.2l-2.4-1-2 3.4 2 1.5a7.6 7.6 0 0 0 0 2.4l-2 1.5 2 3.4 2.4-1a7 7 0 0 0 2 1.2L10 21h4l.5-2.7a7 7 0 0 0 2-1.2l2.4 1 2-3.4-2-1.5A7 7 0 0 0 19 12Z" />
      </>
    ),
    Logout: (
      <>
        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
        <path d="M16 17l5-5-5-5" />
        <path d="M21 12H9" />
      </>
    ),
    Checkout: (
      <>
        <path d="M4 5h3l2 11h9l2-8H8" />
        <path d="M10 20h.1" />
        <path d="M17 20h.1" />
      </>
    ),
    Receipt: (
      <>
        <path d="M6 3h12v18l-3-2-3 2-3-2-3 2V3Z" />
        <path d="M9 8h6" />
        <path d="M9 12h6" />
      </>
    ),
  }

  return (
    <svg className="nav-icon" viewBox="0 0 24 24" aria-hidden="true">
      {paths[name] || paths.Dashboard}
    </svg>
  )
}


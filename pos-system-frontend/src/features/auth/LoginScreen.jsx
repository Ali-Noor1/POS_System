import { useState } from 'react'
import { login } from '../../api'
import heroImage from '../../assets/login-pos-terminal-cutout.png'

export default function LoginScreen({ onLogin }) {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('Admin@123')
  const [showPassword, setShowPassword] = useState(false)
  const [selectedRole, setSelectedRole] = useState('Admin')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      const nextSession = await login(username.trim(), password)
      onLogin(nextSession)
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  function selectRole(role) {
    setSelectedRole(role)

    if (role === 'Admin') {
      setUsername('admin')
      setPassword('Admin@123')
      return
    }

    setUsername('cashier')
    setPassword('Cashier@123')
  }

  return (
    <main className="login-screen">
      <section className="login-showcase" aria-label="Retail POS overview">
        <div className="login-brand">
          <span className="login-logo">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M7 9V7a5 5 0 0 1 10 0v2" />
              <path d="M5 9h14l-1 10H6L5 9Z" />
              <path d="M8.4 14.2c1.4-1.7 2.9-1.7 4.3 0 1.4 1.7 2.9 1.7 4.3 0" />
            </svg>
            <small>POS</small>
          </span>
          <div>
            <h1>Ali Khan <span>Store</span></h1>
            <p>Retail POS System</p>
          </div>
        </div>

        <div className="login-welcome">
          <h2>Welcome Back!</h2>
          <i aria-hidden="true" />
          <p>Please sign in to continue<br />to your account</p>
        </div>

        <div
          className="login-hero-wrap"
          style={{ '--login-hero-image': `url(${JSON.stringify(heroImage)})` }}
          role="img"
          aria-label="Retail POS terminal with barcode scanner and receipt printer"
        />

        <div className="login-feature-row">
          <article>
            <span className="feature-icon sales-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M5 16l4-4 3 3 6-7" />
                <path d="M18 8v5h-5" />
              </svg>
            </span>
            <strong>Manage Sales</strong>
            <p>Track your daily sales and profit</p>
          </article>
          <article>
            <span className="feature-icon inventory-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 3 4 7l8 4 8-4-8-4Z" />
                <path d="M4 7v10l8 4 8-4V7" />
                <path d="M12 11v10" />
              </svg>
            </span>
            <strong>Manage Inventory</strong>
            <p>Keep track of stock and categories</p>
          </article>
          <article>
            <span className="feature-icon customers-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M16 11a4 4 0 1 0-8 0" />
                <path d="M4 20a8 8 0 0 1 16 0" />
                <path d="M18 8a3 3 0 0 1 3 3" />
              </svg>
            </span>
            <strong>Manage Customers</strong>
            <p>Build better relationships with your customers</p>
          </article>
        </div>
      </section>

      <section className="login-panel">
        <div className="login-card-icon">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M8 10V8a4 4 0 0 1 8 0v2" />
            <path d="M7 10h10v9H7z" />
            <path d="M12 14v2" />
          </svg>
        </div>

        <div className="login-card-heading">
          <h2>Sign in to your account</h2>
          <p>Enter your credentials to access your account</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            Username
            <span className="login-input-wrap">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
                <path d="M4 21a8 8 0 0 1 16 0" />
              </svg>
              <input
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                placeholder="Enter your username"
              />
            </span>
          </label>

          <label>
            Password
            <span className="login-input-wrap">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M8 11V8a4 4 0 0 1 8 0v3" />
                <path d="M6 11h12v9H6z" />
              </svg>
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                placeholder="Enter your password"
              />
              <button type="button" className="show-password" onClick={() => setShowPassword(!showPassword)}>
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </span>
          </label>

          <div className="login-options">
            <label className="remember-option">
              <input type="checkbox" defaultChecked />
              <span>Remember Me</span>
            </label>
            <button type="button">Forgot Password?</button>
          </div>

          {error ? <p className="form-error">{error}</p> : null}

          <button className="primary-button" type="submit" disabled={loading}>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M8 11V8a4 4 0 0 1 8 0v3" />
              <path d="M6 11h12v9H6z" />
            </svg>
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div className="login-divider"><span>OR</span></div>

        <p className="role-title">Login as</p>
        <div className="credential-row">
          <button
            className={selectedRole === 'Cashier' ? 'active' : ''}
            type="button"
            onClick={() => selectRole('Cashier')}
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
              <path d="M4 21a8 8 0 0 1 16 0" />
            </svg>
            Cashier
          </button>
          <button
            className={selectedRole === 'Admin' ? 'active' : ''}
            type="button"
            onClick={() => selectRole('Admin')}
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 3 5 6v6c0 4 3 7 7 9 4-2 7-5 7-9V6l-7-3Z" />
              <path d="M9.5 12.5 11 14l3.5-4" />
            </svg>
            Admin
          </button>
        </div>

        <footer className="login-footer">
          <span>© 2026 Ali Khan Store. All rights reserved.</span>
          <span>Version 1.0.0</span>
        </footer>
      </section>
    </main>
  )
}

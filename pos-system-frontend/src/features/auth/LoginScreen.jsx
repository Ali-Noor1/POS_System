import { useState } from 'react'
import { login } from '../../api'

export default function LoginScreen({ onLogin }) {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('Admin@123')
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

  return (
    <main className="login-screen">
      <section className="login-panel">
        <div className="brand-lockup">
          <span className="brand-mark">RP</span>
          <div>
            <p className="eyebrow">Retail POS</p>
            <h1>Sign in</h1>
          </div>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            Username
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
            />
          </label>

          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
            />
          </label>

          {error ? <p className="form-error">{error}</p> : null}

          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <div className="credential-row">
          <button type="button" onClick={() => {
            setUsername('admin')
            setPassword('Admin@123')
          }}>
            Admin
          </button>
          <button type="button" onClick={() => {
            setUsername('cashier')
            setPassword('Cashier@123')
          }}>
            Cashier
          </button>
        </div>
      </section>
    </main>
  )
}


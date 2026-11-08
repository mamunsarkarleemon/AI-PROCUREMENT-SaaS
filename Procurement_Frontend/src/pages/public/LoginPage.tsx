import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { errorMessage } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { Alert, PasswordField, TextField } from '../../components/ui'

const DEMO = [
  { label: 'Admin', email: 'admin@procureai.com', password: 'Admin@123' },
  { label: 'Buyer', email: 'buyer1@demo.com', password: 'Demo@123' },
  { label: 'Supplier', email: 'supplier1@demo.com', password: 'Demo@123' },
]

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const state = (useLocation().state ?? {}) as { from?: string; registeredEmail?: string }
  const [email, setEmail] = useState(state.registeredEmail ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!email.trim() || !password) {
      setError('Enter your email and password.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await login(email.trim(), password)
      navigate(state.from ?? '/app/dashboard', { replace: true })
    } catch (err) {
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <div className="auth-card card">
      <h1>Welcome back</h1>
      <p className="muted">Log in to your ProcureAI account.</p>
      {state.registeredEmail && !error && <Alert tone="success">Account created! Log in to continue.</Alert>}
      {error && <Alert tone="error">{error}</Alert>}
      <form className="form" onSubmit={submit} noValidate>
        <TextField label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus={!state.registeredEmail} />
        <PasswordField label="Password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} autoFocus={!!state.registeredEmail} />
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button>
      </form>
      <p className="auth-switch">
        New to ProcureAI? <Link to="/register">Create an account</Link>
      </p>
      <div className="demo-box">
        <p><b>Demo accounts</b> (sample data, click to fill)</p>
        <div className="demo-buttons">
          {DEMO.map((d) => (
            <button key={d.label} type="button" className="btn btn-ghost btn-sm" onClick={() => { setEmail(d.email); setPassword(d.password) }}>
              {d.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

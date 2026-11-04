import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Alert } from '../components/Alert'
import { AuthLayout } from '../components/AuthLayout'
import { PasswordField, TextField } from '../components/FormField'

interface LocationState {
  from?: string
  registeredEmail?: string
}

export function LoginPage() {
  const { login, logoutReason } = useAuth()
  const navigate = useNavigate()
  const state = (useLocation().state ?? {}) as LocationState

  const [email, setEmail] = useState(state.registeredEmail ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!email.trim() || !password) {
      setError('Enter your email and password.')
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      await login({ email: email.trim(), password })
      navigate(state.from ?? '/dashboard', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Sign in"
      subtitle={
        <>
          New to ProcureAI? <Link to="/register">Create an account</Link>
        </>
      }
    >
      {state.registeredEmail && !error && (
        <Alert tone="success">Your account was created. Sign in to continue.</Alert>
      )}
      {logoutReason && !state.registeredEmail && !error && <Alert tone="info">{logoutReason}</Alert>}
      {error && <Alert tone="error">{error}</Alert>}

      <form onSubmit={handleSubmit} noValidate className="form">
        <TextField
          label="Work email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoFocus={!state.registeredEmail}
          required
        />
        <PasswordField
          label="Password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoFocus={Boolean(state.registeredEmail)}
          required
        />
        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </AuthLayout>
  )
}

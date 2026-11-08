import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { Factory, ShoppingCart } from 'lucide-react'
import { ApiError, errorMessage } from '../../api/client'
import { authApi } from '../../api/endpoints'
import { Alert, PasswordField, TextField } from '../../components/ui'

type Role = 'BUYER' | 'SUPPLIER'

const ROLES: { value: Role; title: string; text: string; icon: typeof Factory }[] = [
  { value: 'BUYER', title: 'I want to buy', text: 'Find products, post requests, negotiate and order.', icon: ShoppingCart },
  { value: 'SUPPLIER', title: 'I want to sell', text: 'List products, send quotes and receive orders.', icon: Factory },
]

export function RegisterPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    role: (params.get('role') === 'SUPPLIER' ? 'SUPPLIER' : 'BUYER') as Role,
    fullName: '',
    email: '',
    password: '',
    confirm: '',
    phone: '',
    companyName: '',
    city: 'Doha',
    country: 'Qatar',
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => {
    setForm((f) => ({ ...f, [k]: e.target.value }))
    setErrors((er) => ({ ...er, [k]: '' }))
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    const errs: Record<string, string> = {}
    if (!form.fullName.trim()) errs.fullName = 'Enter your full name.'
    if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) errs.email = 'Enter a valid email.'
    if (form.password.length < 6) errs.password = 'Use at least 6 characters.'
    if (form.confirm !== form.password) errs.confirm = 'Passwords do not match.'
    if (!form.companyName.trim()) errs.companyName = 'Enter your company name.'
    setErrors(errs)
    if (Object.keys(errs).length) {
      setError('Please fix the highlighted fields.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const user = await authApi.register({
        role: form.role,
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
        phone: form.phone.trim(),
        companyName: form.companyName.trim(),
        city: form.city.trim(),
        country: form.country.trim(),
      })
      navigate('/login', { replace: true, state: { registeredEmail: user.email } })
    } catch (err) {
      if (err instanceof ApiError) setErrors(err.fieldErrors)
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <div className="auth-card card auth-card-wide">
      <h1>Create your account</h1>
      <p className="muted">Free for buyers and suppliers. It takes less than a minute.</p>
      {error && <Alert tone="error">{error}</Alert>}
      <form className="form" onSubmit={submit} noValidate>
        <fieldset className="role-picker">
          <legend>I am a…</legend>
          <div className="role-options">
            {ROLES.map((r) => (
              <label key={r.value} className={`role-option${form.role === r.value ? ' selected' : ''}`}>
                <input type="radio" name="role" value={r.value} checked={form.role === r.value} onChange={() => setForm((f) => ({ ...f, role: r.value }))} />
                <r.icon size={22} aria-hidden="true" />
                <span><b>{r.value === 'BUYER' ? 'Buyer' : 'Supplier'}</b> · {r.title}<small>{r.text}</small></span>
              </label>
            ))}
          </div>
        </fieldset>

        <div className="form-row">
          <TextField label="Full name" autoComplete="name" value={form.fullName} onChange={set('fullName')} error={errors.fullName} required />
          <TextField label="Phone" type="tel" autoComplete="tel" value={form.phone} onChange={set('phone')} placeholder="+974 ..." />
        </div>
        <TextField label="Email" type="email" autoComplete="email" value={form.email} onChange={set('email')} error={errors.email} required />
        <div className="form-row">
          <PasswordField label="Password" autoComplete="new-password" value={form.password} onChange={set('password')} error={errors.password} hint="At least 6 characters." required />
          <PasswordField label="Confirm password" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} required />
        </div>
        <TextField label="Company name" autoComplete="organization" value={form.companyName} onChange={set('companyName')} error={errors.companyName} required />
        <div className="form-row">
          <TextField label="City" value={form.city} onChange={set('city')} />
          <TextField label="Country" value={form.country} onChange={set('country')} />
        </div>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Creating account…' : `Create ${form.role === 'BUYER' ? 'buyer' : 'supplier'} account`}</button>
      </form>
      <p className="auth-switch">
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </div>
  )
}

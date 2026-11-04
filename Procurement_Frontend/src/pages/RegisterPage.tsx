import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { authApi } from '../api/auth'
import { ApiError } from '../api/client'
import type { AccountType, RegisterRequest } from '../api/types'
import { Alert } from '../components/Alert'
import { AuthLayout } from '../components/AuthLayout'
import { FieldShell, PasswordField, TextField } from '../components/FormField'

const COUNTRIES: { code: string; name: string }[] = [
  { code: 'QA', name: 'Qatar' },
  { code: 'AE', name: 'United Arab Emirates' },
  { code: 'SA', name: 'Saudi Arabia' },
  { code: 'KW', name: 'Kuwait' },
  { code: 'BH', name: 'Bahrain' },
  { code: 'OM', name: 'Oman' },
  { code: 'BD', name: 'Bangladesh' },
  { code: 'CN', name: 'China' },
  { code: 'IN', name: 'India' },
  { code: 'PK', name: 'Pakistan' },
  { code: 'TR', name: 'Türkiye' },
  { code: 'DE', name: 'Germany' },
  { code: 'IT', name: 'Italy' },
  { code: 'GB', name: 'United Kingdom' },
  { code: 'US', name: 'United States' },
]

const ACCOUNT_TYPES: { value: AccountType; title: string; text: string }[] = [
  { value: 'BUYER', title: 'I buy', text: 'Source products, send RFQs and compare quotes.' },
  { value: 'SUPPLIER', title: 'I supply', text: 'List products, receive RFQs and submit quotes.' },
]

interface FormState extends Required<RegisterRequest> {
  confirmPassword: string
}

type Errors = Partial<Record<keyof FormState, string>>

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Mirrors the backend rules so users get instant feedback. The server re-validates everything. */
function validate(f: FormState): Errors {
  const e: Errors = {}
  if (!f.fullName.trim()) e.fullName = 'Enter your full name.'
  if (!EMAIL_RE.test(f.email.trim())) e.email = 'Enter a valid email address.'
  if (f.password.length < 8 || f.password.length > 72) e.password = 'Use 8 to 72 characters.'
  else if (!/[A-Za-z]/.test(f.password) || !/\d/.test(f.password)) e.password = 'Include at least one letter and one number.'
  if (f.confirmPassword !== f.password) e.confirmPassword = 'Passwords do not match.'
  if (!f.companyName.trim()) e.companyName = 'Enter your company name.'
  if (!/^[A-Z]{2}$/.test(f.countryCode)) e.countryCode = 'Choose a country.'
  return e
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<FormState>({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
    accountType: 'BUYER',
    companyName: '',
    countryCode: 'QA',
    registrationNo: '',
  })
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }))
    if (errors[key]) setErrors((e) => ({ ...e, [key]: undefined }))
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const clientErrors = validate(form)
    setErrors(clientErrors)
    if (Object.keys(clientErrors).length > 0) {
      setFormError('Please fix the highlighted fields.')
      return
    }

    setFormError(null)
    setSubmitting(true)
    try {
      const user = await authApi.register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
        accountType: form.accountType,
        companyName: form.companyName.trim(),
        countryCode: form.countryCode,
        registrationNo: form.registrationNo.trim() || undefined,
      })
      navigate('/login', { replace: true, state: { registeredEmail: user.email } })
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === 'EMAIL_ALREADY_REGISTERED') setErrors({ email: err.message })
        else if (err.code === 'COMPANY_ALREADY_REGISTERED') setErrors({ registrationNo: err.message })
        else setErrors(err.fieldErrors as Errors)
        setFormError(err.message)
      } else {
        setFormError('Something went wrong. Please try again.')
      }
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Create your account"
      subtitle={
        <>
          Already registered? <Link to="/login">Sign in</Link>
        </>
      }
    >
      {formError && <Alert tone="error">{formError}</Alert>}

      <form onSubmit={handleSubmit} noValidate className="form">
        <fieldset className="account-type">
          <legend>Account type</legend>
          <div className="account-type-options">
            {ACCOUNT_TYPES.map((t) => (
              <label key={t.value} className={`account-option${form.accountType === t.value ? ' selected' : ''}`}>
                <input
                  type="radio"
                  name="accountType"
                  value={t.value}
                  checked={form.accountType === t.value}
                  onChange={() => update('accountType', t.value)}
                />
                <span className="account-option-title">{t.title}</span>
                <span className="account-option-text">{t.text}</span>
              </label>
            ))}
          </div>
        </fieldset>

        <h2 className="form-section">About you</h2>
        <TextField label="Full name" autoComplete="name" value={form.fullName} error={errors.fullName}
          onChange={(e) => update('fullName', e.target.value)} maxLength={150} />
        <TextField label="Work email" type="email" autoComplete="email" value={form.email} error={errors.email}
          onChange={(e) => update('email', e.target.value)} maxLength={320} />
        <div className="field-row">
          <PasswordField label="Password" autoComplete="new-password" value={form.password} error={errors.password}
            hint="8+ characters with a letter and a number." onChange={(e) => update('password', e.target.value)} maxLength={72} />
          <PasswordField label="Confirm password" autoComplete="new-password" value={form.confirmPassword}
            error={errors.confirmPassword} onChange={(e) => update('confirmPassword', e.target.value)} maxLength={72} />
        </div>

        <h2 className="form-section">Your company</h2>
        <TextField label="Company name" autoComplete="organization" value={form.companyName} error={errors.companyName}
          onChange={(e) => update('companyName', e.target.value)} maxLength={200} />
        <div className="field-row">
          <FieldShell label="Country" error={errors.countryCode}>
            {({ inputId, describedBy }) => (
              <select id={inputId} aria-describedby={describedBy} value={form.countryCode}
                onChange={(e) => update('countryCode', e.target.value)}>
                {COUNTRIES.map((c) => (
                  <option key={c.code} value={c.code}>{c.name}</option>
                ))}
              </select>
            )}
          </FieldShell>
          <TextField label="Commercial registration no." optional value={form.registrationNo}
            error={errors.registrationNo} onChange={(e) => update('registrationNo', e.target.value)} maxLength={100} />
        </div>

        {form.accountType === 'SUPPLIER' && (
          <Alert tone="info">Supplier companies are reviewed by our team before they appear in buyer searches.</Alert>
        )}

        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </AuthLayout>
  )
}

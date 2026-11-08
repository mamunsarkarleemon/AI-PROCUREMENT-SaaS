import { useState, type FormEvent } from 'react'
import { ApiError, errorMessage } from '../../api/client'
import { authApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { Alert, Avatar, Badge, PageHeader, PasswordField, TextArea, TextField } from '../../components/ui'
import { formatDate } from '../../lib/format'

export function ProfilePage() {
  const { user, setUser } = useAuth()
  const [form, setForm] = useState({
    fullName: user?.fullName ?? '',
    phone: user?.phone ?? '',
    companyName: user?.companyName ?? '',
    address: user?.address ?? '',
    city: user?.city ?? '',
    country: user?.country ?? '',
    about: user?.about ?? '',
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [msg, setMsg] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)
  const [busy, setBusy] = useState(false)
  if (!user) return null
  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function save(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setMsg(null)
    try {
      setUser(await authApi.updateProfile(form))
      setMsg({ tone: 'success', text: 'Profile saved.' })
      setErrors({})
    } catch (err) {
      if (err instanceof ApiError) setErrors(err.fieldErrors)
      setMsg({ tone: 'error', text: errorMessage(err) })
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageHeader title="My profile" subtitle="This information is shown to buyers and suppliers you work with." />
      <div className="detail-grid">
        <form className="card form" onSubmit={save} noValidate>
          {msg && <Alert tone={msg.tone}>{msg.text}</Alert>}
          <div className="form-row">
            <TextField label="Full name" value={form.fullName} onChange={set('fullName')} error={errors.fullName} required />
            <TextField label="Phone" value={form.phone} onChange={set('phone')} />
          </div>
          <TextField label="Company name" value={form.companyName} onChange={set('companyName')} />
          <TextField label="Address" value={form.address} onChange={set('address')} />
          <div className="form-row">
            <TextField label="City" value={form.city} onChange={set('city')} />
            <TextField label="Country" value={form.country} onChange={set('country')} />
          </div>
          <TextArea label={user.role === 'SUPPLIER' ? 'About your company (shown to buyers)' : 'About your company'} value={form.about} onChange={set('about')} />
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save profile'}</button>
        </form>
        <div className="detail-side">
          <div className="card center">
            <Avatar name={user.fullName} size={72} />
            <h2>{user.fullName}</h2>
            <p className="muted">{user.email}</p>
            <Badge value={user.role} />
            <p className="small muted">Member since {formatDate(user.createdAt)}</p>
          </div>
          <PasswordCard />
        </div>
      </div>
    </>
  )
}

function PasswordCard() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [msg, setMsg] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)
  async function submit(e: FormEvent) {
    e.preventDefault()
    if (next.length < 6) {
      setMsg({ tone: 'error', text: 'New password must be at least 6 characters.' })
      return
    }
    try {
      await authApi.changePassword(current, next)
      setMsg({ tone: 'success', text: 'Password changed.' })
      setCurrent('')
      setNext('')
    } catch (err) {
      setMsg({ tone: 'error', text: errorMessage(err) })
    }
  }
  return (
    <form className="card form" onSubmit={submit} noValidate>
      <h2 className="card-title">Change password</h2>
      {msg && <Alert tone={msg.tone}>{msg.text}</Alert>}
      <PasswordField label="Current password" autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} />
      <PasswordField label="New password" autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} hint="At least 6 characters." />
      <button className="btn btn-outline">Update password</button>
    </form>
  )
}

import { useEffect, useState, type FormEvent } from 'react'
import { Pencil, Search, Trash2, UserCheck, UserPlus, UserX } from 'lucide-react'
import { ApiError, errorMessage } from '../../api/client'
import { adminApi } from '../../api/endpoints'
import type { Role, User, UserForm, UserStatus } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Alert, AsyncBlock, Avatar, Badge, EmptyState, Modal, PageHeader, PasswordField, SelectField, TextArea, TextField } from '../../components/ui'
import { formatDate } from '../../lib/format'
import { useApi } from '../../lib/useApi'

export function UsersPage() {
  const { user: me } = useAuth()
  const [role, setRole] = useState<Role | ''>('')
  const [status, setStatus] = useState<UserStatus | ''>('')
  const [q, setQ] = useState('')
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState<User | 'new' | null>(null)
  const [msg, setMsg] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)

  useEffect(() => {
    const t = window.setTimeout(() => setSearch(q), 300)
    return () => window.clearTimeout(t)
  }, [q])

  const { data, loading, error, reload } = useApi(() => adminApi.users({ role, status, q: search }), [role, status, search])

  async function toggle(u: User) {
    const next = u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await adminApi.setUserStatus(u.id, next)
      setMsg({ tone: 'success', text: `${u.fullName} is now ${next.toLowerCase()}.${next === 'INACTIVE' ? ' They can no longer log in.' : ''}` })
      reload()
    } catch (err) {
      setMsg({ tone: 'error', text: errorMessage(err) })
    }
  }

  async function remove(u: User) {
    if (!window.confirm(`Delete ${u.fullName}? Their products, requests and negotiations will be removed too.`)) return
    try {
      await adminApi.deleteUser(u.id)
      setMsg({ tone: 'success', text: `${u.fullName} was deleted.` })
      reload()
    } catch (err) {
      setMsg({ tone: 'error', text: errorMessage(err) })
    }
  }

  return (
    <>
      <PageHeader title="Users" subtitle="Create, edit, activate or deactivate buyers, suppliers and admins."
        actions={<button className="btn btn-primary" onClick={() => setEditing('new')}><UserPlus size={16} /> Create user</button>} />
      {msg && <Alert tone={msg.tone}>{msg.text}</Alert>}
      <div className="card filters">
        <div className="filter-row">
          <label className="search-box grow">
            <Search size={16} aria-hidden="true" />
            <input type="search" placeholder="Search name, email or company…" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search users" />
          </label>
          <label>Role
            <select value={role} onChange={(e) => setRole(e.target.value as Role | '')}>
              <option value="">All roles</option>
              <option value="BUYER">Buyers</option>
              <option value="SUPPLIER">Suppliers</option>
              <option value="ADMIN">Admins</option>
            </select>
          </label>
          <label>Status
            <select value={status} onChange={(e) => setStatus(e.target.value as UserStatus | '')}>
              <option value="">All</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </label>
        </div>
      </div>
      <AsyncBlock loading={loading} error={error}>
        {data?.length === 0 ? (
          <EmptyState title="No users match" />
        ) : (
          <section className="card">
            <div className="table-wrap">
              <table className="table table-stack">
                <thead>
                  <tr><th>User</th><th>Company</th><th>Role</th><th>Status</th><th>Phone</th><th>City</th><th>Joined</th><th className="actions-col">Actions</th></tr>
                </thead>
                <tbody>
                  {data?.map((u) => (
                    <tr key={u.id}>
                      <td className="stack-full">
                        <div className="cell-product">
                          <Avatar name={u.fullName} size={32} />
                          <span><b>{u.fullName}</b><br /><small className="muted">{u.email}</small></span>
                        </div>
                      </td>
                      <td data-label="Company">{u.companyName ?? '—'}</td>
                      <td data-label="Role"><Badge value={u.role} /></td>
                      <td data-label="Status"><Badge value={u.status} /></td>
                      <td data-label="Phone">{u.phone ?? '—'}</td>
                      <td data-label="City">{u.city ?? '—'}</td>
                      <td className="nowrap" data-label="Joined">{formatDate(u.createdAt)}</td>
                      <td className="actions-col stack-actions">
                        <div className="row-actions">
                          <button className="icon-btn" title="Edit" aria-label={`Edit ${u.fullName}`} onClick={() => setEditing(u)}><Pencil size={16} /></button>
                          {u.id !== me?.id && (
                            <>
                              <button className="icon-btn" title={u.status === 'ACTIVE' ? 'Deactivate' : 'Activate'} aria-label={u.status === 'ACTIVE' ? `Deactivate ${u.fullName}` : `Activate ${u.fullName}`} onClick={() => toggle(u)}>
                                {u.status === 'ACTIVE' ? <UserX size={16} /> : <UserCheck size={16} />}
                              </button>
                              <button className="icon-btn danger" title="Delete" aria-label={`Delete ${u.fullName}`} onClick={() => remove(u)}><Trash2 size={16} /></button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        )}
      </AsyncBlock>
      {editing && (
        <UserFormModal user={editing === 'new' ? null : editing} onClose={() => setEditing(null)}
          onSaved={(u, isNew) => { setEditing(null); setMsg({ tone: 'success', text: `${u.fullName} was ${isNew ? 'created' : 'updated'}.` }); reload() }} />
      )}
    </>
  )
}

function UserFormModal({ user, onClose, onSaved }: { user: User | null; onClose: () => void; onSaved: (u: User, isNew: boolean) => void }) {
  const [form, setForm] = useState<UserForm>({
    fullName: user?.fullName ?? '',
    email: user?.email ?? '',
    password: '',
    role: user?.role ?? 'BUYER',
    status: user?.status ?? 'ACTIVE',
    phone: user?.phone ?? '',
    companyName: user?.companyName ?? '',
    address: user?.address ?? '',
    city: user?.city ?? '',
    country: user?.country ?? '',
    about: user?.about ?? '',
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const set = (k: keyof UserForm) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const saved = user ? await adminApi.updateUser(user.id, form) : await adminApi.createUser(form)
      onSaved(saved, !user)
    } catch (err) {
      if (err instanceof ApiError) setErrors(err.fieldErrors)
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <Modal title={user ? `Edit ${user.fullName}` : 'Create user'} onClose={onClose} wide
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button><button className="btn btn-primary" form="user-form" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button></>}>
      <form id="user-form" className="form" onSubmit={submit} noValidate>
        {error && <Alert tone="error">{error}</Alert>}
        <div className="form-row">
          <TextField label="Full name" value={form.fullName} onChange={set('fullName')} error={errors.fullName} required />
          <TextField label="Email" type="email" value={form.email} onChange={set('email')} error={errors.email} required />
        </div>
        <div className="form-row form-row-3">
          <SelectField label="Role" value={form.role} onChange={set('role')}>
            <option value="BUYER">Buyer</option>
            <option value="SUPPLIER">Supplier</option>
            <option value="ADMIN">Admin</option>
          </SelectField>
          <SelectField label="Status" value={form.status} onChange={set('status')}>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive (cannot log in)</option>
          </SelectField>
          <PasswordField label={user ? 'New password' : 'Password'} autoComplete="new-password" value={form.password} onChange={set('password')} error={errors.password}
            hint={user ? 'Leave empty to keep the current password' : 'At least 6 characters'} required={!user} />
        </div>
        <div className="form-row">
          <TextField label="Company name" value={form.companyName} onChange={set('companyName')} />
          <TextField label="Phone" value={form.phone} onChange={set('phone')} />
        </div>
        <TextField label="Address" value={form.address} onChange={set('address')} />
        <div className="form-row">
          <TextField label="City" value={form.city} onChange={set('city')} />
          <TextField label="Country" value={form.country} onChange={set('country')} />
        </div>
        <TextArea label="About" value={form.about} onChange={set('about')} rows={3} />
      </form>
    </Modal>
  )
}

import { useEffect, useState } from 'react'
import { authApi } from '../api/auth'
import { ApiError } from '../api/client'
import type { CompanyRole, CompanyStatus, DashboardResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Alert } from '../components/Alert'
import { Logo } from '../components/Logo'

const ROLE_LABEL: Record<CompanyRole, string> = {
  BUYER_OWNER: 'Buyer · Owner',
  BUYER_MEMBER: 'Buyer · Member',
  SUPPLIER_OWNER: 'Supplier · Owner',
  SUPPLIER_MEMBER: 'Supplier · Member',
}

const STATUS_LABEL: Record<CompanyStatus, string> = {
  ACTIVE: 'Active',
  PENDING_REVIEW: 'Pending review',
  SUSPENDED: 'Suspended',
}

const NEXT_STEPS = {
  BUYER: ['Create a procurement request', 'Confirm the AI-extracted specification', 'Shortlist suppliers and send RFQs'],
  SUPPLIER: ['Complete your company profile', 'Upload certificates for review', 'Add products, price tiers and MOQ'],
}

type AreaState = { kind: 'loading' } | { kind: 'ok'; data: DashboardResponse } | { kind: 'error'; message: string }

export function DashboardPage() {
  const { user, token, logout } = useAuth()
  const membership = user?.memberships[0]
  const [area, setArea] = useState<AreaState>({ kind: 'loading' })

  // Calls the role-protected endpoint to prove the token + role check works end to end.
  useEffect(() => {
    if (!membership || !token) return
    const controller = new AbortController()
    authApi
      .dashboard(membership.companyType, token, controller.signal)
      .then((data) => setArea({ kind: 'ok', data }))
      .catch((err: unknown) => {
        if (err instanceof DOMException && err.name === 'AbortError') return
        if (err instanceof ApiError && err.status === 401) logout('Your session has expired. Please sign in again.')
        else setArea({ kind: 'error', message: err instanceof ApiError ? err.message : 'Could not load workspace.' })
      })
    return () => controller.abort()
  }, [membership, token, logout])

  if (!user) return null
  const firstName = user.fullName.split(' ')[0]

  return (
    <div className="app-shell">
      <header className="topbar">
        <Logo />
        <div className="topbar-user">
          <span className="topbar-name">
            {user.fullName}
            {membership && <small>{membership.companyName}</small>}
          </span>
          <button type="button" className="btn btn-ghost" onClick={() => logout()}>
            Sign out
          </button>
        </div>
      </header>

      <main className="dashboard">
        <div className="dashboard-heading">
          <h1>Welcome, {firstName}</h1>
          {area.kind === 'ok' && <p>{area.data.message}</p>}
          {area.kind === 'loading' && <p className="muted">Loading your workspace…</p>}
        </div>

        {area.kind === 'error' && <Alert tone="error">{area.message}</Alert>}
        {membership?.companyStatus === 'PENDING_REVIEW' && (
          <Alert tone="warning">
            Your supplier company is pending review. You can set up your profile now; buyers will see you once it is approved.
          </Alert>
        )}

        <div className="card-grid">
          <section className="card">
            <h2>Your account</h2>
            <dl className="details">
              <dt>Name</dt>
              <dd>{user.fullName}</dd>
              <dt>Email</dt>
              <dd>
                {user.email}{' '}
                <span className={`badge ${user.emailVerified ? 'badge-ok' : 'badge-muted'}`}>
                  {user.emailVerified ? 'Verified' : 'Not verified'}
                </span>
              </dd>
              <dt>Member since</dt>
              <dd>{new Date(user.createdAt).toLocaleDateString(undefined, { dateStyle: 'medium' })}</dd>
            </dl>
          </section>

          {membership && (
            <section className="card">
              <h2>Your company</h2>
              <dl className="details">
                <dt>Company</dt>
                <dd>{membership.companyName}</dd>
                <dt>Country</dt>
                <dd>{membership.countryCode}</dd>
                <dt>Your role</dt>
                <dd>{ROLE_LABEL[membership.role]}</dd>
                <dt>Status</dt>
                <dd>
                  <span className={`badge ${membership.companyStatus === 'ACTIVE' ? 'badge-ok' : 'badge-warn'}`}>
                    {STATUS_LABEL[membership.companyStatus]}
                  </span>
                </dd>
              </dl>
            </section>
          )}

          {membership && (
            <section className="card">
              <h2>Coming next</h2>
              <ol className="steps">
                {NEXT_STEPS[membership.companyType].map((s) => (
                  <li key={s}>{s}</li>
                ))}
              </ol>
            </section>
          )}
        </div>
      </main>
    </div>
  )
}

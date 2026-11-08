import type { ReactNode } from 'react'
import { Link, Navigate, Outlet, useLocation } from 'react-router'
import { ShieldOff } from 'lucide-react'
import { useAuth } from './AuthContext'

export function FullPageLoader() {
  return (
    <div className="page-center" role="status">
      <span className="spinner" aria-hidden="true" />
      <span className="sr-only">Loading…</span>
    </div>
  )
}

/** Signed-in users only. */
export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()
  if (status === 'loading') return <FullPageLoader />
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

/** Login / register: signed-in users go straight to their dashboard. */
export function GuestOnly() {
  const { status } = useAuth()
  if (status === 'loading') return <FullPageLoader />
  if (status === 'authenticated') return <Navigate to="/app/dashboard" replace />
  return <Outlet />
}

/**
 * Shows the page only if the admin has given this menu to the user's role.
 * (The backend checks the same thing, so hiding is not the only protection.)
 */
export function RequireMenu({ menu, children }: { menu: string; children: ReactNode }) {
  const { hasMenu } = useAuth()
  if (!hasMenu(menu)) {
    return (
      <div className="empty-state" style={{ marginTop: 48 }}>
        <ShieldOff size={40} aria-hidden="true" />
        <h2>You don’t have access to this page</h2>
        <p>Ask the platform admin to enable it for your account type.</p>
        <Link className="btn btn-primary" to="/app/dashboard">
          Back to dashboard
        </Link>
      </div>
    )
  }
  return <>{children}</>
}

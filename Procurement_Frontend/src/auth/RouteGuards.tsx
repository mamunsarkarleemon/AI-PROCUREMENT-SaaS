import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './AuthContext'

function FullPageSpinner() {
  return (
    <div className="page-center" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span className="sr-only">Loading…</span>
    </div>
  )
}

/**
 * Only signed-in users may see the nested routes.
 * This is a UX convenience - the backend still enforces every permission.
 */
export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <FullPageSpinner />
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

/** Login/register pages: send already signed-in users to the dashboard. */
export function GuestOnly() {
  const { status } = useAuth()
  if (status === 'loading') return <FullPageSpinner />
  if (status === 'authenticated') return <Navigate to="/dashboard" replace />
  return <Outlet />
}

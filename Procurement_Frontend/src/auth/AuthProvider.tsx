import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/auth'
import { ApiError } from '../api/client'
import type { LoginRequest, User } from '../api/types'
import { AuthContext, type AuthContextValue, type AuthStatus } from './AuthContext'
import { tokenStorage, type StoredSession } from './tokenStorage'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<StoredSession | null>(() => tokenStorage.load())
  const [user, setUser] = useState<User | null>(null)
  const [status, setStatus] = useState<AuthStatus>(() => (tokenStorage.load() ? 'loading' : 'anonymous'))
  const [logoutReason, setLogoutReason] = useState<string | null>(null)

  const logout = useCallback((reason?: string) => {
    tokenStorage.clear()
    setSession(null)
    setUser(null)
    setStatus('anonymous')
    setLogoutReason(reason ?? null)
  }, [])

  // After a page reload we only have the token: load the profile from the server.
  // The backend decides whether the token is still valid.
  useEffect(() => {
    if (!session || user) return
    const controller = new AbortController()
    authApi
      .me(session.token, controller.signal)
      .then((me) => {
        setUser(me)
        setStatus('authenticated')
      })
      .catch((err: unknown) => {
        if (err instanceof DOMException && err.name === 'AbortError') return
        logout(err instanceof ApiError && err.status === 401 ? 'Your session has expired. Please sign in again.' : undefined)
      })
    return () => controller.abort()
  }, [session, user, logout])

  // Sign out automatically when the access token expires.
  useEffect(() => {
    if (!session) return
    const timer = window.setTimeout(
      () => logout('Your session has expired. Please sign in again.'),
      Math.max(0, session.expiresAt - Date.now()),
    )
    return () => window.clearTimeout(timer)
  }, [session, logout])

  const login = useCallback(async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials)
    setSession(tokenStorage.save(response.accessToken, response.expiresIn))
    setUser(response.user)
    setStatus('authenticated')
    setLogoutReason(null)
    return response.user
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, token: session?.token ?? null, login, logout, logoutReason }),
    [status, user, session, login, logout, logoutReason],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

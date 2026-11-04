import { createContext, useContext } from 'react'
import type { LoginRequest, User } from '../api/types'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

export interface AuthContextValue {
  status: AuthStatus
  user: User | null
  token: string | null
  login: (credentials: LoginRequest) => Promise<User>
  logout: (reason?: string) => void
  /** Message shown on the login page after an automatic logout (e.g. session expired). */
  logoutReason: string | null
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}

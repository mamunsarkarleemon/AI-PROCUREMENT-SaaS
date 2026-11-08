import { createContext, useContext } from 'react'
import type { MenuItem, User } from '../api/types'

export interface AuthContextValue {
  status: 'loading' | 'authenticated' | 'anonymous'
  user: User | null
  menus: MenuItem[]
  login: (email: string, password: string) => Promise<User>
  logout: () => void
  /** Reload user + menus (after profile edit, or when admin changes menu access). */
  refresh: () => Promise<void>
  setUser: (u: User) => void
  hasMenu: (key: string) => boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}

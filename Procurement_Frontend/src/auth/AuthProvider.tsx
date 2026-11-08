import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { setUnauthorizedHandler, tokenStore } from '../api/client'
import { authApi } from '../api/endpoints'
import type { MenuItem, User } from '../api/types'
import { AuthContext, type AuthContextValue } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [menus, setMenus] = useState<MenuItem[]>([])
  const [status, setStatus] = useState<AuthContextValue['status']>(() => (tokenStore.get() ? 'loading' : 'anonymous'))

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    setMenus([])
    setStatus('anonymous')
  }, [])

  const refresh = useCallback(async () => {
    const [me, m] = await Promise.all([authApi.me(), authApi.menus()])
    setUser(me)
    setMenus(m)
    setStatus('authenticated')
  }, [])

  // Any 401 from the server (token expired, user deactivated) logs the user out.
  useEffect(() => setUnauthorizedHandler(logout), [logout])

  // On page load: if a token is saved, load the user and their menus.
  useEffect(() => {
    if (!tokenStore.get()) return
    Promise.all([authApi.me(), authApi.menus()]).then(
      ([me, m]) => {
        setUser(me)
        setMenus(m)
        setStatus('authenticated')
      },
      () => logout(),
    )
  }, [logout])

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await authApi.login(email, password)
      tokenStore.set(res.token)
      await refresh()
      return res.user
    },
    [refresh],
  )

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      menus,
      login,
      logout,
      refresh,
      setUser,
      hasMenu: (key) => menus.some((m) => m.key === key),
    }),
    [status, user, menus, login, logout, refresh],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

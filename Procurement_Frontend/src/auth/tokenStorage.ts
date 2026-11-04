/**
 * Where the access token lives in the browser.
 *
 * sessionStorage (not localStorage): the token disappears when the tab is closed, and it is
 * short-lived (60 min). It is still readable by JavaScript, so XSS could steal it - the planned
 * upgrade is an HttpOnly refresh-token cookie (blueprint Appendix G). Keeping all access in this
 * one file makes that change easy.
 */
const KEY = 'procureai.session'

export interface StoredSession {
  token: string
  /** Epoch milliseconds. */
  expiresAt: number
}

export const tokenStorage = {
  load(): StoredSession | null {
    try {
      const raw = sessionStorage.getItem(KEY)
      if (!raw) return null
      const session = JSON.parse(raw) as StoredSession
      if (typeof session.token !== 'string' || session.expiresAt <= Date.now()) {
        sessionStorage.removeItem(KEY)
        return null
      }
      return session
    } catch {
      return null
    }
  },

  save(token: string, expiresInSeconds: number): StoredSession {
    const session = { token, expiresAt: Date.now() + expiresInSeconds * 1000 }
    try {
      sessionStorage.setItem(KEY, JSON.stringify(session))
    } catch {
      // Storage blocked (private mode etc.): the session still works until the page reloads.
    }
    return session
  },

  clear(): void {
    try {
      sessionStorage.removeItem(KEY)
    } catch {
      // ignore
    }
  },
}

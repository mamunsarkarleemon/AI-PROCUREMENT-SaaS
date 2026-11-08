import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router'
import { Bell, CheckCheck, ChevronDown, Home, LogOut, Menu, PanelLeftClose, PanelLeftOpen, UserCircle } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { notificationApi } from '../api/endpoints'
import type { NotificationItem } from '../api/types'
import { Avatar, Badge, Logo } from '../components/ui'
import { Icon } from '../lib/icons'
import { timeAgo } from '../lib/format'

const ROLE_LABEL = { ADMIN: 'Admin', BUYER: 'Buyer', SUPPLIER: 'Supplier' } as const

/** The logged-in shell: top navbar, role-based left menu, footer. */
export function AppLayout() {
  const { user, menus } = useAuth()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const { pathname } = useLocation()

  // Close the mobile menu whenever the page changes.
  const [lastPath, setLastPath] = useState(pathname)
  if (lastPath !== pathname) {
    setLastPath(pathname)
    setMobileOpen(false)
  }

  if (!user) return null
  const current = menus.find((m) => pathname.startsWith(m.path))

  return (
    <div className={`app${collapsed ? ' sidebar-collapsed' : ''}${mobileOpen ? ' sidebar-mobile-open' : ''}`}>
      <header className="topbar">
        <div className="topbar-left">
          <button type="button" className="icon-btn mobile-only" aria-label="Open menu" onClick={() => setMobileOpen(true)}>
            <Menu size={22} />
          </button>
          <Link to="/app/dashboard" className="topbar-logo"><Logo /></Link>
          <span className="topbar-divider" aria-hidden="true" />
          <span className="topbar-company">{user.companyName || user.fullName}</span>
          <Badge value={user.role} label={ROLE_LABEL[user.role]} />
        </div>
        <div className="topbar-right">
          <Link to="/" className="icon-btn" title="Public website" aria-label="Public website"><Home size={19} /></Link>
          <NotificationBell />
          <UserMenu />
        </div>
      </header>

      <div className="sidebar-backdrop" onClick={() => setMobileOpen(false)} aria-hidden="true" />
      <aside className="sidebar" aria-label="Menu">
        <nav>
          {menus.map((m) => (
            <NavLink key={m.key} to={m.path} className={({ isActive }) => `side-link${isActive ? ' active' : ''}`} title={collapsed ? m.label : undefined}>
              <Icon name={m.icon} size={19} />
              <span>{m.label}</span>
            </NavLink>
          ))}
        </nav>
        <button type="button" className="side-collapse" onClick={() => setCollapsed((c) => !c)}>
          {collapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
          <span>{collapsed ? 'Expand' : 'Collapsed view'}</span>
        </button>
      </aside>

      <div className="app-body">
        <div className="breadcrumb">
          <Link to="/app/dashboard">home</Link>
          <span aria-hidden="true">›</span>
          <span>{current?.label ?? 'details'}</span>
        </div>
        <main className="app-main">
          <Outlet />
        </main>
        <footer className="app-footer">
          <span>Copyright © {new Date().getFullYear()} <b>ProcureAI</b>. All rights reserved.</span>
          <span>v1.0.0</span>
        </footer>
      </div>
    </div>
  )
}

function useClickOutside(onOutside: () => void) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) onOutside()
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [onOutside])
  return ref
}

function NotificationBell() {
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState<NotificationItem[]>([])
  const [unread, setUnread] = useState(0)
  const navigate = useNavigate()
  const close = useCallback(() => setOpen(false), [])
  const ref = useClickOutside(close)

  const load = useCallback(() => {
    notificationApi.list().then((r) => {
      setItems(r.items)
      setUnread(r.unread)
    }).catch(() => {})
  }, [])

  // Check for new notifications every 30 seconds.
  useEffect(() => {
    load()
    const t = window.setInterval(load, 30000)
    return () => window.clearInterval(t)
  }, [load])

  async function openItem(n: NotificationItem) {
    setOpen(false)
    if (!n.read) {
      await notificationApi.read(n.id).catch(() => {})
      load()
    }
    if (n.link) navigate(n.link)
  }

  async function readAll() {
    await notificationApi.readAll().catch(() => {})
    load()
  }

  return (
    <div className="dropdown" ref={ref}>
      <button type="button" className="icon-btn" aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`} aria-expanded={open} onClick={() => { setOpen((o) => !o); load() }}>
        <Bell size={19} />
        {unread > 0 && <span className="dot-count">{unread > 9 ? '9+' : unread}</span>}
      </button>
      {open && (
        <div className="dropdown-panel notif-panel">
          <div className="dropdown-head">
            <b>Notifications</b>
            {unread > 0 && (
              <button type="button" className="link-btn" onClick={readAll}><CheckCheck size={15} /> Mark all read</button>
            )}
          </div>
          {items.length === 0 ? (
            <p className="muted dropdown-empty">You’re all caught up.</p>
          ) : (
            <ul>
              {items.map((n) => (
                <li key={n.id}>
                  <button type="button" className={`notif${n.read ? '' : ' unread'}`} onClick={() => openItem(n)}>
                    <span>{n.message}</span>
                    <small>{timeAgo(n.createdAt)}</small>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}

function UserMenu() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const close = useCallback(() => setOpen(false), [])
  const ref = useClickOutside(close)
  if (!user) return null
  return (
    <div className="dropdown" ref={ref}>
      <button type="button" className="user-btn" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
        <Avatar name={user.fullName} size={34} />
        <span className="user-btn-name">{user.fullName}</span>
        <ChevronDown size={16} aria-hidden="true" />
      </button>
      {open && (
        <div className="dropdown-panel user-panel">
          <div className="user-panel-head">
            <b>{user.fullName}</b>
            <small>{user.email}</small>
          </div>
          <button type="button" className="menu-item" onClick={() => { setOpen(false); navigate('/app/profile') }}>
            <UserCircle size={17} /> My profile
          </button>
          <button type="button" className="menu-item danger" onClick={() => { logout(); navigate('/') }}>
            <LogOut size={17} /> Log out
          </button>
        </div>
      )}
    </div>
  )
}

import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router'
import { Mail, MapPin, Menu, Phone, X } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { Logo } from '../components/ui'

const LINKS = [
  { href: '/#features', label: 'Features' },
  { href: '/#how-it-works', label: 'How it works' },
  { href: '/#marketplace', label: 'Marketplace' },
  { href: '/#faq', label: 'FAQ' },
  { href: '/#contact', label: 'Contact' },
]

/** Top navbar + footer for the landing, login and register pages. */
export function PublicLayout() {
  const { status } = useAuth()
  const [open, setOpen] = useState(false)
  const { pathname } = useLocation()
  const authPage = pathname === '/login' || pathname === '/register'

  return (
    <div className="public">
      <header className="site-nav">
        <div className="container site-nav-inner">
          <Link to="/" aria-label="ProcureAI home" onClick={() => setOpen(false)}>
            <Logo />
          </Link>
          <nav className={`site-links${open ? ' open' : ''}`} aria-label="Main">
            {LINKS.map((l) => (
              <a key={l.href} href={l.href} onClick={() => setOpen(false)}>
                {l.label}
              </a>
            ))}
            <div className="site-auth">
              {status === 'authenticated' ? (
                <Link className="btn btn-primary" to="/app/dashboard">Go to dashboard</Link>
              ) : (
                <>
                  <Link className="btn btn-ghost" to="/login" onClick={() => setOpen(false)}>Log in</Link>
                  <Link className="btn btn-primary" to="/register" onClick={() => setOpen(false)}>Create account</Link>
                </>
              )}
            </div>
          </nav>
          <button type="button" className="icon-btn site-burger" aria-label={open ? 'Close menu' : 'Open menu'} aria-expanded={open} onClick={() => setOpen((o) => !o)}>
            {open ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </header>

      <main className={authPage ? 'auth-main' : undefined}>
        <Outlet />
      </main>

      <footer className="site-footer">
        <div className="container footer-grid">
          <div>
            <Logo light />
            <p>The B2B procurement marketplace for Qatar SMEs. Find suppliers, compare quotes, negotiate and track every order in one place.</p>
          </div>
          <div>
            <h3>Platform</h3>
            <a href="/#features">Features</a>
            <a href="/#how-it-works">How it works</a>
            <a href="/#marketplace">Marketplace</a>
            <a href="/#faq">FAQ</a>
          </div>
          <div>
            <h3>Get started</h3>
            <Link to="/register">Register as Buyer</Link>
            <Link to="/register?role=SUPPLIER">Register as Supplier</Link>
            <Link to="/login">Log in</Link>
          </div>
          <div>
            <h3>Contact</h3>
            <span><MapPin size={14} aria-hidden="true" /> West Bay, Doha, Qatar</span>
            <span><Phone size={14} aria-hidden="true" /> +974 4400 0000</span>
            <span><Mail size={14} aria-hidden="true" /> hello@procureai.qa</span>
          </div>
        </div>
        <div className="container footer-bottom">
          <span>© {new Date().getFullYear()} ProcureAI. All rights reserved.</span>
          <span>Made for Qatar SMEs</span>
        </div>
      </footer>
    </div>
  )
}

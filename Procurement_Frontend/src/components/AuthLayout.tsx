import type { ReactNode } from 'react'
import { Logo } from './Logo'

const HIGHLIGHTS = [
  'Turn a plain-language request into a confirmed specification',
  'Compare supplier quotes on price, MOQ and landed cost',
  'Send RFQs and track shipments in one place',
]

/** Two-column layout for the sign-in and sign-up pages. */
export function AuthLayout({ title, subtitle, children }: { title: string; subtitle: ReactNode; children: ReactNode }) {
  return (
    <div className="auth-shell">
      <aside className="auth-brand">
        <Logo inverted />
        <div className="auth-brand-copy">
          <h2>Procurement for Qatar SMEs, without the spreadsheets.</h2>
          <ul>
            {HIGHLIGHTS.map((h) => (
              <li key={h}>{h}</li>
            ))}
          </ul>
        </div>
        <p className="auth-brand-foot">Buyers and suppliers, one workspace.</p>
      </aside>

      <main className="auth-main">
        <div className="auth-card">
          <div className="auth-mobile-logo">
            <Logo />
          </div>
          <h1>{title}</h1>
          <p className="auth-subtitle">{subtitle}</p>
          {children}
        </div>
      </main>
    </div>
  )
}

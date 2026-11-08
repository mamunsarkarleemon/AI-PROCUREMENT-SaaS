import { useEffect, useId, useRef, useState, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'
import { AlertCircle, CheckCircle2, Info, Package, TriangleAlert, X } from 'lucide-react'

// ---------- Logo ----------
export function Logo({ light = false }: { light?: boolean }) {
  return (
    <span className={`logo${light ? ' logo-light' : ''}`}>
      <span className="logo-mark" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="18" height="18">
          <path d="M5 8h14l-1.4 9.1a2 2 0 0 1-2 1.7H8.4a2 2 0 0 1-2-1.7L5 8Z" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
          <path d="M9 8V6.5a3 3 0 0 1 6 0V8" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
      </span>
      <span className="logo-text">
        Procure<b>AI</b>
      </span>
    </span>
  )
}

// ---------- Alert ----------
const ALERT_ICON = { error: AlertCircle, success: CheckCircle2, info: Info, warning: TriangleAlert }
export function Alert({ tone, children }: { tone: 'error' | 'success' | 'info' | 'warning'; children: ReactNode }) {
  const I = ALERT_ICON[tone]
  return (
    <div className={`alert alert-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <I size={18} aria-hidden="true" />
      <div>{children}</div>
    </div>
  )
}

// ---------- Status badge (always text + colour, never colour alone) ----------
const TONE: Record<string, string> = {
  ACTIVE: 'green', OPEN: 'blue', PENDING: 'amber', CONFIRMED: 'blue', SHIPPED: 'purple', DELIVERED: 'green',
  CANCELLED: 'red', CLOSED: 'gray', INACTIVE: 'gray', ACCEPTED: 'green', REJECTED: 'red', WITHDRAWN: 'gray',
  BUYER: 'blue', SUPPLIER: 'purple', ADMIN: 'dark',
}
export function Badge({ value, label }: { value: string; label?: string }) {
  const text = label ?? value.charAt(0) + value.slice(1).toLowerCase().replace(/_/g, ' ')
  return <span className={`badge badge-${TONE[value] ?? 'gray'}`}>{text}</span>
}

// ---------- Page header ----------
export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  )
}

// ---------- Loading / empty / error states ----------
export function Loader({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="loader" role="status">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  )
}

export function EmptyState({ icon, title, text, action }: { icon?: ReactNode; title: string; text?: string; action?: ReactNode }) {
  return (
    <div className="empty-state">
      {icon ?? <Package size={36} aria-hidden="true" />}
      <h2>{title}</h2>
      {text && <p>{text}</p>}
      {action}
    </div>
  )
}

/** Shows loader / error / content in one line: <AsyncBlock loading error>…</AsyncBlock> */
export function AsyncBlock({ loading, error, children }: { loading: boolean; error: string | null; children: ReactNode }) {
  if (loading) return <Loader />
  if (error) return <Alert tone="error">{error}</Alert>
  return <>{children}</>
}

// ---------- Product / request thumbnail with a placeholder when there is no photo ----------
export function Thumb({ src, alt, size = 'md' }: { src: string | null | undefined; alt: string; size?: 'sm' | 'md' | 'lg' }) {
  const [broken, setBroken] = useState(false)
  if (!src || broken) {
    return (
      <div className={`thumb thumb-${size} thumb-empty`} aria-label={alt} role="img">
        <Package size={size === 'sm' ? 18 : 32} aria-hidden="true" />
      </div>
    )
  }
  return <img className={`thumb thumb-${size}`} src={src} alt={alt} loading="lazy" onError={() => setBroken(true)} />
}

export function Avatar({ name, size = 36 }: { name: string; size?: number }) {
  const initials = name.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0]!.toUpperCase()).join('')
  return (
    <span className="avatar" style={{ width: size, height: size, fontSize: size * 0.38 }} aria-hidden="true">
      {initials}
    </span>
  )
}

// ---------- Modal ----------
export function Modal({ title, onClose, children, footer, wide = false }: {
  title: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
  wide?: boolean
}) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    ref.current?.focus()
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = prev
    }
  }, [onClose])
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal${wide ? ' modal-wide' : ''}`} role="dialog" aria-modal="true" aria-label={title} tabIndex={-1} ref={ref}>
        <div className="modal-head">
          <h2>{title}</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close">
            <X size={18} />
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  )
}

// ---------- Form fields ----------
function FieldWrap({ label, error, hint, required, children }: {
  label: string
  error?: string
  hint?: ReactNode
  required?: boolean
  children: (id: string, describedBy?: string) => ReactNode
}) {
  const id = useId()
  const msgId = `${id}-msg`
  return (
    <div className={`field${error ? ' field-invalid' : ''}`}>
      <label htmlFor={id}>
        {label}
        {required && <span className="req" aria-hidden="true"> *</span>}
      </label>
      {children(id, error || hint ? msgId : undefined)}
      {error ? <p id={msgId} className="field-error">{error}</p> : hint ? <p id={msgId} className="field-hint">{hint}</p> : null}
    </div>
  )
}

type Common = { label: string; error?: string; hint?: ReactNode }

export function TextField({ label, error, hint, ...rest }: Common & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <FieldWrap label={label} error={error} hint={hint} required={rest.required}>
      {(id, d) => <input id={id} aria-invalid={!!error} aria-describedby={d} {...rest} />}
    </FieldWrap>
  )
}

export function TextArea({ label, error, hint, ...rest }: Common & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <FieldWrap label={label} error={error} hint={hint} required={rest.required}>
      {(id, d) => <textarea id={id} rows={4} aria-invalid={!!error} aria-describedby={d} {...rest} />}
    </FieldWrap>
  )
}

export function SelectField({ label, error, hint, children, ...rest }: Common & SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <FieldWrap label={label} error={error} hint={hint} required={rest.required}>
      {(id, d) => (
        <select id={id} aria-invalid={!!error} aria-describedby={d} {...rest}>
          {children}
        </select>
      )}
    </FieldWrap>
  )
}

export function PasswordField({ label, error, hint, ...rest }: Common & InputHTMLAttributes<HTMLInputElement>) {
  const [show, setShow] = useState(false)
  return (
    <FieldWrap label={label} error={error} hint={hint} required={rest.required}>
      {(id, d) => (
        <div className="password-wrap">
          <input id={id} type={show ? 'text' : 'password'} aria-invalid={!!error} aria-describedby={d} {...rest} />
          <button type="button" className="password-toggle" onClick={() => setShow((s) => !s)} aria-pressed={show}>
            {show ? 'Hide' : 'Show'}
          </button>
        </div>
      )}
    </FieldWrap>
  )
}

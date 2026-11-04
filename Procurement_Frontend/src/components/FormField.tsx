import { useId, useState, type InputHTMLAttributes, type ReactNode } from 'react'

interface FieldShellProps {
  label: string
  error?: string
  hint?: ReactNode
  optional?: boolean
  children: (ids: { inputId: string; describedBy: string | undefined }) => ReactNode
}

/** Label + control + hint/error, wired up with the right aria attributes. */
export function FieldShell({ label, error, hint, optional, children }: FieldShellProps) {
  const inputId = useId()
  const messageId = `${inputId}-msg`
  const hasMessage = Boolean(error || hint)
  return (
    <div className={`field${error ? ' field-invalid' : ''}`}>
      <label htmlFor={inputId}>
        {label}
        {optional && <span className="field-optional"> (optional)</span>}
      </label>
      {children({ inputId, describedBy: hasMessage ? messageId : undefined })}
      {error ? (
        <p id={messageId} className="field-error">{error}</p>
      ) : hint ? (
        <p id={messageId} className="field-hint">{hint}</p>
      ) : null}
    </div>
  )
}

type InputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> & {
  label: string
  error?: string
  hint?: ReactNode
  optional?: boolean
}

export function TextField({ label, error, hint, optional, ...input }: InputProps) {
  return (
    <FieldShell label={label} error={error} hint={hint} optional={optional}>
      {({ inputId, describedBy }) => (
        <input id={inputId} aria-invalid={Boolean(error)} aria-describedby={describedBy} {...input} />
      )}
    </FieldShell>
  )
}

export function PasswordField({ label, error, hint, ...input }: Omit<InputProps, 'type'>) {
  const [visible, setVisible] = useState(false)
  return (
    <FieldShell label={label} error={error} hint={hint}>
      {({ inputId, describedBy }) => (
        <div className="password-wrap">
          <input
            id={inputId}
            type={visible ? 'text' : 'password'}
            aria-invalid={Boolean(error)}
            aria-describedby={describedBy}
            {...input}
          />
          <button
            type="button"
            className="password-toggle"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? 'Hide password' : 'Show password'}
            aria-pressed={visible}
          >
            {visible ? 'Hide' : 'Show'}
          </button>
        </div>
      )}
    </FieldShell>
  )
}

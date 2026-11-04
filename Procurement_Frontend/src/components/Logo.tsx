export function Logo({ inverted = false }: { inverted?: boolean }) {
  return (
    <span className={`logo${inverted ? ' logo-inverted' : ''}`}>
      <svg viewBox="0 0 32 32" width="28" height="28" aria-hidden="true">
        <rect width="32" height="32" rx="8" fill="currentColor" opacity="0.15" />
        <path d="M9 11h14l-1.6 9.2a2 2 0 0 1-2 1.8h-6.8a2 2 0 0 1-2-1.8L9 11Z" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
        <path d="M13 11V9.5a3 3 0 0 1 6 0V11" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      </svg>
      <span>ProcureAI</span>
    </span>
  )
}

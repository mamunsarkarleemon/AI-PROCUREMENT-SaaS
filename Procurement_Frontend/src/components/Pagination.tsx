import { ChevronLeft, ChevronRight } from 'lucide-react'
import { formatNumber } from '../lib/format'

/** Page numbers to show, e.g. 1 … 4 5 6 … 12 (null = the dots). */
function pageList(page: number, total: number): (number | null)[] {
  const wanted = [...new Set([1, page - 1, page, page + 1, total])].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b)
  const out: (number | null)[] = []
  wanted.forEach((p, i) => {
    if (i > 0 && p - wanted[i - 1] > 1) out.push(null)
    out.push(p)
  })
  return out
}

/** "Showing 13–24 of 57" with Previous / page numbers / Next. Hidden when everything fits on one page. */
export function Pagination({ page, totalPages, totalItems, pageSize, onChange, scrollToTop = true }: {
  page: number
  totalPages: number
  totalItems: number
  pageSize: number
  onChange: (page: number) => void
  /** Turn off for a list in the lower part of a page, so the page does not jump. */
  scrollToTop?: boolean
}) {
  if (totalPages <= 1) return null

  function go(p: number) {
    onChange(p)
    if (scrollToTop) window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const from = (page - 1) * pageSize + 1
  const to = Math.min(page * pageSize, totalItems)

  return (
    <nav className="pagination no-print" aria-label="Pages">
      <p className="pagination-info">Showing <b>{formatNumber(from)}–{formatNumber(to)}</b> of <b>{formatNumber(totalItems)}</b></p>
      <div className="pagination-controls">
        <button type="button" className="page-btn page-step" disabled={page <= 1} onClick={() => go(page - 1)}>
          <ChevronLeft size={16} aria-hidden="true" /> Previous
        </button>
        <span className="page-numbers">
          {pageList(page, totalPages).map((p, i) => (p === null
            ? <span key={`dots-${i}`} className="page-dots" aria-hidden="true">…</span>
            : <button key={p} type="button" className={`page-btn${p === page ? ' active' : ''}`} aria-current={p === page ? 'page' : undefined} aria-label={`Page ${p}`} onClick={() => go(p)}>{p}</button>))}
        </span>
        <span className="page-compact">Page {page} of {totalPages}</span>
        <button type="button" className="page-btn page-step" disabled={page >= totalPages} onClick={() => go(page + 1)}>
          Next <ChevronRight size={16} aria-hidden="true" />
        </button>
      </div>
    </nav>
  )
}

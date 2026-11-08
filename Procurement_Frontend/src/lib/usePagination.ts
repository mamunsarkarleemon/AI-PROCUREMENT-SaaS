import { useState } from 'react'

/**
 * Splits a loaded list into pages:
 *   const paging = usePagination(rows, 10, filterText)
 *   rows -> paging.pageItems, then <Pagination {...paging.props} />
 * Goes back to page 1 whenever `resetKey` changes (e.g. the search text or a tab).
 */
export function usePagination<T>(items: T[], pageSize = 10, resetKey: string | number = '') {
  const [page, setPage] = useState(1)
  const [lastKey, setLastKey] = useState(resetKey)
  if (lastKey !== resetKey) {
    setLastKey(resetKey)
    setPage(1)
  }

  const totalItems = items.length
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))
  // If rows were deleted and this page no longer exists, show the last one.
  const current = Math.min(page, totalPages)
  const pageItems = items.slice((current - 1) * pageSize, current * pageSize)

  return { pageItems, props: { page: current, totalPages, totalItems, pageSize, onChange: setPage } }
}

import { useState } from 'react'
import { ShoppingCart } from 'lucide-react'
import { orderApi } from '../../api/endpoints'
import type { Order, OrderStatus } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { OrdersTable } from '../../components/OrdersTable'
import { Pagination } from '../../components/Pagination'
import { AsyncBlock, EmptyState, PageHeader } from '../../components/ui'
import { formatMoney } from '../../lib/format'
import { useApi } from '../../lib/useApi'
import { usePagination } from '../../lib/usePagination'

type Filter = OrderStatus | '' | 'RETURNS'

const matches = (o: Order, f: Filter) => (f === 'RETURNS' ? o.returnStatus === 'PENDING' : !f || o.status === f)

const FILTERS: { value: Filter; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'SHIPPED', label: 'Shipped' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'RETURNS', label: 'Returns pending' },
]

export function OrdersPage() {
  const { user } = useAuth()
  const { data, loading, error } = useApi(() => orderApi.list(), [])
  const [status, setStatus] = useState<Filter>('')
  const rows = (data ?? []).filter((o) => matches(o, status))
  const paging = usePagination(rows, 10, status)
  if (!user) return null
  const total = rows.filter((o) => o.status !== 'CANCELLED').reduce((s, o) => s + o.totalAmount, 0)

  return (
    <>
      <PageHeader
        title="Orders"
        subtitle={user.role === 'BUYER' ? 'Everything you ordered.' : user.role === 'SUPPLIER' ? 'Orders buyers placed with you. Confirm, ship and track them here.' : 'All orders on the platform.'}
      />
      <div className="tabs" role="tablist">
        {FILTERS.map((f) => {
          const count = (data ?? []).filter((o) => matches(o, f.value)).length
          return (
            <button key={f.label} role="tab" aria-selected={status === f.value} className={`tab${status === f.value ? ' active' : ''}`} onClick={() => setStatus(f.value)}>
              {f.label} <span className="tab-count">{count}</span>
            </button>
          )
        })}
      </div>
      <AsyncBlock loading={loading} error={error}>
        {rows.length === 0 ? (
          <EmptyState icon={<ShoppingCart size={32} />} title="No orders here" />
        ) : (
          <section className="card">
            <div className="card-head">
              <h2>{rows.length} order{rows.length === 1 ? '' : 's'}</h2>
              <span className="muted">Value (excl. cancelled): <b>{formatMoney(total)}</b></span>
            </div>
            <OrdersTable orders={paging.pageItems} viewer={user.role} />
            <Pagination {...paging.props} />
          </section>
        )}
      </AsyncBlock>
    </>
  )
}

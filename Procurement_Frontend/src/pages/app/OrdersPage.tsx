import { useState } from 'react'
import { ShoppingCart } from 'lucide-react'
import { orderApi } from '../../api/endpoints'
import type { OrderStatus } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { OrdersTable } from '../../components/OrdersTable'
import { AsyncBlock, EmptyState, PageHeader } from '../../components/ui'
import { formatMoney } from '../../lib/format'
import { useApi } from '../../lib/useApi'

const FILTERS: { value: OrderStatus | ''; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'SHIPPED', label: 'Shipped' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

export function OrdersPage() {
  const { user } = useAuth()
  const { data, loading, error } = useApi(() => orderApi.list(), [])
  const [status, setStatus] = useState<OrderStatus | ''>('')
  if (!user) return null
  const rows = (data ?? []).filter((o) => !status || o.status === status)
  const total = rows.filter((o) => o.status !== 'CANCELLED').reduce((s, o) => s + o.totalAmount, 0)

  return (
    <>
      <PageHeader
        title="Orders"
        subtitle={user.role === 'BUYER' ? 'Everything you ordered.' : user.role === 'SUPPLIER' ? 'Orders buyers placed with you. Confirm, ship and track them here.' : 'All orders on the platform.'}
      />
      <div className="tabs" role="tablist">
        {FILTERS.map((f) => {
          const count = (data ?? []).filter((o) => !f.value || o.status === f.value).length
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
            <OrdersTable orders={rows} viewer={user.role} />
          </section>
        )}
      </AsyncBlock>
    </>
  )
}

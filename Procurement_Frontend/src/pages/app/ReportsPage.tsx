import { useState } from 'react'
import { Download, Printer } from 'lucide-react'
import { adminApi, dashboardApi } from '../../api/endpoints'
import type { Report, User } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { AmountChart, BarList, StatCard } from '../../components/Charts'
import { OrdersTable } from '../../components/OrdersTable'
import { AsyncBlock, Badge, EmptyState } from '../../components/ui'
import { formatDate, formatMoney, formatNumber, isoDate } from '../../lib/format'
import { useApi } from '../../lib/useApi'

const PRESETS = [
  { label: 'Last 7 days', days: 7 },
  { label: 'Last 30 days', days: 30 },
  { label: 'Last 90 days', days: 90 },
  { label: 'This year', days: 0 },
]

export function ReportsPage() {
  const { user } = useAuth()
  const today = new Date()
  const [from, setFrom] = useState(isoDate(new Date(today.getTime() - 29 * 86400000)))
  const [to, setTo] = useState(isoDate(today))
  const [supplierId, setSupplierId] = useState<number | null>(null)
  const [buyerId, setBuyerId] = useState<number | null>(null)
  const isAdmin = user?.role === 'ADMIN'

  const { data: r, loading, error } = useApi(() => dashboardApi.report({ from, to, supplierId, buyerId }), [from, to, supplierId, buyerId])
  const { data: suppliers } = useApi<User[]>(() => (isAdmin ? adminApi.users({ role: 'SUPPLIER' }) : Promise.resolve([])), [isAdmin])
  const { data: buyers } = useApi<User[]>(() => (isAdmin ? adminApi.users({ role: 'BUYER' }) : Promise.resolve([])), [isAdmin])
  if (!user) return null

  function preset(days: number) {
    const end = new Date()
    const start = days === 0 ? new Date(end.getFullYear(), 0, 1) : new Date(end.getTime() - (days - 1) * 86400000)
    setFrom(isoDate(start))
    setTo(isoDate(end))
  }

  const amountWord = user.role === 'BUYER' ? 'Total spent' : user.role === 'SUPPLIER' ? 'Total sales' : 'Order value'

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Reports</h1>
          <p>{user.role === 'BUYER' ? 'What you bought, from whom and when.' : user.role === 'SUPPLIER' ? 'What you sold, to whom and when.' : 'Platform-wide orders. Filter by supplier or buyer.'}</p>
        </div>
        <div className="page-actions no-print">
          {r && <button className="btn btn-ghost" onClick={() => downloadCsv(r)}><Download size={16} /> Export CSV</button>}
          <button className="btn btn-ghost" onClick={() => window.print()}><Printer size={16} /> Print</button>
        </div>
      </div>

      <div className="card filters no-print">
        <div className="preset-row">
          {PRESETS.map((p) => <button key={p.label} type="button" className="chip-btn" onClick={() => preset(p.days)}>{p.label}</button>)}
        </div>
        <div className="filter-row">
          <label>From<input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} /></label>
          <label>To<input type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} /></label>
          {isAdmin && (
            <>
              <label>Supplier
                <select value={supplierId ?? ''} onChange={(e) => setSupplierId(e.target.value ? Number(e.target.value) : null)}>
                  <option value="">All suppliers</option>
                  {suppliers?.map((s) => <option key={s.id} value={s.id}>{s.companyName ?? s.fullName}</option>)}
                </select>
              </label>
              <label>Buyer
                <select value={buyerId ?? ''} onChange={(e) => setBuyerId(e.target.value ? Number(e.target.value) : null)}>
                  <option value="">All buyers</option>
                  {buyers?.map((b) => <option key={b.id} value={b.id}>{b.companyName ?? b.fullName}</option>)}
                </select>
              </label>
            </>
          )}
        </div>
      </div>

      <AsyncBlock loading={loading} error={error}>
        {r && (
          <>
            <p className="muted print-only">Period: {formatDate(r.from)} – {formatDate(r.to)}</p>
            <div className="stat-grid">
              <StatCard card={{ label: amountWord, value: r.summary.totalAmount, format: 'money', hint: 'Excluding cancelled orders', icon: 'Wallet', tone: 'blue' }} />
              <StatCard card={{ label: 'Orders', value: r.summary.orderCount, format: 'count', hint: `${r.summary.pendingCount} pending · ${r.summary.cancelledCount} cancelled`, icon: 'ShoppingCart', tone: 'purple' }} />
              <StatCard card={{ label: 'Delivered value', value: r.summary.deliveredAmount, format: 'money', hint: `${r.summary.deliveredCount} delivered orders`, icon: 'Package', tone: 'green' }} />
              <StatCard card={{ label: 'Average order', value: r.summary.averageOrder, format: 'money', hint: 'Per non-cancelled order', icon: 'BarChart3', tone: 'orange' }} />
            </div>

            <section className="card">
              <div className="card-head">
                <h2>{amountWord} per day</h2>
                <span className="muted">{formatDate(r.from)} – {formatDate(r.to)}</span>
              </div>
              <AmountChart data={r.daily} />
            </section>

            <div className="grid-2">
              {user.role !== 'SUPPLIER' && (
                <section className="card">
                  <h2 className="card-title">By supplier</h2>
                  <BarList rows={r.bySupplier.map((x) => ({ label: x.name, sub: `${x.orders} order${x.orders === 1 ? '' : 's'}`, value: x.amount }))} />
                </section>
              )}
              {user.role !== 'BUYER' && (
                <section className="card">
                  <h2 className="card-title">By buyer</h2>
                  <BarList rows={r.byBuyer.map((x) => ({ label: x.name, sub: `${x.orders} order${x.orders === 1 ? '' : 's'}`, value: x.amount }))} />
                </section>
              )}
              <section className="card">
                <h2 className="card-title">By product</h2>
                <BarList rows={r.byProduct.slice(0, 8).map((x) => ({ label: x.productName, sub: `${formatNumber(x.quantity)} ${x.unit}`, value: x.amount }))} />
              </section>
              <section className="card">
                <h2 className="card-title">By status</h2>
                <table className="table table-compact">
                  <thead><tr><th>Status</th><th className="num">Orders</th><th className="num">Amount</th></tr></thead>
                  <tbody>
                    {r.byStatus.map((s) => (
                      <tr key={s.status}><td><Badge value={s.status} /></td><td className="num">{s.orders}</td><td className="num">{formatMoney(s.amount)}</td></tr>
                    ))}
                  </tbody>
                </table>
              </section>
            </div>

            <section className="card">
              <h2 className="card-title">Orders in this period ({r.orders.length})</h2>
              {r.orders.length === 0 ? <EmptyState title="No orders in this period" /> : <OrdersTable orders={r.orders} viewer={user.role} />}
            </section>
          </>
        )}
      </AsyncBlock>
    </>
  )
}

function downloadCsv(r: Report) {
  const header = ['Order', 'Date', 'Product', 'Supplier', 'Buyer', 'Quantity', 'Unit', 'Unit price', 'Total', 'Status']
  const lines = r.orders.map((o) => [o.orderNo, o.createdAt.slice(0, 10), o.productName, o.supplier.companyName, o.buyer.companyName, o.quantity, o.unit, o.unitPrice, o.totalAmount, o.status])
  const csv = [header, ...lines].map((row) => row.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `orders_${r.from}_to_${r.to}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

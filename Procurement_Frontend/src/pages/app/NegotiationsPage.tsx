import { useState } from 'react'
import { useNavigate } from 'react-router'
import { Handshake } from 'lucide-react'
import { negotiationApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { AsyncBlock, Badge, EmptyState, PageHeader, Thumb } from '../../components/ui'
import { formatMoney, formatNumber, timeAgo } from '../../lib/format'
import { useApi } from '../../lib/useApi'

export function NegotiationsPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { data, loading, error } = useApi(() => negotiationApi.list(), [])
  const [filter, setFilter] = useState<'all' | 'turn' | 'open' | 'closed'>('all')
  if (!user) return null

  const rows = (data ?? []).filter((n) =>
    filter === 'all' ? true : filter === 'turn' ? n.myTurn : filter === 'open' ? n.status === 'OPEN' : n.status !== 'OPEN')
  const myTurnCount = (data ?? []).filter((n) => n.myTurn).length

  return (
    <>
      <PageHeader title="Negotiations" subtitle={user.role === 'BUYER' ? 'Price offers you made to suppliers.' : 'Price offers buyers sent you.'} />
      <div className="tabs" role="tablist">
        {([['all', 'All'], ['turn', `Waiting for me (${myTurnCount})`], ['open', 'Open'], ['closed', 'Closed']] as const).map(([k, label]) => (
          <button key={k} role="tab" aria-selected={filter === k} className={`tab${filter === k ? ' active' : ''}`} onClick={() => setFilter(k)}>{label}</button>
        ))}
      </div>
      <AsyncBlock loading={loading} error={error}>
        {rows.length === 0 ? (
          <EmptyState icon={<Handshake size={32} />} title="No negotiations here" text={user.role === 'BUYER' ? 'Open a product in the marketplace and click “Negotiate price”.' : 'When a buyer makes an offer on your product, it appears here.'} />
        ) : (
          <section className="card">
            <ul className="neg-list">
              {rows.map((n) => {
                const other = user.role === 'BUYER' ? n.supplier : n.buyer
                return (
                  <li key={n.id}>
                    <button type="button" className="neg-row" onClick={() => navigate(`/app/negotiations/${n.id}`)}>
                      <Thumb src={n.productImage} alt={n.productName} size="sm" />
                      <span className="grow">
                        <b>{n.productName}</b>
                        <small>{user.role === 'ADMIN' ? `${n.buyer.companyName} ↔ ${n.supplier.companyName}` : other.companyName} · updated {timeAgo(n.updatedAt)}</small>
                      </span>
                      <span className="neg-offer">
                        <b>{formatMoney(n.currentPrice)}</b>
                        <small>× {formatNumber(n.currentQty)} {n.unit} · list {formatMoney(n.listPrice)}</small>
                      </span>
                      {n.myTurn ? <span className="badge badge-amber">Your turn</span> : <Badge value={n.status} />}
                    </button>
                  </li>
                )
              })}
            </ul>
          </section>
        )}
      </AsyncBlock>
    </>
  )
}

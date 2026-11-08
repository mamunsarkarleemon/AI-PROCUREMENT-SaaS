import { useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { ArrowLeft, Check, Mail, MapPin, Phone, Printer } from 'lucide-react'
import { errorMessage } from '../../api/client'
import { orderApi } from '../../api/endpoints'
import type { OrderStatus, Party } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Alert, AsyncBlock, Badge, Thumb } from '../../components/ui'
import { ORDER_ACTION_LABEL, ORDER_SOURCE_LABEL, ORDER_STEPS } from '../../lib/constants'
import { formatDateTime, formatMoney, formatNumber } from '../../lib/format'
import { useApi } from '../../lib/useApi'

export function OrderDetailPage() {
  const id = Number(useParams().id)
  const created = (useLocation().state as { created?: boolean } | null)?.created
  const { user } = useAuth()
  const { data: o, setData, loading, error } = useApi(() => orderApi.get(id), [id])
  const [actionError, setActionError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  if (!user) return null

  async function change(status: OrderStatus) {
    if (status === 'CANCELLED' && !window.confirm('Cancel this order?')) return
    setBusy(true)
    setActionError(null)
    try {
      setData(await orderApi.setStatus(id, status))
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const stepIndex = o ? ORDER_STEPS.indexOf(o.status) : -1

  return (
    <>
      <Link to="/app/orders" className="back-link"><ArrowLeft size={16} /> All orders</Link>
      {created && <Alert tone="success">Your order was placed. The supplier has been notified.</Alert>}
      <AsyncBlock loading={loading} error={error}>
        {o && (
          <>
            <div className="page-header">
              <div>
                <h1>Order {o.orderNo}</h1>
                <p>Placed {formatDateTime(o.createdAt)} · {ORDER_SOURCE_LABEL[o.source]}</p>
              </div>
              <div className="page-actions">
                <Badge value={o.status} />
                <button className="btn btn-ghost btn-sm" onClick={() => window.print()}><Printer size={15} /> Print</button>
              </div>
            </div>

            {o.status === 'CANCELLED' ? (
              <Alert tone="error">This order was cancelled.</Alert>
            ) : (
              <ol className="progress card" aria-label="Order progress">
                {ORDER_STEPS.map((s, i) => (
                  <li key={s} className={i < stepIndex ? 'done' : i === stepIndex ? 'current' : ''} aria-current={i === stepIndex ? 'step' : undefined}>
                    <span className="progress-dot">{i < stepIndex ? <Check size={14} /> : i + 1}</span>
                    <span>{s.charAt(0) + s.slice(1).toLowerCase()}</span>
                  </li>
                ))}
              </ol>
            )}

            <div className="detail-grid">
              <div className="card">
                <h2 className="card-title">Items</h2>
                <div className="order-line">
                  <Thumb src={o.productImage} alt={o.productName} size="sm" />
                  <div className="grow">
                    {o.productId ? <Link to={`/app/products/${o.productId}`}><b>{o.productName}</b></Link> : <b>{o.productName}</b>}
                    <small>{formatNumber(o.quantity)} {o.unit} × {formatMoney(o.unitPrice)}</small>
                  </div>
                  <b>{formatMoney(o.totalAmount)}</b>
                </div>
                <div className="summary-box">
                  <span>Order total</span>
                  <b>{formatMoney(o.totalAmount)}</b>
                </div>
                {o.note && <p className="quote-msg">Note: “{o.note}”</p>}
                <p className="small muted">
                  {o.negotiationId && <>From <Link to={`/app/negotiations/${o.negotiationId}`}>negotiation #{o.negotiationId}</Link>. </>}
                  {o.requestId && <>From <Link to={`/app/requests/${o.requestId}`}>buying request #{o.requestId}</Link>. </>}
                  Last updated {formatDateTime(o.updatedAt)}.
                </p>

                {actionError && <Alert tone="error">{actionError}</Alert>}
                {o.nextStatuses.length > 0 && (
                  <div className="detail-actions">
                    {o.nextStatuses.map((s) => (
                      <button key={s} className={`btn ${s === 'CANCELLED' ? 'btn-ghost danger-text' : 'btn-primary'}`} disabled={busy} onClick={() => change(s)}>
                        {user.role === 'ADMIN' ? `Set ${s.toLowerCase()}` : ORDER_ACTION_LABEL[s]}
                      </button>
                    ))}
                  </div>
                )}
                {user.role === 'SUPPLIER' && o.status === 'PENDING' && <p className="small muted">Confirming takes {formatNumber(o.quantity)} {o.unit} from your stock.</p>}
                {user.role === 'BUYER' && o.status === 'SHIPPED' && <p className="small muted">Click “Mark as received” when the goods arrive.</p>}
              </div>
              <div className="detail-side">
                <PartyCard title="Buyer" party={o.buyer} />
                <PartyCard title="Supplier" party={o.supplier} />
              </div>
            </div>
          </>
        )}
      </AsyncBlock>
    </>
  )
}

function PartyCard({ title, party }: { title: string; party: Party }) {
  return (
    <div className="card">
      <h2 className="card-title">{title}</h2>
      <p><b>{party.companyName}</b><br /><span className="muted">{party.fullName}</span></p>
      <ul className="contact-mini">
        {(party.city || party.country) && <li><MapPin size={15} aria-hidden="true" /> {[party.city, party.country].filter(Boolean).join(', ')}</li>}
        {party.phone && <li><Phone size={15} aria-hidden="true" /> {party.phone}</li>}
        <li><Mail size={15} aria-hidden="true" /> <a href={`mailto:${party.email}`}>{party.email}</a></li>
      </ul>
    </div>
  )
}

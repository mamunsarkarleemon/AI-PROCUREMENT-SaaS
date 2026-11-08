import { useState, type FormEvent } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { ArrowLeft, Check, Mail, MapPin, PackageCheck, Phone, Printer, Star, Undo2 } from 'lucide-react'
import { errorMessage } from '../../api/client'
import { orderApi } from '../../api/endpoints'
import type { Order, OrderStatus, Party } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Alert, AsyncBlock, Badge, Modal, TextArea, TextField, Thumb } from '../../components/ui'
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
  const [receiving, setReceiving] = useState(false)
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

  async function confirmReturn(order: Order) {
    if (!window.confirm(`Confirm you got ${order.returnedQty} ${order.unit} back? They will be added to your stock.`)) return
    setBusy(true)
    setActionError(null)
    try {
      setData(await orderApi.confirmReturn(id))
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
                  <span>{o.returnedQty > 0 ? 'Amount to pay' : 'Order total'}</span>
                  <b>{formatMoney(o.totalAmount)}</b>
                </div>
                {o.returnStatus !== 'NONE' && (
                  <div className={`return-box${o.returnStatus === 'CONFIRMED' ? ' done' : ''}`}>
                    <div className="return-head">
                      <b><Undo2 size={16} aria-hidden="true" /> Partly received</b>
                      <Badge value={o.returnStatus === 'PENDING' ? 'PENDING' : 'DELIVERED'} label={o.returnStatus === 'PENDING' ? 'Return pending' : 'Return confirmed'} />
                    </div>
                    <dl className="facts">
                      <div><dt>Ordered</dt><dd>{formatNumber(o.quantity)} {o.unit}</dd></div>
                      <div><dt>Received</dt><dd>{formatNumber(o.receivedQty ?? 0)} {o.unit}</dd></div>
                      <div><dt>Returned</dt><dd>{formatNumber(o.returnedQty)} {o.unit}</dd></div>
                    </dl>
                    <p className="small muted">Order value {formatMoney(o.orderedAmount)}, returned {formatMoney(o.orderedAmount - o.totalAmount)}.</p>
                    {o.returnReason && <p className="quote-msg">Reason: “{o.returnReason}”</p>}
                    {o.returnStatus === 'PENDING'
                      ? <p className="small muted">Waiting for the supplier to confirm the returned {o.unit} came back. They go back into stock after that.</p>
                      : <p className="small muted">Supplier confirmed on {formatDateTime(o.returnConfirmedAt ?? o.updatedAt)}. The returned {o.unit} are back in stock.</p>}
                    {o.canConfirmReturn && (
                      <div className="detail-actions">
                        <button className="btn btn-primary" disabled={busy} onClick={() => confirmReturn(o)}>
                          <PackageCheck size={16} aria-hidden="true" /> Confirm return ({formatNumber(o.returnedQty)} {o.unit})
                        </button>
                      </div>
                    )}
                  </div>
                )}
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
                      <button key={s} className={`btn ${s === 'CANCELLED' ? 'btn-ghost danger-text' : 'btn-primary'}`} disabled={busy} onClick={() => (s === 'DELIVERED' && user.role === 'BUYER' ? setReceiving(true) : change(s))}>
                        {user.role === 'ADMIN' ? `Set ${s.toLowerCase()}` : ORDER_ACTION_LABEL[s]}
                      </button>
                    ))}
                  </div>
                )}
                {user.role === 'SUPPLIER' && o.status === 'PENDING' && <p className="small muted">Confirming takes {formatNumber(o.quantity)} {o.unit} from your stock.</p>}
                {user.role === 'BUYER' && o.status === 'SHIPPED' && <p className="small muted">Click “Mark as received” when the goods arrive. If some are damaged, you can accept only the good ones.</p>}
                {user.role === 'BUYER' && o.status === 'DELIVERED' && o.productId && (
                  <div className="detail-actions">
                    <Link className="btn btn-outline" to={`/app/products/${o.productId}#reviews`}><Star size={16} aria-hidden="true" /> Rate this product</Link>
                  </div>
                )}
              </div>
              <div className="detail-side">
                <PartyCard title="Buyer" party={o.buyer} />
                <PartyCard title="Supplier" party={o.supplier} />
              </div>
            </div>
          </>
        )}
      </AsyncBlock>
      {o && receiving && <ReceiveDialog order={o} onClose={() => setReceiving(false)} onDone={(updated) => { setData(updated); setReceiving(false) }} />}
    </>
  )
}

/** Buyer enters how many good units arrived; the rest go back to the supplier. */
function ReceiveDialog({ order: o, onClose, onDone }: { order: Order; onClose: () => void; onDone: (updated: Order) => void }) {
  const [qty, setQty] = useState(String(o.quantity))
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const received = Number(qty)
  const valid = qty !== '' && Number.isInteger(received) && received >= 0 && received <= o.quantity
  const returned = valid ? o.quantity - received : 0
  const needsReason = returned > 0

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      onDone(await orderApi.receive(o.id, received, needsReason ? reason : ''))
    } catch (err) {
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <Modal title="Receive this order" onClose={onClose}
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button><button className="btn btn-primary" form="receive-form" disabled={busy || !valid || (needsReason && !reason.trim())}>{busy ? 'Saving…' : 'Confirm received'}</button></>}>
      <form id="receive-form" className="form" onSubmit={submit}>
        {error && <Alert tone="error">{error}</Alert>}
        <p className="muted">You ordered <b>{formatNumber(o.quantity)} {o.unit}</b> of {o.productName}. Enter how many arrived in good condition.</p>
        <TextField label={`Good quantity received (${o.unit})`} type="number" min={0} max={o.quantity} step={1} value={qty} onChange={(e) => setQty(e.target.value)}
          error={qty !== '' && !valid ? `Enter a whole number from 0 to ${o.quantity}` : undefined} required />
        {needsReason && (
          <>
            <Alert tone="warning"><b>{formatNumber(returned)} {o.unit}</b> will go back to the supplier and be added to their stock after they confirm.</Alert>
            <TextArea label="Why are they being returned?" value={reason} onChange={(e) => setReason(e.target.value)} rows={2} maxLength={2000} placeholder="e.g. Damaged during delivery" required />
          </>
        )}
        <div className="summary-box">
          <span>{formatNumber(valid ? received : 0)} × {formatMoney(o.unitPrice)}</span>
          <b>{formatMoney((valid ? received : 0) * o.unitPrice)}</b>
        </div>
        <p className="small muted">You pay only for what you accept.</p>
      </form>
    </Modal>
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

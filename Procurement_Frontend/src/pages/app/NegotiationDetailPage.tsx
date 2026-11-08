import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { ArrowLeft, Check, CornerDownRight, X } from 'lucide-react'
import { errorMessage } from '../../api/client'
import { negotiationApi } from '../../api/endpoints'
import type { Negotiation } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Alert, AsyncBlock, Badge, TextArea, TextField, Thumb } from '../../components/ui'
import { formatDateTime, formatMoney, formatNumber } from '../../lib/format'
import { useApi } from '../../lib/useApi'

const ACTION_TEXT = { OFFER: 'made an offer', COUNTER: 'sent a counter-offer', ACCEPT: 'accepted the deal', REJECT: 'ended the negotiation' }

export function NegotiationDetailPage() {
  const id = Number(useParams().id)
  const { user } = useAuth()
  const { data: n, setData, loading, error } = useApi(() => negotiationApi.get(id), [id])
  const [actionError, setActionError] = useState<string | null>(null)
  const [showCounter, setShowCounter] = useState(false)
  if (!user) return null

  async function run(fn: () => Promise<Negotiation>) {
    setActionError(null)
    try {
      setData(await fn())
      setShowCounter(false)
    } catch (err) {
      setActionError(errorMessage(err))
    }
  }

  return (
    <>
      <Link to="/app/negotiations" className="back-link"><ArrowLeft size={16} /> All negotiations</Link>
      <AsyncBlock loading={loading} error={error}>
        {n && (
          <div className="detail-grid">
            <div className="card">
              <div className="card-head">
                <h2>Conversation</h2>
                <Badge value={n.status} />
              </div>
              <ol className="chat">
                {n.messages.map((m) => {
                  const mine = m.senderId === user.id
                  return (
                    <li key={m.id} className={`chat-msg${mine ? ' mine' : ''} action-${m.action.toLowerCase()}`}>
                      <div className="chat-bubble">
                        <p className="chat-who"><b>{mine ? 'You' : m.senderName}</b> {ACTION_TEXT[m.action]}</p>
                        {m.pricePerUnit !== null && m.action !== 'REJECT' && (
                          <p className="chat-offer">
                            {formatMoney(m.pricePerUnit)} × {formatNumber(m.quantity)} {n.unit} = <b>{formatMoney(m.total)}</b>
                          </p>
                        )}
                        {m.message && <p className="chat-text">{m.message}</p>}
                        <time>{formatDateTime(m.createdAt)}</time>
                      </div>
                    </li>
                  )
                })}
              </ol>

              {actionError && <Alert tone="error">{actionError}</Alert>}
              {n.status === 'ACCEPTED' && n.orderId && (
                <Alert tone="success">Deal agreed! <Link to={`/app/orders/${n.orderId}`}>Open the order</Link>.</Alert>
              )}
              {n.status === 'REJECTED' && <Alert tone="info">This negotiation is closed.</Alert>}
              {n.status === 'OPEN' && user.role !== 'ADMIN' && (
                n.myTurn ? (
                  showCounter ? (
                    <CounterForm n={n} onCancel={() => setShowCounter(false)} onSubmit={(d) => run(() => negotiationApi.counter(n.id, d))} />
                  ) : (
                    <div className="chat-actions">
                      <p><b>Your turn.</b> Accept {formatMoney(n.currentPrice)} per {n.unit} for {formatNumber(n.currentQty)} {n.unit}?</p>
                      <div className="detail-actions">
                        <button className="btn btn-primary" onClick={() => window.confirm(`Accept ${formatMoney(n.currentTotal)}? An order will be created.`) && run(() => negotiationApi.accept(n.id))}>
                          <Check size={16} /> Accept
                        </button>
                        <button className="btn btn-outline" onClick={() => setShowCounter(true)}><CornerDownRight size={16} /> Counter-offer</button>
                        <button className="btn btn-ghost danger-text" onClick={() => window.confirm('End this negotiation?') && run(() => negotiationApi.reject(n.id))}><X size={16} /> Reject</button>
                      </div>
                    </div>
                  )
                ) : (
                  <div className="chat-actions">
                    <p className="muted">Waiting for {user.role === 'BUYER' ? n.supplier.companyName : n.buyer.companyName} to reply.</p>
                    <button className="btn btn-ghost btn-sm danger-text" onClick={() => window.confirm('End this negotiation?') && run(() => negotiationApi.reject(n.id))}>End negotiation</button>
                  </div>
                )
              )}
            </div>

            <div className="detail-side">
              <div className="card">
                <div className="party">
                  <Thumb src={n.productImage} alt={n.productName} size="sm" />
                  <div>
                    <Link to={`/app/products/${n.productId}`}><b>{n.productName}</b></Link>
                    <small>List price {formatMoney(n.listPrice)} / {n.unit}</small>
                  </div>
                </div>
                <dl className="facts">
                  <div><dt>Current offer</dt><dd className="strong">{formatMoney(n.currentPrice)} / {n.unit}</dd></div>
                  <div><dt>Quantity</dt><dd>{formatNumber(n.currentQty)} {n.unit}</dd></div>
                  <div><dt>Total</dt><dd className="strong">{formatMoney(n.currentTotal)}</dd></div>
                  <div><dt>Discount vs list</dt><dd>{n.listPrice > 0 ? `${(((n.listPrice - n.currentPrice) / n.listPrice) * 100).toFixed(1)}%` : '—'}</dd></div>
                  <div><dt>Supplier stock</dt><dd>{formatNumber(n.stockQty)} {n.unit}</dd></div>
                  <div><dt>Minimum order</dt><dd>{formatNumber(n.minOrderQty)} {n.unit}</dd></div>
                </dl>
              </div>
              <div className="card">
                <h2 className="card-title">Parties</h2>
                <p><span className="muted">Buyer:</span> <b>{n.buyer.companyName}</b></p>
                <p><span className="muted">Supplier:</span> <b>{n.supplier.companyName}</b></p>
              </div>
            </div>
          </div>
        )}
      </AsyncBlock>
    </>
  )
}

function CounterForm({ n, onSubmit, onCancel }: { n: Negotiation; onSubmit: (d: { quantity: number; pricePerUnit: number; message: string }) => void; onCancel: () => void }) {
  const [price, setPrice] = useState(String(n.currentPrice))
  const [qty, setQty] = useState(String(n.currentQty))
  const [message, setMessage] = useState('')
  function submit(e: FormEvent) {
    e.preventDefault()
    onSubmit({ pricePerUnit: Number(price), quantity: Number(qty), message })
  }
  return (
    <form className="form chat-actions" onSubmit={submit}>
      <div className="form-row">
        <TextField label={`Your price per ${n.unit} (QAR)`} type="number" step="0.01" min="0.01" value={price} onChange={(e) => setPrice(e.target.value)} required />
        <TextField label="Quantity" type="number" min="1" value={qty} onChange={(e) => setQty(e.target.value)} required />
      </div>
      <TextArea label="Message" rows={2} value={message} onChange={(e) => setMessage(e.target.value)} />
      <div className="detail-actions">
        <button className="btn btn-primary">Send counter-offer ({formatMoney((Number(price) || 0) * (Number(qty) || 0))})</button>
        <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}

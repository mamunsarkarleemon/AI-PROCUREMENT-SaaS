import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ArrowLeft, Handshake, Mail, MapPin, Pencil, Phone, ShoppingCart } from 'lucide-react'
import { ApiError, errorMessage } from '../../api/client'
import { negotiationApi, orderApi, productApi } from '../../api/endpoints'
import type { Product } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { MediaGallery } from '../../components/Media'
import { Alert, AsyncBlock, Avatar, Badge, Modal, TextArea, TextField } from '../../components/ui'
import { formatMoney, formatNumber } from '../../lib/format'
import { useApi } from '../../lib/useApi'

export function ProductDetailPage() {
  const id = Number(useParams().id)
  const { user, hasMenu } = useAuth()
  const { data: p, loading, error } = useApi(() => productApi.get(id), [id])
  const [dialog, setDialog] = useState<'buy' | 'negotiate' | null>(null)
  const isOwner = p && user && p.supplier.id === user.id

  return (
    <>
      <Link to="/app/marketplace" className="back-link"><ArrowLeft size={16} /> Back to marketplace</Link>
      <AsyncBlock loading={loading} error={error}>
        {p && (
          <div className="detail-grid">
            <div className="card">
              <MediaGallery media={p.media} alt={p.name} />
            </div>
            <div className="detail-side">
              <div className="card">
                {p.category && <span className="product-cat">{p.category.name}</span>}
                <h1 className="detail-title">{p.name}</h1>
                {p.status !== 'ACTIVE' && <Badge value={p.status} label="Hidden from marketplace" />}
                <p className="detail-price">{formatMoney(p.price)} <small>per {p.unit}</small></p>
                <dl className="facts">
                  <div><dt>In stock</dt><dd>{formatNumber(p.stockQty)} {p.unit}</dd></div>
                  <div><dt>Minimum order</dt><dd>{formatNumber(p.minOrderQty)} {p.unit}</dd></div>
                </dl>
                {p.description && <p className="pre-line">{p.description}</p>}

                {user?.role === 'BUYER' && (
                  <div className="detail-actions">
                    <button className="btn btn-primary btn-lg" onClick={() => setDialog('buy')} disabled={p.stockQty < p.minOrderQty}>
                      <ShoppingCart size={18} /> Buy now
                    </button>
                    {hasMenu('negotiations') && (
                      <button className="btn btn-outline btn-lg" onClick={() => setDialog('negotiate')}>
                        <Handshake size={18} /> Negotiate price
                      </button>
                    )}
                  </div>
                )}
                {user?.role === 'BUYER' && p.stockQty < p.minOrderQty && <Alert tone="warning">Not enough stock to order right now. You can still negotiate.</Alert>}
                {isOwner && (
                  <div className="detail-actions">
                    <Link className="btn btn-primary" to={`/app/my-products?edit=${p.id}`}><Pencil size={16} /> Edit product</Link>
                  </div>
                )}
              </div>

              <div className="card">
                <h2 className="card-title">Supplier</h2>
                <div className="party">
                  <Avatar name={p.supplier.companyName} size={44} />
                  <div>
                    <b>{p.supplier.companyName}</b>
                    <small>{p.supplier.fullName}</small>
                  </div>
                </div>
                <ul className="contact-mini">
                  {(p.supplier.city || p.supplier.country) && <li><MapPin size={15} aria-hidden="true" /> {[p.supplier.city, p.supplier.country].filter(Boolean).join(', ')}</li>}
                  {p.supplier.phone && <li><Phone size={15} aria-hidden="true" /> {p.supplier.phone}</li>}
                  <li><Mail size={15} aria-hidden="true" /> {p.supplier.email}</li>
                </ul>
              </div>
            </div>
          </div>
        )}
      </AsyncBlock>
      {p && dialog === 'buy' && <BuyNowDialog product={p} onClose={() => setDialog(null)} />}
      {p && dialog === 'negotiate' && <NegotiateDialog product={p} onClose={() => setDialog(null)} />}
    </>
  )
}

function BuyNowDialog({ product: p, onClose }: { product: Product; onClose: () => void }) {
  const navigate = useNavigate()
  const [qty, setQty] = useState(String(p.minOrderQty))
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const n = Number(qty)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const order = await orderApi.buyNow(p.id, n, note)
      navigate(`/app/orders/${order.id}`, { state: { created: true } })
    } catch (err) {
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <Modal title={`Order ${p.name}`} onClose={onClose}
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button><button className="btn btn-primary" form="buy-form" disabled={busy || !n}>{busy ? 'Placing order…' : 'Place order'}</button></>}>
      <form id="buy-form" className="form" onSubmit={submit}>
        {error && <Alert tone="error">{error}</Alert>}
        <TextField label={`Quantity (${p.unit})`} type="number" min={p.minOrderQty} max={p.stockQty} value={qty} onChange={(e) => setQty(e.target.value)}
          hint={`Minimum ${p.minOrderQty}, available ${formatNumber(p.stockQty)}`} required />
        <TextArea label="Note to supplier" value={note} onChange={(e) => setNote(e.target.value)} placeholder="Delivery address, timing, packing…" rows={3} />
        <div className="summary-box">
          <span>{formatNumber(n || 0)} × {formatMoney(p.price)}</span>
          <b>{formatMoney((n || 0) * p.price)}</b>
        </div>
        <p className="small muted">The supplier will confirm your order. You can cancel while it is still pending.</p>
      </form>
    </Modal>
  )
}

function NegotiateDialog({ product: p, onClose }: { product: Product; onClose: () => void }) {
  const navigate = useNavigate()
  const [qty, setQty] = useState(String(p.minOrderQty))
  const [price, setPrice] = useState(String((p.price * 0.9).toFixed(2)))
  const [message, setMessage] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const n = await negotiationApi.start({ productId: p.id, quantity: Number(qty), pricePerUnit: Number(price), message })
      navigate(`/app/negotiations/${n.id}`)
    } catch (err) {
      if (err instanceof ApiError) setErrors(err.fieldErrors)
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <Modal title="Make an offer" onClose={onClose}
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button><button className="btn btn-primary" form="neg-form" disabled={busy}>{busy ? 'Sending…' : 'Send offer'}</button></>}>
      <form id="neg-form" className="form" onSubmit={submit}>
        {error && <Alert tone="error">{error}</Alert>}
        <p className="muted">Listed price: <b>{formatMoney(p.price)}</b> per {p.unit}. The supplier can accept, reject or send a counter-offer.</p>
        <div className="form-row">
          <TextField label={`Quantity (${p.unit})`} type="number" min={p.minOrderQty} value={qty} onChange={(e) => setQty(e.target.value)} error={errors.quantity} hint={`Minimum ${p.minOrderQty}`} required />
          <TextField label="Your price per unit (QAR)" type="number" step="0.01" min="0.01" value={price} onChange={(e) => setPrice(e.target.value)} error={errors.pricePerUnit} required />
        </div>
        <TextArea label="Message" value={message} onChange={(e) => setMessage(e.target.value)} placeholder="e.g. We order every month, can you do a better price?" rows={3} />
        <div className="summary-box">
          <span>Your offer total</span>
          <b>{formatMoney((Number(qty) || 0) * (Number(price) || 0))}</b>
        </div>
      </form>
    </Modal>
  )
}

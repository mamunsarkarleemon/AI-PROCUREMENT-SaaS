import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ArrowLeft, CalendarClock, Check, MapPin, Pencil, Truck, X } from 'lucide-react'
import { ApiError, errorMessage } from '../../api/client'
import { productApi, quoteApi, requestApi } from '../../api/endpoints'
import type { BuyRequest, Product, Quote } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { MediaGallery, MediaList, MediaPicker } from '../../components/Media'
import { Alert, AsyncBlock, Avatar, Badge, EmptyState, SelectField, TextArea, TextField } from '../../components/ui'
import { formatDate, formatMoney, formatNumber, timeAgo } from '../../lib/format'
import { useApi } from '../../lib/useApi'
import { RequestFormModal } from './MyRequestsPage'

export function RequestDetailPage() {
  const id = Number(useParams().id)
  const { user } = useAuth()
  const navigate = useNavigate()
  const { data: r, loading, error, reload } = useApi(() => requestApi.get(id), [id])
  const [message, setMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)
  const [editing, setEditing] = useState(false)
  if (!user) return null
  const isOwner = r && r.buyer.id === user.id

  async function accept(q: Quote) {
    if (!window.confirm(`Accept the quote from ${q.supplier.companyName} for ${formatMoney(q.total)}? An order will be created and the request will close.`)) return
    try {
      const order = await quoteApi.accept(q.id)
      navigate(`/app/orders/${order.id}`, { state: { created: true } })
    } catch (err) {
      setMessage({ tone: 'error', text: errorMessage(err) })
    }
  }

  async function reject(q: Quote) {
    try {
      await quoteApi.reject(q.id)
      reload()
    } catch (err) {
      setMessage({ tone: 'error', text: errorMessage(err) })
    }
  }

  return (
    <>
      <Link to={user.role === 'BUYER' ? '/app/my-requests' : user.role === 'ADMIN' ? '/app/admin/requests' : '/app/marketplace?tab=requests'} className="back-link">
        <ArrowLeft size={16} /> Back
      </Link>
      {message && <Alert tone={message.tone}>{message.text}</Alert>}
      <AsyncBlock loading={loading} error={error}>
        {r && (
          <>
            <div className="detail-grid">
              <div className="card">
                <div className="request-top">
                  <h1 className="detail-title">{r.title}</h1>
                  <Badge value={r.status} />
                </div>
                <p className="small muted">Posted by <b>{r.buyer.companyName}</b> · {timeAgo(r.createdAt)}{r.category ? ` · ${r.category.name}` : ''}</p>
                <dl className="facts facts-4">
                  <div><dt>Quantity</dt><dd>{formatNumber(r.quantity)} {r.unit}</dd></div>
                  <div><dt>Budget / unit</dt><dd>{r.targetPrice !== null ? formatMoney(r.targetPrice) : 'Open'}</dd></div>
                  <div><dt><CalendarClock size={14} aria-hidden="true" /> Needed by</dt><dd>{formatDate(r.neededBy)}</dd></div>
                  <div><dt><MapPin size={14} aria-hidden="true" /> Deliver to</dt><dd>{r.deliveryCity ?? '—'}</dd></div>
                </dl>
                {r.description && <p className="pre-line">{r.description}</p>}
                {r.media.length > 0 && <MediaGallery media={r.media} alt={r.title} />}
                {isOwner && r.status === 'OPEN' && (
                  <button className="btn btn-ghost btn-sm" onClick={() => setEditing(true)}><Pencil size={14} /> Edit request</button>
                )}
              </div>

              <div className="detail-side">
                {user.role === 'SUPPLIER' && <QuoteBox request={r} onSaved={reload} />}
                {(isOwner || user.role === 'ADMIN') && (
                  <div className="card">
                    <h2 className="card-title">Summary</h2>
                    <p><b>{r.quotes.length}</b> quote{r.quotes.length === 1 ? '' : 's'} received.</p>
                    {r.quotes.length > 0 && (
                      <p className="muted">Lowest price: <b>{formatMoney(Math.min(...r.quotes.map((q) => q.pricePerUnit)))}</b> / {r.unit}</p>
                    )}
                    {r.status === 'OPEN' && isOwner && <p className="small muted">Accepting a quote creates an order and closes this request.</p>}
                  </div>
                )}
              </div>
            </div>

            {(isOwner || user.role === 'ADMIN') && (
              <section className="section-block">
                <h2>Quotes</h2>
                {r.quotes.length === 0 ? (
                  <EmptyState title="No quotes yet" text="Suppliers have been notified. Quotes will appear here." />
                ) : (
                  <div className="quote-grid">
                    {r.quotes.map((q, i) => (
                      <article className={`quote-card card${i === 0 && r.quotes.length > 1 ? ' best' : ''}`} key={q.id}>
                        {i === 0 && r.quotes.length > 1 && <span className="best-tag">Lowest price</span>}
                        <div className="party">
                          <Avatar name={q.supplier.companyName} size={40} />
                          <div className="grow">
                            <b>{q.supplier.companyName}</b>
                            <small>{q.supplier.city ?? ''} · {timeAgo(q.createdAt)}</small>
                          </div>
                          <Badge value={q.status} />
                        </div>
                        <p className="quote-price">{formatMoney(q.pricePerUnit)} <small>/ {r.unit}</small></p>
                        <dl className="facts">
                          <div><dt>Quantity</dt><dd>{formatNumber(q.quantity)} {r.unit}</dd></div>
                          <div><dt>Total</dt><dd className="strong">{formatMoney(q.total)}</dd></div>
                          <div><dt><Truck size={14} aria-hidden="true" /> Delivery</dt><dd>{q.deliveryDays != null ? `${q.deliveryDays} days` : '—'}</dd></div>
                        </dl>
                        {q.productId && <p className="small">Product: <Link to={`/app/products/${q.productId}`}>{q.productName}</Link></p>}
                        {q.message && <p className="quote-msg">“{q.message}”</p>}
                        {q.media.length > 0 && <MediaGallery media={q.media} alt={`Quote from ${q.supplier.companyName}`} />}
                        {isOwner && r.status === 'OPEN' && q.status === 'PENDING' && (
                          <div className="detail-actions">
                            <button className="btn btn-primary" onClick={() => accept(q)}><Check size={16} /> Accept & order</button>
                            <button className="btn btn-ghost" onClick={() => reject(q)}><X size={16} /> Decline</button>
                          </div>
                        )}
                      </article>
                    ))}
                  </div>
                )}
              </section>
            )}
          </>
        )}
      </AsyncBlock>
      {r && editing && <RequestFormModal request={r} onClose={() => setEditing(false)} onSaved={() => { setEditing(false); reload() }} />}
    </>
  )
}

/** Supplier side: send or update my quote for this request. */
function QuoteBox({ request: r, onSaved }: { request: BuyRequest; onSaved: () => void }) {
  const mine = r.quotes[0] // the backend only returns the supplier's own quote
  const { data: products } = useApi<Product[]>(() => productApi.mine().catch(() => []), [])
  const [form, setForm] = useState({
    pricePerUnit: mine ? String(mine.pricePerUnit) : '',
    quantity: String(mine?.quantity ?? r.quantity),
    deliveryDays: mine?.deliveryDays != null ? String(mine.deliveryDays) : '',
    productId: mine?.productId ? String(mine.productId) : '',
    message: mine?.message ?? '',
  })
  const [files, setFiles] = useState<File[]>([])
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)
  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const editable = r.status === 'OPEN' && (!mine || mine.status === 'PENDING' || mine.status === 'WITHDRAWN')

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setDone(false)
    try {
      const q = await quoteApi.submit(r.id, {
        pricePerUnit: Number(form.pricePerUnit),
        quantity: Number(form.quantity),
        deliveryDays: form.deliveryDays ? Number(form.deliveryDays) : null,
        productId: form.productId ? Number(form.productId) : null,
        message: form.message,
      })
      if (files.length) await quoteApi.upload(q.id, files)
      setFiles([])
      setDone(true)
      onSaved()
    } catch (err) {
      if (err instanceof ApiError) setErrors(err.fieldErrors)
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function withdraw() {
    if (!mine || !window.confirm('Withdraw your quote?')) return
    await quoteApi.withdraw(mine.id).catch((err) => setError(errorMessage(err)))
    onSaved()
  }

  return (
    <div className="card">
      <div className="card-head">
        <h2 className="card-title">{mine ? 'Your quote' : 'Send a quote'}</h2>
        {mine && <Badge value={mine.status} />}
      </div>
      {mine?.status === 'ACCEPTED' && <Alert tone="success">The buyer accepted your quote. Check your orders.</Alert>}
      {mine?.status === 'REJECTED' && <Alert tone="info">The buyer chose another offer this time.</Alert>}
      {!editable && !mine && <Alert tone="info">This request is closed.</Alert>}
      {done && <Alert tone="success">Quote sent. The buyer has been notified.</Alert>}
      {error && <Alert tone="error">{error}</Alert>}
      {editable && (
        <form className="form" onSubmit={submit} noValidate>
          <div className="form-row">
            <TextField label={`Price per ${r.unit} (QAR)`} type="number" step="0.01" min="0" value={form.pricePerUnit} onChange={set('pricePerUnit')} error={errors.pricePerUnit} required />
            <TextField label="Quantity" type="number" min="1" value={form.quantity} onChange={set('quantity')} error={errors.quantity} required />
          </div>
          <div className="form-row">
            <TextField label="Delivery (days)" type="number" min="0" value={form.deliveryDays} onChange={set('deliveryDays')} />
            <SelectField label="Link my product" value={form.productId} onChange={set('productId')}>
              <option value="">— None —</option>
              {products?.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </SelectField>
          </div>
          <TextArea label="Message to buyer" value={form.message} onChange={set('message')} rows={3} />
          {mine && mine.media.length > 0 && <MediaList media={mine.media} />}
          <MediaPicker files={files} onChange={setFiles} max={4} />
          <div className="summary-box">
            <span>Total</span>
            <b>{formatMoney((Number(form.pricePerUnit) || 0) * (Number(form.quantity) || 0))}</b>
          </div>
          <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Sending…' : mine ? 'Update quote' : 'Send quote'}</button>
          {mine?.status === 'PENDING' && <button type="button" className="btn btn-ghost btn-block" onClick={withdraw}>Withdraw quote</button>}
        </form>
      )}
    </div>
  )
}

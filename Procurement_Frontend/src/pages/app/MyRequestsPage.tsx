import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { CalendarClock, ClipboardList, MapPin, Plus, Trash2, XCircle } from 'lucide-react'
import { ApiError, errorMessage } from '../../api/client'
import { publicApi, requestApi } from '../../api/endpoints'
import type { BuyRequest, BuyRequestForm, Category } from '../../api/types'
import { MediaPicker } from '../../components/Media'
import { Pagination } from '../../components/Pagination'
import { Alert, AsyncBlock, Badge, EmptyState, Modal, PageHeader, SelectField, TextArea, TextField, Thumb } from '../../components/ui'
import { UNITS } from '../../lib/constants'
import { formatDate, formatMoney, formatNumber, timeAgo } from '../../lib/format'
import { useApi } from '../../lib/useApi'
import { usePagination } from '../../lib/usePagination'

/** Buyer: my buying requests. Admin: all requests (admin=true). */
export function MyRequestsPage({ admin = false }: { admin?: boolean }) {
  const [params, setParams] = useSearchParams()
  const { data, loading, error, reload } = useApi(() => (admin ? requestApi.all() : requestApi.mine()), [admin])
  const [picked, setPicked] = useState<BuyRequest | 'new' | null>(null)
  const [message, setMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)
  const paging = usePagination(data ?? [], 8)

  // /app/my-requests?new=1 opens the form directly (e.g. from the dashboard button).
  const editing = picked ?? (params.get('new') ? 'new' : null)
  const setEditing = (v: BuyRequest | 'new' | null) => {
    setPicked(v)
    if (v === null && params.get('new')) setParams({}, { replace: true })
  }

  async function close(r: BuyRequest) {
    if (!window.confirm(`Close "${r.title}"? Suppliers will no longer be able to quote.`)) return
    try {
      await requestApi.close(r.id)
      reload()
    } catch (err) {
      setMessage({ tone: 'error', text: errorMessage(err) })
    }
  }

  async function remove(r: BuyRequest) {
    if (!window.confirm(`Delete "${r.title}" and all its quotes?`)) return
    try {
      await requestApi.remove(r.id)
      setMessage({ tone: 'success', text: 'Request deleted.' })
      reload()
    } catch (err) {
      setMessage({ tone: 'error', text: errorMessage(err) })
    }
  }

  return (
    <>
      <PageHeader
        title={admin ? 'All buying requests' : 'My buying requests'}
        subtitle={admin ? 'Every request posted by buyers.' : 'Tell suppliers what you need. They reply with quotes, photos and delivery time.'}
        actions={!admin && <button className="btn btn-primary" onClick={() => setEditing('new')}><Plus size={16} /> Post a request</button>}
      />
      {message && <Alert tone={message.tone}>{message.text}</Alert>}
      <AsyncBlock loading={loading} error={error}>
        {data?.length === 0 ? (
          <EmptyState icon={<ClipboardList size={32} />} title="No buying requests yet"
            text="Post what you want to buy. Every supplier will be notified."
            action={!admin && <button className="btn btn-primary" onClick={() => setEditing('new')}><Plus size={16} /> Post a request</button>} />
        ) : (
          <div className="request-list">
            {paging.pageItems.map((r) => (
              <div className="request-card card" key={r.id}>
                <Thumb src={r.imageUrl} alt={r.title} size="sm" />
                <div className="grow">
                  <div className="request-top">
                    <Link to={`/app/requests/${r.id}`}><h3>{r.title}</h3></Link>
                    <Badge value={r.status} />
                  </div>
                  <div className="request-facts">
                    <span><b>{formatNumber(r.quantity)}</b> {r.unit}</span>
                    {r.targetPrice !== null && <span>Budget <b>{formatMoney(r.targetPrice)}</b> / {r.unit}</span>}
                    {r.neededBy && <span><CalendarClock size={14} aria-hidden="true" /> by {formatDate(r.neededBy)}</span>}
                    {r.deliveryCity && <span><MapPin size={14} aria-hidden="true" /> {r.deliveryCity}</span>}
                  </div>
                  <p className="small muted">{admin ? `${r.buyer.companyName} · ` : ''}Posted {timeAgo(r.createdAt)}</p>
                </div>
                <div className="request-side">
                  <Link to={`/app/requests/${r.id}`} className={`quote-count${r.quoteCount ? ' has' : ''}`}>
                    <b>{r.quoteCount}</b> quote{r.quoteCount === 1 ? '' : 's'}
                  </Link>
                  <div className="row-actions">
                    {r.status === 'OPEN' && <button className="icon-btn" title="Close request" aria-label={`Close ${r.title}`} onClick={() => close(r)}><XCircle size={16} /></button>}
                    <button className="icon-btn danger" title="Delete" aria-label={`Delete ${r.title}`} onClick={() => remove(r)}><Trash2 size={16} /></button>
                  </div>
                </div>
              </div>
            ))}
            <Pagination {...paging.props} />
          </div>
        )}
      </AsyncBlock>
      {editing && (
        <RequestFormModal
          request={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            setMessage({ tone: 'success', text: 'Your request was posted. Suppliers have been notified.' })
            reload()
          }}
        />
      )}
    </>
  )
}

export function RequestFormModal({ request, onClose, onSaved }: { request: BuyRequest | null; onClose: () => void; onSaved: (r: BuyRequest) => void }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [form, setForm] = useState({
    title: request?.title ?? '',
    description: request?.description ?? '',
    categoryId: request?.category ? String(request.category.id) : '',
    quantity: request ? String(request.quantity) : '',
    unit: request?.unit ?? 'carton',
    targetPrice: request?.targetPrice != null ? String(request.targetPrice) : '',
    neededBy: request?.neededBy ?? '',
    deliveryCity: request?.deliveryCity ?? 'Doha',
  })
  const [files, setFiles] = useState<File[]>([])
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    publicApi.categories().then(setCategories).catch(() => {})
  }, [])
  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    const body: BuyRequestForm = {
      title: form.title,
      description: form.description,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      quantity: Number(form.quantity),
      unit: form.unit,
      targetPrice: form.targetPrice ? Number(form.targetPrice) : null,
      neededBy: form.neededBy || null,
      deliveryCity: form.deliveryCity,
    }
    try {
      let saved = request ? await requestApi.update(request.id, body) : await requestApi.create(body)
      if (files.length) saved = await requestApi.upload(saved.id, files)
      onSaved(saved)
    } catch (err) {
      if (err instanceof ApiError) setErrors(err.fieldErrors)
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <Modal title={request ? 'Edit buying request' : 'Post a buying request'} onClose={onClose} wide
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button><button className="btn btn-primary" form="request-form" disabled={busy}>{busy ? 'Saving…' : request ? 'Save' : 'Post request'}</button></>}>
      <form id="request-form" className="form" onSubmit={submit} noValidate>
        {error && <Alert tone="error">{error}</Alert>}
        <TextField label="What do you want to buy?" value={form.title} onChange={set('title')} error={errors.title} required placeholder="e.g. Printed paper napkins with our logo" />
        <div className="form-row form-row-3">
          <TextField label="Quantity" type="number" min="1" value={form.quantity} onChange={set('quantity')} error={errors.quantity} required />
          <SelectField label="Unit" value={form.unit} onChange={set('unit')}>
            {UNITS.map((u) => <option key={u}>{u}</option>)}
          </SelectField>
          <SelectField label="Category" value={form.categoryId} onChange={set('categoryId')}>
            <option value="">— Choose —</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </SelectField>
        </div>
        <div className="form-row form-row-3">
          <TextField label="Budget per unit (QAR)" type="number" step="0.01" min="0" value={form.targetPrice} onChange={set('targetPrice')} error={errors.targetPrice} hint="Optional" />
          <TextField label="Needed by" type="date" value={form.neededBy} onChange={set('neededBy')} />
          <TextField label="Delivery city" value={form.deliveryCity} onChange={set('deliveryCity')} />
        </div>
        <TextArea label="Details" value={form.description} onChange={set('description')} placeholder="Size, material, colour, certificates, packing…" />
        <div className="field">
          <span className="label">Reference photos (optional)</span>
          <MediaPicker files={files} onChange={setFiles} max={4} />
        </div>
      </form>
    </Modal>
  )
}

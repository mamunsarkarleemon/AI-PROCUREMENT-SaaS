import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { Eye, EyeOff, PackagePlus, Pencil, Trash2 } from 'lucide-react'
import { ApiError, errorMessage } from '../../api/client'
import { mediaApi, productApi, publicApi } from '../../api/endpoints'
import type { Category, Media, Product, ProductForm } from '../../api/types'
import { MediaList, MediaPicker } from '../../components/Media'
import { Pagination } from '../../components/Pagination'
import { Alert, AsyncBlock, Badge, EmptyState, Modal, PageHeader, SelectField, TextArea, TextField, Thumb } from '../../components/ui'
import { UNITS } from '../../lib/constants'
import { formatDate, formatMoney, formatNumber } from '../../lib/format'
import { useApi } from '../../lib/useApi'
import { usePagination } from '../../lib/usePagination'

/** Supplier: add, edit, hide and delete products. Also used by admin for "All Products" (admin=true). */
export function MyProductsPage({ admin = false }: { admin?: boolean }) {
  const [params, setParams] = useSearchParams()
  const { data, loading, error, reload } = useApi(() => (admin ? productApi.all() : productApi.mine()), [admin])
  const [picked, setPicked] = useState<Product | 'new' | null>(null)
  const [message, setMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)
  const [filter, setFilter] = useState('')

  // Links like /app/my-products?new=1 or ?edit=5 open the form directly.
  const editId = Number(params.get('edit')) || null
  const fromLink: Product | 'new' | null = params.get('new')
    ? 'new'
    : editId && data ? data.find((x) => x.id === editId) ?? null : null
  const editing = picked ?? fromLink
  const setEditing = (v: Product | 'new' | null) => {
    setPicked(v)
    if (v === null && (params.get('new') || params.get('edit'))) setParams({}, { replace: true })
  }

  async function toggle(p: Product) {
    try {
      await productApi.setStatus(p.id, p.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
      setMessage({ tone: 'success', text: `${p.name} is now ${p.status === 'ACTIVE' ? 'hidden' : 'visible'} in the marketplace.` })
      reload()
    } catch (err) {
      setMessage({ tone: 'error', text: errorMessage(err) })
    }
  }

  async function remove(p: Product) {
    if (!window.confirm(`Delete "${p.name}"? This cannot be undone.`)) return
    try {
      await productApi.remove(p.id)
      setMessage({ tone: 'success', text: `${p.name} was deleted.` })
      reload()
    } catch (err) {
      setMessage({ tone: 'error', text: errorMessage(err) })
    }
  }

  const rows = (data ?? []).filter((p) =>
    !filter || `${p.name} ${p.supplier.companyName} ${p.category?.name ?? ''}`.toLowerCase().includes(filter.toLowerCase()))
  const paging = usePagination(rows, 10, filter)

  return (
    <>
      <PageHeader
        title={admin ? 'All products' : 'My products'}
        subtitle={admin ? 'Every product on the platform. Hide or remove anything that breaks the rules.' : 'Products you sell. Active products are visible to every buyer.'}
        actions={!admin && <button className="btn btn-primary" onClick={() => setEditing('new')}><PackagePlus size={16} /> Add product</button>}
      />
      {message && <Alert tone={message.tone}>{message.text}</Alert>}
      <AsyncBlock loading={loading} error={error}>
        {data && data.length === 0 ? (
          <EmptyState title="No products yet" text={admin ? 'Suppliers have not listed any products.' : 'Add your first product with photos, price and stock.'}
            action={!admin && <button className="btn btn-primary" onClick={() => setEditing('new')}><PackagePlus size={16} /> Add product</button>} />
        ) : (
          <section className="card">
            <div className="card-head">
              <h2>{formatNumber(rows.length)} products</h2>
              <input className="input-sm" type="search" placeholder="Filter…" value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Filter products" />
            </div>
            <div className="table-wrap">
              <table className="table table-stack">
                <thead>
                  <tr>
                    <th>Product</th>
                    {admin && <th>Supplier</th>}
                    <th>Category</th>
                    <th className="num">Price</th>
                    <th className="num">Stock</th>
                    <th className="num">MOQ</th>
                    <th>Status</th>
                    <th>Updated</th>
                    <th className="actions-col">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {paging.pageItems.map((p) => (
                    <tr key={p.id}>
                      <td className="stack-full">
                        <div className="cell-product">
                          <Thumb src={p.imageUrl} alt={p.name} size="sm" />
                          <Link to={`/app/products/${p.id}`} className="strong">{p.name}</Link>
                        </div>
                      </td>
                      {admin && <td data-label="Supplier">{p.supplier.companyName}</td>}
                      <td data-label="Category">{p.category?.name ?? '—'}</td>
                      <td className="num" data-label="Price">{formatMoney(p.price)} <small className="muted">/{p.unit}</small></td>
                      <td className="num" data-label="Stock">
                        <span className={p.stockQty < p.minOrderQty ? 'text-danger strong' : ''}>{formatNumber(p.stockQty)}</span>
                      </td>
                      <td className="num" data-label="MOQ">{formatNumber(p.minOrderQty)}</td>
                      <td data-label="Status"><Badge value={p.status} /></td>
                      <td className="nowrap" data-label="Updated">{formatDate(p.updatedAt)}</td>
                      <td className="actions-col stack-actions">
                        <div className="row-actions">
                          <button className="icon-btn" title="Edit" aria-label={`Edit ${p.name}`} onClick={() => setEditing(p)}><Pencil size={16} /></button>
                          <button className="icon-btn" title={p.status === 'ACTIVE' ? 'Hide' : 'Show'} aria-label={p.status === 'ACTIVE' ? `Hide ${p.name}` : `Show ${p.name}`} onClick={() => toggle(p)}>
                            {p.status === 'ACTIVE' ? <EyeOff size={16} /> : <Eye size={16} />}
                          </button>
                          <button className="icon-btn danger" title="Delete" aria-label={`Delete ${p.name}`} onClick={() => remove(p)}><Trash2 size={16} /></button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination {...paging.props} />
          </section>
        )}
      </AsyncBlock>
      {editing && (
        <ProductFormModal
          product={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={(p, isNew) => {
            setEditing(null)
            setMessage({ tone: 'success', text: `${p.name} was ${isNew ? 'added' : 'saved'}.` })
            reload()
          }}
        />
      )}
    </>
  )
}

function ProductFormModal({ product, onClose, onSaved }: { product: Product | null; onClose: () => void; onSaved: (p: Product, isNew: boolean) => void }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [form, setForm] = useState({
    name: product?.name ?? '',
    description: product?.description ?? '',
    categoryId: product?.category?.id ? String(product.category.id) : '',
    unit: product?.unit ?? 'carton',
    price: product ? String(product.price) : '',
    stockQty: product ? String(product.stockQty) : '',
    minOrderQty: product ? String(product.minOrderQty) : '1',
    status: product?.status ?? 'ACTIVE',
  })
  const [media, setMedia] = useState<Media[]>(product?.media ?? [])
  const [files, setFiles] = useState<File[]>([])
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    publicApi.categories().then(setCategories).catch(() => {})
  }, [])

  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function deleteMedia(m: Media) {
    if (!window.confirm('Delete this file?')) return
    try {
      await mediaApi.remove(m.id)
      setMedia((list) => list.filter((x) => x.id !== m.id))
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setErrors({})
    const body: ProductForm = {
      name: form.name,
      description: form.description,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      unit: form.unit,
      price: Number(form.price),
      stockQty: Number(form.stockQty),
      minOrderQty: Number(form.minOrderQty),
      status: form.status as ProductForm['status'],
    }
    try {
      let saved = product ? await productApi.update(product.id, body) : await productApi.create(body)
      if (files.length) saved = await productApi.upload(saved.id, files)
      onSaved(saved, !product)
    } catch (err) {
      if (err instanceof ApiError) setErrors(err.fieldErrors)
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <Modal title={product ? `Edit ${product.name}` : 'Add a product'} onClose={onClose} wide
      footer={<><button className="btn btn-ghost" onClick={onClose}>Cancel</button><button className="btn btn-primary" form="product-form" disabled={busy}>{busy ? 'Saving…' : 'Save product'}</button></>}>
      <form id="product-form" className="form" onSubmit={submit} noValidate>
        {error && <Alert tone="error">{error}</Alert>}
        <TextField label="Product name" value={form.name} onChange={set('name')} error={errors.name} required placeholder="e.g. Kraft Paper Food Box 750ml" />
        <div className="form-row">
          <SelectField label="Category" value={form.categoryId} onChange={set('categoryId')}>
            <option value="">— Choose —</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </SelectField>
          <SelectField label="Status" value={form.status} onChange={set('status')} hint="Inactive products are hidden from buyers.">
            <option value="ACTIVE">Active (visible)</option>
            <option value="INACTIVE">Inactive (hidden)</option>
          </SelectField>
        </div>
        <div className="form-row form-row-4">
          <TextField label="Price per unit (QAR)" type="number" step="0.01" min="0" value={form.price} onChange={set('price')} error={errors.price} required />
          <SelectField label="Unit" value={form.unit} onChange={set('unit')} error={errors.unit}>
            {UNITS.map((u) => <option key={u}>{u}</option>)}
          </SelectField>
          <TextField label="Stock quantity" type="number" min="0" value={form.stockQty} onChange={set('stockQty')} error={errors.stockQty} required />
          <TextField label="Minimum order (MOQ)" type="number" min="1" value={form.minOrderQty} onChange={set('minOrderQty')} error={errors.minOrderQty} required />
        </div>
        <TextArea label="Description" value={form.description} onChange={set('description')} placeholder="Material, size, packing, certificates…" />
        <div className="field">
          <span className="label">Photos & short video</span>
          <MediaList media={media} onDelete={deleteMedia} />
          <MediaPicker files={files} onChange={setFiles} />
        </div>
      </form>
    </Modal>
  )
}

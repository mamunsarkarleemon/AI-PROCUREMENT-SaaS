import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { CalendarClock, ClipboardList, MapPin, Search, Store } from 'lucide-react'
import { productApi, publicApi, requestApi } from '../../api/endpoints'
import type { Category } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { AsyncBlock, Badge, EmptyState, PageHeader, Thumb } from '../../components/ui'
import { formatDate, formatMoney, formatNumber, timeAgo } from '../../lib/format'
import { useApi } from '../../lib/useApi'

/** Common page for buyers and suppliers: all products + all open buying requests. */
export function MarketplacePage() {
  const { user, hasMenu } = useAuth()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') === 'requests' ? 'requests' : 'products'
  const [q, setQ] = useState('')
  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [categories, setCategories] = useState<Category[]>([])

  useEffect(() => {
    publicApi.categories().then(setCategories).catch(() => {})
  }, [])

  // Search 300 ms after the user stops typing.
  useEffect(() => {
    const t = window.setTimeout(() => setSearch(q), 300)
    return () => window.clearTimeout(t)
  }, [q])

  const products = useApi(() => (tab === 'products' ? productApi.marketplace(search, categoryId) : Promise.resolve([])), [tab, search, categoryId])
  const requests = useApi(() => (tab === 'requests' ? requestApi.open(search, categoryId) : Promise.resolve([])), [tab, search, categoryId])

  return (
    <>
      <PageHeader
        title="Marketplace"
        subtitle="Products from every supplier and what buyers are looking for."
        actions={
          user?.role === 'BUYER' && hasMenu('my_requests') ? (
            <Link className="btn btn-primary" to="/app/my-requests?new=1"><ClipboardList size={16} /> Post a buying request</Link>
          ) : user?.role === 'SUPPLIER' && hasMenu('my_products') ? (
            <Link className="btn btn-primary" to="/app/my-products?new=1">Add my product</Link>
          ) : null
        }
      />

      <div className="toolbar card">
        <div className="tabs" role="tablist">
          <button role="tab" aria-selected={tab === 'products'} className={`tab${tab === 'products' ? ' active' : ''}`} onClick={() => setParams({})}>
            <Store size={16} /> Products
          </button>
          <button role="tab" aria-selected={tab === 'requests'} className={`tab${tab === 'requests' ? ' active' : ''}`} onClick={() => setParams({ tab: 'requests' })}>
            <ClipboardList size={16} /> Buying requests
          </button>
        </div>
        <div className="toolbar-filters">
          <label className="search-box">
            <Search size={16} aria-hidden="true" />
            <input type="search" placeholder={tab === 'products' ? 'Search products or suppliers…' : 'Search requests…'} value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search" />
          </label>
          <select value={categoryId ?? ''} onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : null)} aria-label="Category">
            <option value="">All categories</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
      </div>

      {tab === 'products' ? (
        <AsyncBlock loading={products.loading} error={products.error}>
          {products.data?.length === 0 ? (
            <EmptyState title="No products found" text="Try another search or category." />
          ) : (
            <div className="product-grid">
              {products.data?.map((p) => (
                <Link to={`/app/products/${p.id}`} className="product-card" key={p.id}>
                  <Thumb src={p.imageUrl} alt={p.name} />
                  <div className="product-card-body">
                    {p.category && <span className="product-cat">{p.category.name}</span>}
                    <h3>{p.name}</h3>
                    <p className="product-price">{formatMoney(p.price)} <small>/ {p.unit}</small></p>
                    <p className="product-meta">MOQ {formatNumber(p.minOrderQty)} · Stock {formatNumber(p.stockQty)} {p.unit}</p>
                    <p className="product-supplier">
                      {p.supplier.companyName}
                      {p.supplier.city && <span><MapPin size={12} aria-hidden="true" /> {p.supplier.city}</span>}
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </AsyncBlock>
      ) : (
        <AsyncBlock loading={requests.loading} error={requests.error}>
          {requests.data?.length === 0 ? (
            <EmptyState icon={<ClipboardList size={32} />} title="No open buying requests" text="When buyers post what they need, it shows up here." />
          ) : (
            <div className="request-list">
              {requests.data?.map((r) => (
                <Link to={`/app/requests/${r.id}`} className="request-card card" key={r.id}>
                  <Thumb src={r.imageUrl} alt={r.title} size="sm" />
                  <div className="grow">
                    <div className="request-top">
                      <h3>{r.title}</h3>
                      <Badge value={r.status} />
                    </div>
                    <p className="muted clamp-2">{r.description}</p>
                    <div className="request-facts">
                      <span><b>{formatNumber(r.quantity)}</b> {r.unit}</span>
                      {r.targetPrice !== null && <span>Budget <b>{formatMoney(r.targetPrice)}</b> / {r.unit}</span>}
                      {r.neededBy && <span><CalendarClock size={14} aria-hidden="true" /> by {formatDate(r.neededBy)}</span>}
                      {r.deliveryCity && <span><MapPin size={14} aria-hidden="true" /> {r.deliveryCity}</span>}
                      <span>{r.quoteCount} quote{r.quoteCount === 1 ? '' : 's'}</span>
                    </div>
                    <p className="small muted">{r.buyer.companyName} · {timeAgo(r.createdAt)}{r.category ? ` · ${r.category.name}` : ''}</p>
                  </div>
                  {user?.role === 'SUPPLIER' && (
                    <span className="btn btn-primary btn-sm">{r.quotes.length ? 'View my quote' : 'Send quote'}</span>
                  )}
                </Link>
              ))}
            </div>
          )}
        </AsyncBlock>
      )}
    </>
  )
}

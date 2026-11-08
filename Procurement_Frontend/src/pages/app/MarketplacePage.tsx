import { useEffect, useState, type ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router'
import { CalendarClock, ClipboardList, MapPin, Search, Star, Store, TrendingUp } from 'lucide-react'
import { productApi, publicApi, requestApi } from '../../api/endpoints'
import type { Category, PageResult, Product, ProductHighlights } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Pagination } from '../../components/Pagination'
import { ProductCard } from '../../components/ProductCard'
import { AsyncBlock, Badge, EmptyState, PageHeader, Thumb } from '../../components/ui'
import { formatDate, formatMoney, formatNumber, timeAgo } from '../../lib/format'
import { useApi } from '../../lib/useApi'
import { usePagination } from '../../lib/usePagination'

const PRODUCTS_PER_PAGE = 12

const SORTS = [
  { value: 'newest', label: 'Newest first' },
  { value: 'rating', label: 'Top rated' },
  { value: 'selling', label: 'Top selling' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
]

function HighlightRow({ title, icon, products }: { title: string; icon: ReactNode; products: Product[] }) {
  if (products.length === 0) return null
  return (
    <section className="market-section">
      <h2>{icon} {title}</h2>
      <div className="product-grid product-grid-top">
        {products.map((p) => <ProductCard product={p} key={p.id} />)}
      </div>
    </section>
  )
}

/** Common page for all roles: all products + all open buying requests. */
export function MarketplacePage() {
  const { user, hasMenu } = useAuth()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') === 'requests' ? 'requests' : 'products'
  const [q, setQ] = useState('')
  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [sort, setSort] = useState('newest')
  const [page, setPage] = useState(1)

  // A new search, category, sort or tab starts again from page 1.
  const filterKey = `${tab}|${search}|${categoryId}|${sort}`
  const [lastFilterKey, setLastFilterKey] = useState(filterKey)
  if (lastFilterKey !== filterKey) {
    setLastFilterKey(filterKey)
    setPage(1)
  }

  useEffect(() => {
    publicApi.categories().then(setCategories).catch(() => {})
  }, [])

  // Search 300 ms after the user stops typing.
  useEffect(() => {
    const t = window.setTimeout(() => setSearch(q), 300)
    return () => window.clearTimeout(t)
  }, [q])

  const products = useApi<PageResult<Product> | null>(
    () => (tab === 'products' ? productApi.page({ q: search, categoryId, sort, page: page - 1, size: PRODUCTS_PER_PAGE }) : Promise.resolve(null)),
    [tab, search, categoryId, sort, page],
  )
  const requests = useApi(() => (tab === 'requests' ? requestApi.open(search, categoryId) : Promise.resolve([])), [tab, search, categoryId])
  const requestPages = usePagination(requests.data ?? [], 8, filterKey)

  // Top rated / top selling rows show on the plain marketplace, not on search results.
  const showHighlights = tab === 'products' && !search && !categoryId
  const highlights = useApi<ProductHighlights | null>(() => (showHighlights ? productApi.highlights() : Promise.resolve(null)), [showHighlights])

  const lastPage = products.data?.totalPages ?? 0
  if (lastPage > 0 && page > lastPage) setPage(lastPage)

  return (
    <>
      <PageHeader
        title="Marketplace"
        subtitle={user?.role === 'ADMIN'
          ? 'Every product on sale with its price, availability, rating and supplier. Open a product for full details.'
          : 'Products from every supplier and what buyers are looking for.'}
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
          {tab === 'products' && (
            <select value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort products">
              {SORTS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          )}
        </div>
      </div>

      {tab === 'products' ? (
        <>
          {showHighlights && page === 1 && highlights.data && (
            <>
              <HighlightRow title="Top rated products" icon={<Star size={20} className="icon-star" fill="currentColor" aria-hidden="true" />} products={highlights.data.topRated} />
              <HighlightRow title="Top selling products" icon={<TrendingUp size={20} className="icon-selling" aria-hidden="true" />} products={highlights.data.topSelling} />
            </>
          )}
          <AsyncBlock loading={products.loading} error={products.error}>
            {products.data?.items.length === 0 ? (
              <EmptyState title="No products found" text="Try another search or category." />
            ) : (
              <section className="market-section">
                {showHighlights && <h2><Store size={20} aria-hidden="true" /> All products</h2>}
                <div className="product-grid">
                  {products.data?.items.map((p) => <ProductCard product={p} key={p.id} />)}
                </div>
                {products.data && <Pagination page={page} totalPages={products.data.totalPages} totalItems={products.data.totalItems} pageSize={PRODUCTS_PER_PAGE} onChange={setPage} />}
              </section>
            )}
          </AsyncBlock>
        </>
      ) : (
        <AsyncBlock loading={requests.loading} error={requests.error}>
          {requests.data?.length === 0 ? (
            <EmptyState icon={<ClipboardList size={32} />} title="No open buying requests" text="When buyers post what they need, it shows up here." />
          ) : (
            <div className="request-list">
              {requestPages.pageItems.map((r) => (
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
              <Pagination {...requestPages.props} />
            </div>
          )}
        </AsyncBlock>
      )}
    </>
  )
}

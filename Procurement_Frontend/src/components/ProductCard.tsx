import { Link } from 'react-router'
import { MapPin } from 'lucide-react'
import type { Product } from '../api/types'
import { formatMoney, formatNumber } from '../lib/format'
import { RatingLine } from './Rating'
import { Badge, Thumb } from './ui'

/** In stock / Low stock (less than one minimum order) / Out of stock. */
export function StockBadge({ product: p }: { product: Product }) {
  if (p.stockQty <= 0) return <Badge value="CANCELLED" label="Out of stock" />
  if (p.stockQty < p.minOrderQty) return <Badge value="PENDING" label="Low stock" />
  return <Badge value="ACTIVE" label="In stock" />
}

/** One product in the marketplace: photo, rating, price, availability and supplier. */
export function ProductCard({ product: p }: { product: Product }) {
  return (
    <Link to={`/app/products/${p.id}`} className="product-card">
      <Thumb src={p.imageUrl} alt={p.name} />
      <div className="product-card-body">
        {p.category && <span className="product-cat">{p.category.name}</span>}
        <h3>{p.name}</h3>
        <RatingLine value={p.ratingAvg} count={p.ratingCount} />
        <p className="product-price">{formatMoney(p.price)} <small>/ {p.unit}</small></p>
        <p className="product-meta">MOQ {formatNumber(p.minOrderQty)} · Stock {formatNumber(p.stockQty)} {p.unit}</p>
        <p className="product-flags">
          <StockBadge product={p} />
          {p.soldQty > 0 && <span className="product-sold">{formatNumber(p.soldQty)} sold</span>}
        </p>
        <p className="product-supplier">
          {p.supplier.companyName}
          {p.supplier.city && <span><MapPin size={12} aria-hidden="true" /> {p.supplier.city}</span>}
        </p>
      </div>
    </Link>
  )
}

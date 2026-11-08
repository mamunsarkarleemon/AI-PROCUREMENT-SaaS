import { useNavigate } from 'react-router'
import type { Order, Role } from '../api/types'
import { formatDate, formatMoney, formatNumber } from '../lib/format'
import { Badge, Thumb } from './ui'

/** Orders list used on the dashboard, Orders page and Reports page. Click a row to open it. */
export function OrdersTable({ orders, viewer }: { orders: Order[]; viewer: Role }) {
  const navigate = useNavigate()
  return (
    <div className="table-wrap">
      <table className="table table-stack">
        <thead>
          <tr>
            <th>Order</th>
            <th>Product</th>
            {viewer !== 'SUPPLIER' && <th>Supplier</th>}
            {viewer !== 'BUYER' && <th>Buyer</th>}
            <th className="num">Qty</th>
            <th className="num">Total</th>
            <th>Status</th>
            <th>Date</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.id} className="clickable" onClick={() => navigate(`/app/orders/${o.id}`)}>
              <td className="nowrap stack-title">
                <a href={`/app/orders/${o.id}`} onClick={(e) => { e.preventDefault(); navigate(`/app/orders/${o.id}`) }} className="strong">
                  {o.orderNo}
                </a>
              </td>
              <td className="stack-full">
                <div className="cell-product">
                  <Thumb src={o.productImage} alt={o.productName} size="sm" />
                  <span>{o.productName}</span>
                </div>
              </td>
              {viewer !== 'SUPPLIER' && <td data-label="Supplier">{o.supplier.companyName}</td>}
              {viewer !== 'BUYER' && <td data-label="Buyer">{o.buyer.companyName}</td>}
              <td className="num" data-label="Qty">{formatNumber(o.quantity)} {o.unit}</td>
              <td className="num strong" data-label="Total">{formatMoney(o.totalAmount)}</td>
              <td className="stack-status"><Badge value={o.status} /></td>
              <td className="nowrap" data-label="Date">{formatDate(o.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

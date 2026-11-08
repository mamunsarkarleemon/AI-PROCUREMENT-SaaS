import { useNavigate } from 'react-router'
import type { Order, Role } from '../api/types'
import { formatDate, formatMoney, formatNumber } from '../lib/format'
import { Badge, Thumb } from './ui'

/** Orders list used on the dashboard, Orders page and Reports page. Click a row to open it. */
export function OrdersTable({ orders, viewer }: { orders: Order[]; viewer: Role }) {
  const navigate = useNavigate()
  return (
    <div className="table-wrap">
      <table className="table">
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
              <td className="nowrap">
                <a href={`/app/orders/${o.id}`} onClick={(e) => { e.preventDefault(); navigate(`/app/orders/${o.id}`) }} className="strong">
                  {o.orderNo}
                </a>
              </td>
              <td>
                <div className="cell-product">
                  <Thumb src={o.productImage} alt={o.productName} size="sm" />
                  <span>{o.productName}</span>
                </div>
              </td>
              {viewer !== 'SUPPLIER' && <td>{o.supplier.companyName}</td>}
              {viewer !== 'BUYER' && <td>{o.buyer.companyName}</td>}
              <td className="num">{formatNumber(o.quantity)} {o.unit}</td>
              <td className="num strong">{formatMoney(o.totalAmount)}</td>
              <td><Badge value={o.status} /></td>
              <td className="nowrap">{formatDate(o.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

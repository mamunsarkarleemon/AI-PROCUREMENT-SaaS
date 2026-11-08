import { Link } from 'react-router'
import { ArrowRight, ClipboardList, PackagePlus, Search, ShoppingCart, Users } from 'lucide-react'
import { dashboardApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { AmountChart, StatCard } from '../../components/Charts'
import { OrdersTable } from '../../components/OrdersTable'
import { AsyncBlock, Avatar, Badge, EmptyState, PageHeader, Thumb } from '../../components/ui'
import { formatDate, formatMoney, formatNumber } from '../../lib/format'
import { useApi } from '../../lib/useApi'

export function DashboardPage() {
  const { user, hasMenu } = useAuth()
  const { data, loading, error } = useApi(() => dashboardApi.get(), [])
  if (!user) return null
  const firstName = user.fullName.split(' ')[0]

  const actions =
    user.role === 'BUYER' ? (
      <>
        {hasMenu('marketplace') && <Link className="btn btn-ghost" to="/app/marketplace"><Search size={16} /> Browse products</Link>}
        {hasMenu('my_requests') && <Link className="btn btn-primary" to="/app/my-requests?new=1"><ClipboardList size={16} /> Post a buying request</Link>}
      </>
    ) : user.role === 'SUPPLIER' ? (
      <>
        {hasMenu('marketplace') && <Link className="btn btn-ghost" to="/app/marketplace?tab=requests"><ClipboardList size={16} /> Buyer requests</Link>}
        {hasMenu('my_products') && <Link className="btn btn-primary" to="/app/my-products?new=1"><PackagePlus size={16} /> Add product</Link>}
      </>
    ) : (
      <>
        <Link className="btn btn-ghost" to="/app/reports">Reports</Link>
        <Link className="btn btn-primary" to="/app/admin/users"><Users size={16} /> Manage users</Link>
      </>
    )

  return (
    <>
      <PageHeader title={`Welcome back, ${firstName}`} subtitle="Here’s what is happening with your business today." actions={actions} />
      <AsyncBlock loading={loading} error={error}>
        {data && (
          <>
            <div className="stat-grid">
              {data.cards.map((c) => <StatCard key={c.label} card={c} />)}
            </div>

            <div className="grid-2-1">
              <section className="card">
                <div className="card-head">
                  <div>
                    <h2>{data.chartTitle}</h2>
                    <p className="muted">Total {formatMoney(data.chart.reduce((s, d) => s + d.amount, 0))} · {data.chart.reduce((s, d) => s + d.orders, 0)} orders</p>
                  </div>
                  {hasMenu('reports') && <Link to="/app/reports" className="link">Full report <ArrowRight size={14} /></Link>}
                </div>
                <AmountChart data={data.chart} />
              </section>

              {user.role === 'ADMIN' ? (
                <section className="card">
                  <div className="card-head">
                    <h2>Newest users</h2>
                    <Link to="/app/admin/users" className="link">All users <ArrowRight size={14} /></Link>
                  </div>
                  <ul className="simple-list">
                    {data.recentUsers.map((u) => (
                      <li key={u.id}>
                        <Avatar name={u.fullName} size={34} />
                        <span className="grow">
                          <b>{u.fullName}</b>
                          <small>{u.companyName ?? u.email} · {formatDate(u.createdAt)}</small>
                        </span>
                        <Badge value={u.role} />
                      </li>
                    ))}
                  </ul>
                </section>
              ) : (
                <section className="card">
                  <div className="card-head">
                    <h2>{data.productsTitle}</h2>
                    {user.role === 'BUYER'
                      ? hasMenu('marketplace') && <Link to="/app/marketplace" className="link">Marketplace <ArrowRight size={14} /></Link>
                      : hasMenu('my_products') && <Link to="/app/my-products" className="link">My products <ArrowRight size={14} /></Link>}
                  </div>
                  {data.products.length === 0 ? (
                    <p className="muted">{user.role === 'SUPPLIER' ? 'Add your first product to start selling.' : 'No products yet.'}</p>
                  ) : (
                    <ul className="simple-list">
                      {data.products.map((p) => (
                        <li key={p.id}>
                          <Thumb src={p.imageUrl} alt={p.name} size="sm" />
                          <span className="grow">
                            <Link to={`/app/products/${p.id}`}><b>{p.name}</b></Link>
                            <small>{user.role === 'BUYER' ? p.supplier.companyName : `${formatMoney(p.price)} / ${p.unit}`}</small>
                          </span>
                          {user.role === 'BUYER' ? (
                            <span className="strong nowrap">{formatMoney(p.price)}</span>
                          ) : (
                            <span className={`badge ${p.stockQty < p.minOrderQty * 2 ? 'badge-red' : 'badge-gray'}`}>{formatNumber(p.stockQty)} left</span>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </section>
              )}
            </div>

            <section className="card">
              <div className="card-head">
                <h2>Recent orders</h2>
                {hasMenu('orders') && <Link to="/app/orders" className="link">All orders <ArrowRight size={14} /></Link>}
              </div>
              {data.recentOrders.length === 0 ? (
                <EmptyState icon={<ShoppingCart size={32} />} title="No orders yet" text={user.role === 'BUYER' ? 'Order from the marketplace or accept a quote to see it here.' : 'Orders will appear here when buyers order from you.'} />
              ) : (
                <OrdersTable orders={data.recentOrders} viewer={user.role} />
              )}
            </section>
          </>
        )}
      </AsyncBlock>
    </>
  )
}

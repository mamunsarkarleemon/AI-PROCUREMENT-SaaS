import { Link, useNavigate } from 'react-router'
import { FileText } from 'lucide-react'
import { quoteApi } from '../../api/endpoints'
import { AsyncBlock, Badge, EmptyState, PageHeader } from '../../components/ui'
import { formatDate, formatMoney, formatNumber } from '../../lib/format'
import { useApi } from '../../lib/useApi'

/** Supplier: every quote I have sent and what happened to it. */
export function MyQuotesPage() {
  const { data, loading, error } = useApi(() => quoteApi.mine(), [])
  const navigate = useNavigate()
  return (
    <>
      <PageHeader title="My quotes" subtitle="Quotes you sent for buyers’ requests."
        actions={<Link className="btn btn-primary" to="/app/marketplace?tab=requests">Find buying requests</Link>} />
      <AsyncBlock loading={loading} error={error}>
        {data?.length === 0 ? (
          <EmptyState icon={<FileText size={32} />} title="No quotes sent yet" text="Open the Buying requests tab in the marketplace and send your first quote." />
        ) : (
          <section className="card">
            <div className="table-wrap">
              <table className="table table-stack">
                <thead>
                  <tr><th>Request</th><th>Buyer</th><th className="num">Price</th><th className="num">Qty</th><th className="num">Total</th><th>Delivery</th><th>Status</th><th>Sent</th></tr>
                </thead>
                <tbody>
                  {data?.map((q) => (
                    <tr key={q.id} className="clickable" onClick={() => navigate(`/app/requests/${q.requestId}`)}>
                      <td className="strong stack-full">{q.requestTitle}</td>
                      <td data-label="Buyer">{q.buyer.companyName}</td>
                      <td className="num" data-label="Price">{formatMoney(q.pricePerUnit)}</td>
                      <td className="num" data-label="Qty">{formatNumber(q.quantity)} {q.requestUnit}</td>
                      <td className="num strong" data-label="Total">{formatMoney(q.total)}</td>
                      <td data-label="Delivery">{q.deliveryDays != null ? `${q.deliveryDays} days` : '—'}</td>
                      <td data-label="Status"><Badge value={q.status} /></td>
                      <td className="nowrap" data-label="Sent">{formatDate(q.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        )}
      </AsyncBlock>
    </>
  )
}

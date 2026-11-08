import { Mail, Trash2 } from 'lucide-react'
import { adminApi } from '../../api/endpoints'
import { Pagination } from '../../components/Pagination'
import { AsyncBlock, EmptyState, PageHeader } from '../../components/ui'
import { formatDateTime } from '../../lib/format'
import { useApi } from '../../lib/useApi'
import { usePagination } from '../../lib/usePagination'

/** Messages sent from the landing page contact form. */
export function MessagesPage() {
  const { data, loading, error, reload } = useApi(() => adminApi.messages(), [])
  const paging = usePagination(data ?? [], 8)

  async function remove(id: number) {
    if (!window.confirm('Delete this message?')) return
    await adminApi.deleteMessage(id).catch(() => {})
    reload()
  }

  return (
    <>
      <PageHeader title="Contact messages" subtitle="Sent by visitors from the “Contact” section of the website." />
      <AsyncBlock loading={loading} error={error}>
        {data?.length === 0 ? (
          <EmptyState icon={<Mail size={32} />} title="No messages yet" />
        ) : (
          <div className="message-list">
            {paging.pageItems.map((m) => (
              <article className="card" key={m.id}>
                <div className="card-head">
                  <div>
                    <h2>{m.subject || '(no subject)'}</h2>
                    <p className="small muted">{m.name} · <a href={`mailto:${m.email}`}>{m.email}</a> · {formatDateTime(m.createdAt)}</p>
                  </div>
                  <div className="row-actions">
                    <a className="btn btn-ghost btn-sm" href={`mailto:${m.email}?subject=${encodeURIComponent('Re: ' + (m.subject ?? ''))}`}>Reply</a>
                    <button className="icon-btn danger" aria-label="Delete message" onClick={() => remove(m.id)}><Trash2 size={16} /></button>
                  </div>
                </div>
                <p className="pre-line">{m.message}</p>
              </article>
            ))}
            <Pagination {...paging.props} />
          </div>
        )}
      </AsyncBlock>
    </>
  )
}

import { useState, type FormEvent } from 'react'
import { Check, Pencil, Plus, Trash2, X } from 'lucide-react'
import { errorMessage } from '../../api/client'
import { adminApi, publicApi } from '../../api/endpoints'
import type { Category } from '../../api/types'
import { Alert, AsyncBlock, PageHeader } from '../../components/ui'
import { useApi } from '../../lib/useApi'
import { usePagination } from '../../lib/usePagination'
import { Pagination } from '../../components/Pagination'

export function CategoriesPage() {
  const { data, loading, error, reload } = useApi(() => publicApi.categories(), [])
  const paging = usePagination(data ?? [], 15)
  const [name, setName] = useState('')
  const [editId, setEditId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [msg, setMsg] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)

  async function act(fn: () => Promise<unknown>, ok: string) {
    try {
      await fn()
      setMsg({ tone: 'success', text: ok })
      reload()
    } catch (err) {
      setMsg({ tone: 'error', text: errorMessage(err) })
    }
  }

  function add(e: FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    act(() => adminApi.createCategory(name.trim()), `Category “${name.trim()}” added.`).then(() => setName(''))
  }

  function remove(c: Category) {
    if (!window.confirm(`Delete “${c.name}”? Products in it will have no category.`)) return
    act(() => adminApi.deleteCategory(c.id), `Category “${c.name}” deleted.`)
  }

  return (
    <>
      <PageHeader title="Categories" subtitle="Product categories used in the marketplace, products and buying requests." />
      {msg && <Alert tone={msg.tone}>{msg.text}</Alert>}
      <section className="card narrow-card">
        <form className="inline-form" onSubmit={add}>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="New category name" aria-label="New category name" />
          <button className="btn btn-primary"><Plus size={16} /> Add</button>
        </form>
        <AsyncBlock loading={loading} error={error}>
          <ul className="simple-list">
            {paging.pageItems.map((c) => (
              <li key={c.id}>
                {editId === c.id ? (
                  <form className="inline-form grow" onSubmit={(e) => { e.preventDefault(); act(() => adminApi.renameCategory(c.id, editName), 'Category renamed.').then(() => setEditId(null)) }}>
                    <input value={editName} onChange={(e) => setEditName(e.target.value)} aria-label="Category name" autoFocus />
                    <button className="icon-btn" aria-label="Save"><Check size={16} /></button>
                    <button type="button" className="icon-btn" aria-label="Cancel" onClick={() => setEditId(null)}><X size={16} /></button>
                  </form>
                ) : (
                  <>
                    <span className="grow">{c.name}</span>
                    <button className="icon-btn" aria-label={`Rename ${c.name}`} onClick={() => { setEditId(c.id); setEditName(c.name) }}><Pencil size={16} /></button>
                    <button className="icon-btn danger" aria-label={`Delete ${c.name}`} onClick={() => remove(c)}><Trash2 size={16} /></button>
                  </>
                )}
              </li>
            ))}
          </ul>
          <Pagination {...paging.props} scrollToTop={false} />
        </AsyncBlock>
      </section>
    </>
  )
}

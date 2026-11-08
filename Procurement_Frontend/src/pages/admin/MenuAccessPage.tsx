import { useState } from 'react'
import { Lock } from 'lucide-react'
import { errorMessage } from '../../api/client'
import { adminApi } from '../../api/endpoints'
import type { MenuAccessRow } from '../../api/types'
import { Alert, AsyncBlock, PageHeader } from '../../components/ui'
import { Icon } from '../../lib/icons'
import { useApi } from '../../lib/useApi'

/** Admin decides which sidebar menus buyers and suppliers get. Changes apply immediately. */
export function MenuAccessPage() {
  const { data, setData, loading, error } = useApi(() => adminApi.menuAccess(), [])
  const [msg, setMsg] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)
  const [saving, setSaving] = useState<string | null>(null)

  async function toggle(row: MenuAccessRow, role: 'BUYER' | 'SUPPLIER') {
    const enabled = role === 'BUYER' ? !row.buyerEnabled : !row.supplierEnabled
    setSaving(`${row.key}-${role}`)
    try {
      setData(await adminApi.setMenuAccess(row.key, role, enabled))
      setMsg({ tone: 'success', text: `“${row.label}” is now ${enabled ? 'visible' : 'hidden'} for ${role === 'BUYER' ? 'buyers' : 'suppliers'}.` })
    } catch (err) {
      setMsg({ tone: 'error', text: errorMessage(err) })
    } finally {
      setSaving(null)
    }
  }

  return (
    <>
      <PageHeader title="Menu access" subtitle="Choose which menus buyers and suppliers can see and use. The server also blocks hidden pages." />
      {msg && <Alert tone={msg.tone}>{msg.text}</Alert>}
      <AsyncBlock loading={loading} error={error}>
        <section className="card">
          <div className="table-wrap">
            <table className="table access-table">
              <thead>
                <tr><th>Menu</th><th className="center">Buyer</th><th className="center">Supplier</th></tr>
              </thead>
              <tbody>
                {data?.map((row) => (
                  <tr key={row.key}>
                    <td>
                      <span className="menu-cell"><Icon name={row.icon} size={18} /> <b>{row.label}</b>
                        {row.locked && <span className="badge badge-gray"><Lock size={11} aria-hidden="true" /> always on</span>}
                      </span>
                    </td>
                    {(['BUYER', 'SUPPLIER'] as const).map((role) => {
                      const applies = role === 'BUYER' ? row.forBuyer : row.forSupplier
                      const on = role === 'BUYER' ? row.buyerEnabled : row.supplierEnabled
                      return (
                        <td key={role} className="center">
                          {applies ? (
                            <label className="switch" title={row.locked ? 'Always on' : undefined}>
                              <input type="checkbox" checked={on} disabled={row.locked || saving === `${row.key}-${role}`}
                                onChange={() => toggle(row, role)} aria-label={`${row.label} for ${role.toLowerCase()}s`} />
                              <span className="switch-track" aria-hidden="true" />
                              <span className="switch-text">{on ? 'On' : 'Off'}</span>
                            </label>
                          ) : (
                            <span className="muted small">n/a</span>
                          )}
                        </td>
                      )
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="small muted">Admins always see every admin menu. “n/a” means the menu does not apply to that account type.</p>
        </section>
      </AsyncBlock>
    </>
  )
}

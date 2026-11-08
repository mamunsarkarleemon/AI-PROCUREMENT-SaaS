import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { DayPoint } from '../api/types'
import { formatCompact, formatMoney, formatNumber } from '../lib/format'
import { Icon } from '../lib/icons'
import type { StatCardData } from '../api/types'

/**
 * One series (amount per day), one brand colour, thin 2px line, light grid,
 * hover tooltip with the exact value. No legend needed: the card title names the series.
 */
export function AmountChart({ data, height = 260 }: { data: DayPoint[]; height?: number }) {
  const rows = data.map((d) => ({ ...d, label: new Date(d.date).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }) }))
  const total = data.reduce((s, d) => s + d.amount, 0)
  return (
    <div className="chart-wrap">
      <ResponsiveContainer width="100%" height={height}>
        <AreaChart data={rows} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <defs>
            <linearGradient id="amountFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--chart-1)" stopOpacity={0.22} />
              <stop offset="100%" stopColor="var(--chart-1)" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke="var(--grid)" vertical={false} />
          <XAxis dataKey="label" tick={{ fill: 'var(--text-muted)', fontSize: 12 }} tickLine={false} axisLine={{ stroke: 'var(--border)' }} interval="preserveStartEnd" minTickGap={24} />
          <YAxis tickFormatter={formatCompact} tick={{ fill: 'var(--text-muted)', fontSize: 12 }} tickLine={false} axisLine={false} width={48} />
          <Tooltip
            cursor={{ stroke: 'var(--text-muted)', strokeDasharray: '3 3' }}
            contentStyle={{ borderRadius: 8, border: '1px solid var(--border)', boxShadow: 'var(--shadow)', fontSize: 13 }}
            formatter={(value, name) => (name === 'amount' ? [formatMoney(Number(value)), 'Amount'] : [String(value), name])}
            labelFormatter={(label, payload) => {
              const p = payload?.[0]?.payload as (DayPoint & { label: string }) | undefined
              return p ? `${label} · ${p.orders} order${p.orders === 1 ? '' : 's'}` : String(label)
            }}
          />
          <Area type="monotone" dataKey="amount" stroke="var(--chart-1)" strokeWidth={2} fill="url(#amountFill)" activeDot={{ r: 5, strokeWidth: 2, stroke: 'var(--surface)' }} />
        </AreaChart>
      </ResponsiveContainer>
      {total === 0 && <p className="chart-empty">No orders in this period yet.</p>}
    </div>
  )
}

/** Horizontal bars for "top suppliers / buyers / products". Plain HTML, value printed beside each bar. */
export function BarList({ rows }: { rows: { label: string; sub?: string; value: number }[] }) {
  const max = Math.max(...rows.map((r) => r.value), 1)
  if (rows.length === 0) return <p className="muted">No data for this period.</p>
  return (
    <ul className="barlist">
      {rows.map((r) => (
        <li key={r.label + (r.sub ?? '')} title={`${r.label}: ${formatMoney(r.value)}`}>
          <div className="barlist-head">
            <span className="barlist-label">
              {r.label}
              {r.sub && <small>{r.sub}</small>}
            </span>
            <span className="barlist-value">{formatMoney(r.value)}</span>
          </div>
          <div className="barlist-track">
            <div className="barlist-bar" style={{ width: `${Math.max((r.value / max) * 100, 2)}%` }} />
          </div>
        </li>
      ))}
    </ul>
  )
}

export function StatCard({ card }: { card: StatCardData }) {
  return (
    <div className={`stat-card tone-${card.tone}`}>
      <span className="stat-icon"><Icon name={card.icon} size={20} /></span>
      <span className="stat-label">{card.label}</span>
      <span className="stat-value">{card.format === 'money' ? formatMoney(card.value) : formatNumber(card.value)}</span>
      <span className="stat-hint">{card.hint}</span>
    </div>
  )
}

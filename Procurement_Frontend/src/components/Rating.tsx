import { useState } from 'react'
import { Star } from 'lucide-react'
import { formatNumber } from '../lib/format'

const FIVE = [1, 2, 3, 4, 5]

/** Read-only stars; 4.3 fills four and a third stars. */
export function Stars({ value, size = 15 }: { value: number; size?: number }) {
  const percent = Math.min(Math.max(value / 5, 0), 1) * 100
  return (
    <span className="stars" role="img" aria-label={`${value.toFixed(1)} out of 5 stars`}>
      <span className="stars-empty" aria-hidden="true">{FIVE.map((i) => <Star key={i} size={size} />)}</span>
      <span className="stars-fill" style={{ width: `${percent}%` }} aria-hidden="true">{FIVE.map((i) => <Star key={i} size={size} fill="currentColor" />)}</span>
    </span>
  )
}

/** Stars + "4.3 (12)" for product cards, or "No reviews yet". */
export function RatingLine({ value, count }: { value: number; count: number }) {
  if (count === 0) return <p className="rating-line muted">No reviews yet</p>
  return (
    <p className="rating-line">
      <Stars value={value} />
      <b>{value.toFixed(1)}</b>
      <span className="muted">({formatNumber(count)})</span>
    </p>
  )
}

const WORDS = ['', 'Poor', 'Fair', 'Good', 'Very good', 'Excellent']

/** Tap a star to choose 1 to 5. */
export function StarInput({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  const [hover, setHover] = useState(0)
  const shown = hover || value
  return (
    <div className="star-input" role="radiogroup" aria-label="Your rating" onMouseLeave={() => setHover(0)}>
      {FIVE.map((i) => (
        <button key={i} type="button" role="radio" aria-checked={value === i} aria-label={`${i} star${i === 1 ? '' : 's'}: ${WORDS[i]}`}
          className={i <= shown ? 'on' : ''} onMouseEnter={() => setHover(i)} onClick={() => onChange(i)}>
          <Star size={26} fill={i <= shown ? 'currentColor' : 'none'} aria-hidden="true" />
        </button>
      ))}
      <span className="star-word">{WORDS[shown]}</span>
    </div>
  )
}

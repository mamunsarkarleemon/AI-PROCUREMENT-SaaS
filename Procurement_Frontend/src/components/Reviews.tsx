import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Heart, Lightbulb, MessageSquare, ThumbsDown, ThumbsUp, Trash2, type LucideIcon } from 'lucide-react'
import { errorMessage } from '../api/client'
import { reviewApi } from '../api/endpoints'
import type { ReactionType, Review, ReviewList } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { formatNumber, timeAgo } from '../lib/format'
import { useApi } from '../lib/useApi'
import { usePagination } from '../lib/usePagination'
import { Pagination } from './Pagination'
import { StarInput, Stars } from './Rating'
import { Alert, AsyncBlock, Avatar, TextArea } from './ui'

const REACTIONS: { type: ReactionType; label: string; icon: LucideIcon }[] = [
  { type: 'LIKE', label: 'Like', icon: ThumbsUp },
  { type: 'LOVE', label: 'Love', icon: Heart },
  { type: 'HELPFUL', label: 'Helpful', icon: Lightbulb },
  { type: 'DISLIKE', label: 'Dislike', icon: ThumbsDown },
]

/** Product page: rating summary, the buyer's own review form, and everyone's reviews with reactions. */
export function ReviewsSection({ productId, onSummary }: { productId: number; onSummary: (average: number, count: number) => void }) {
  const { user } = useAuth()
  const { data, setData, loading, error } = useApi(() => reviewApi.list(productId), [productId])
  const paging = usePagination(data?.items ?? [], 5, productId)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const ref = useRef<HTMLElement>(null)
  const loaded = data !== null

  // Links such as /app/products/5#reviews jump here once the reviews have loaded.
  useEffect(() => {
    if (loaded && window.location.hash === '#reviews') ref.current?.scrollIntoView({ behavior: 'smooth' })
  }, [loaded])

  async function run(action: () => Promise<ReviewList>) {
    setBusy(true)
    setActionError(null)
    try {
      const list = await action()
      setData(list)
      onSummary(list.average, list.count)
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="card" id="reviews" ref={ref}>
      <h2 className="card-title">Ratings and reviews</h2>
      <AsyncBlock loading={loading} error={error}>
        {data && (
          <>
            {data.count > 0 && <Summary list={data} />}
            {actionError && <Alert tone="error">{actionError}</Alert>}
            {data.canReview && <ReviewForm key={data.myReview?.updatedAt ?? 'new'} mine={data.myReview} busy={busy}
              onSave={(rating, comment) => run(() => reviewApi.save(productId, rating, comment))} />}
            {user?.role === 'BUYER' && !data.canReview && <p className="small muted">You can rate this product after you have received it.</p>}
            {data.count === 0
              ? <p className="muted reviews-empty"><MessageSquare size={18} aria-hidden="true" /> No reviews yet.</p>
              : (
                <>
                  <ul className="review-list">
                    {paging.pageItems.map((r) => (
                      <ReviewItem key={r.id} review={r} busy={busy} canDelete={r.mine || user?.role === 'ADMIN'}
                        onReact={(type) => run(() => reviewApi.react(r.id, r.myReaction === type ? null : type))}
                        onDelete={() => {
                          if (window.confirm('Delete this review?')) run(() => reviewApi.remove(r.id))
                        }} />
                    ))}
                  </ul>
                  <Pagination {...paging.props} scrollToTop={false} />
                </>
              )}
          </>
        )}
      </AsyncBlock>
    </section>
  )
}

function Summary({ list }: { list: ReviewList }) {
  return (
    <div className="review-summary">
      <div className="review-score">
        <b>{list.average.toFixed(1)}</b>
        <Stars value={list.average} size={18} />
        <span className="muted small">{formatNumber(list.count)} review{list.count === 1 ? '' : 's'}</span>
      </div>
      <ul className="review-bars">
        {[5, 4, 3, 2, 1].map((stars) => {
          const n = list.breakdown[stars] ?? 0
          return (
            <li key={stars}>
              <span>{stars} star</span>
              <span className="barlist-track"><span className="review-bar" style={{ width: `${(n / list.count) * 100}%` }} /></span>
              <span className="review-bar-count">{n}</span>
            </li>
          )
        })}
      </ul>
    </div>
  )
}

function ReviewForm({ mine, busy, onSave }: { mine: Review | null; busy: boolean; onSave: (rating: number, comment: string) => void }) {
  const [rating, setRating] = useState(mine?.rating ?? 0)
  const [comment, setComment] = useState(mine?.comment ?? '')

  function submit(e: FormEvent) {
    e.preventDefault()
    if (rating) onSave(rating, comment)
  }

  return (
    <form className="form review-form" onSubmit={submit}>
      <h3>{mine ? 'Your review' : 'Rate this product'}</h3>
      <StarInput value={rating} onChange={setRating} />
      <TextArea label="Your comment (optional)" value={comment} onChange={(e) => setComment(e.target.value)} rows={3} maxLength={2000} placeholder="How was the quality, packing and delivery?" />
      <div><button className="btn btn-primary" disabled={busy || !rating}>{mine ? 'Update review' : 'Submit review'}</button></div>
    </form>
  )
}

function ReviewItem({ review: r, busy, canDelete, onReact, onDelete }: {
  review: Review
  busy: boolean
  canDelete: boolean
  onReact: (type: ReactionType) => void
  onDelete: () => void
}) {
  return (
    <li className="review">
      <div className="review-head">
        <Avatar name={r.reviewerName} size={38} />
        <div className="grow">
          <b>{r.reviewerName}{r.mine && <span className="muted"> (you)</span>}</b>
          <small className="muted">Verified buyer · {timeAgo(r.updatedAt)}</small>
        </div>
        {canDelete && <button type="button" className="icon-btn danger" aria-label="Delete review" title="Delete review" disabled={busy} onClick={onDelete}><Trash2 size={16} /></button>}
      </div>
      <Stars value={r.rating} />
      {r.comment && <p className="pre-line review-comment">{r.comment}</p>}
      <div className="reactions">
        {REACTIONS.map(({ type, label, icon: I }) => {
          const count = r.reactions[type] ?? 0
          const active = r.myReaction === type
          return (
            <button key={type} type="button" className={`react-btn${active ? ' active' : ''}`} disabled={busy || r.mine} aria-pressed={active}
              aria-label={`${label}, ${count}`} title={r.mine ? 'You cannot react to your own review' : label} onClick={() => onReact(type)}>
              <I size={15} fill={active ? 'currentColor' : 'none'} aria-hidden="true" />
              <span className="react-label">{label}</span>
              {count > 0 && <span>{count}</span>}
            </button>
          )
        })}
      </div>
    </li>
  )
}

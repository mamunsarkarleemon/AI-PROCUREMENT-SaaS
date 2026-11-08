import { useEffect, useMemo, useState } from 'react'
import { ImagePlus, PlayCircle, Trash2, X } from 'lucide-react'
import type { Media } from '../api/types'
import { Thumb } from './ui'

/** Big image/video viewer with a strip of thumbnails. */
export function MediaGallery({ media, alt }: { media: Media[]; alt: string }) {
  const [active, setActive] = useState(0)
  const current = media[Math.min(active, media.length - 1)]
  if (media.length === 0) return <Thumb src={null} alt={alt} size="lg" />
  return (
    <div className="gallery">
      <div className="gallery-main">
        {current.type === 'VIDEO' ? (
          <video src={current.url} controls preload="metadata" />
        ) : (
          <img src={current.url} alt={alt} />
        )}
      </div>
      {media.length > 1 && (
        <div className="gallery-strip">
          {media.map((m, i) => (
            <button
              key={m.id}
              type="button"
              className={`gallery-thumb${i === active ? ' active' : ''}`}
              onClick={() => setActive(i)}
              aria-label={`Show ${m.type === 'VIDEO' ? 'video' : 'photo'} ${i + 1}`}
            >
              {m.type === 'VIDEO' ? (
                <span className="gallery-video-thumb"><PlayCircle size={22} aria-hidden="true" /></span>
              ) : (
                <img src={m.url} alt="" />
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

/** Small row of existing files with a delete button (used in edit forms). */
export function MediaList({ media, onDelete }: { media: Media[]; onDelete?: (m: Media) => void }) {
  if (media.length === 0) return null
  return (
    <div className="media-list">
      {media.map((m) => (
        <div className="media-item" key={m.id}>
          {m.type === 'VIDEO' ? (
            <span className="media-video"><PlayCircle size={22} aria-hidden="true" /> Video</span>
          ) : (
            <img src={m.url} alt={m.originalName ?? 'photo'} />
          )}
          {onDelete && (
            <button type="button" className="media-remove" onClick={() => onDelete(m)} aria-label="Delete file">
              <Trash2 size={14} />
            </button>
          )}
        </div>
      ))}
    </div>
  )
}

/** Choose photos / short videos before upload, with previews. */
export function MediaPicker({ files, onChange, max = 8 }: { files: File[]; onChange: (f: File[]) => void; max?: number }) {
  const previews = useMemo(() => files.map((f) => ({ file: f, url: URL.createObjectURL(f) })), [files])
  useEffect(() => () => previews.forEach((p) => URL.revokeObjectURL(p.url)), [previews])

  return (
    <div className="media-picker">
      <label className="media-drop">
        <ImagePlus size={22} aria-hidden="true" />
        <span>
          <b>Add photos or a short video</b>
          <small>JPG, PNG, WEBP up to 5 MB · MP4, WEBM, MOV up to 50 MB</small>
        </span>
        <input
          type="file"
          multiple
          accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm,video/quicktime"
          onChange={(e) => {
            const picked = Array.from(e.target.files ?? [])
            onChange([...files, ...picked].slice(0, max))
            e.target.value = ''
          }}
        />
      </label>
      {previews.length > 0 && (
        <div className="media-list">
          {previews.map((p, i) => (
            <div className="media-item" key={p.url}>
              {p.file.type.startsWith('video/') ? (
                <span className="media-video"><PlayCircle size={22} aria-hidden="true" /> Video</span>
              ) : (
                <img src={p.url} alt={p.file.name} />
              )}
              <button type="button" className="media-remove" onClick={() => onChange(files.filter((_, j) => j !== i))} aria-label={`Remove ${p.file.name}`}>
                <X size={14} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

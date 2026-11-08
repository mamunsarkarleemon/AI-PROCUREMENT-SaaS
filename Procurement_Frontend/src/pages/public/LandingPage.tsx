import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import {
  ArrowRight,
  BarChart3,
  BadgeCheck,
  Bell,
  CheckCircle2,
  ClipboardList,
  Clock,
  Factory,
  Handshake,
  Mail,
  MapPin,
  Phone,
  ShieldCheck,
  ShoppingCart,
  Store,
  Truck,
  Users,
} from 'lucide-react'
import { errorMessage } from '../../api/client'
import { publicApi } from '../../api/endpoints'
import type { Category, Product, PublicStats } from '../../api/types'
import { Alert, TextArea, TextField, Thumb } from '../../components/ui'
import { formatMoney, formatNumber } from '../../lib/format'

const FEATURES = [
  { icon: Store, title: 'Supplier marketplace', text: 'Browse products from verified local and international suppliers with photos, short videos, price, stock and MOQ.' },
  { icon: ClipboardList, title: 'Post what you need', text: 'Describe the product once. Every supplier on the platform sees it and sends you a quote with photos and delivery time.' },
  { icon: Handshake, title: 'Negotiate in one thread', text: 'Offer your price, get counter-offers and agree on a deal. Every step is saved, so nothing gets lost in chats.' },
  { icon: Truck, title: 'Track every order', text: 'From pending to confirmed, shipped and delivered. Both sides see the same status at the same time.' },
  { icon: BarChart3, title: 'Clear reports', text: 'See spending or sales by day, supplier, buyer and product for any date range.' },
  { icon: ShieldCheck, title: 'Role-based access', text: 'Buyers, suppliers and admins each see only their own tools. Admins control which menus every role can use.' },
]

const STEPS = {
  buyer: [
    { title: 'Create a buyer account', text: 'Register with your company details in under a minute.' },
    { title: 'Find or request products', text: 'Search the marketplace or post a buying request for suppliers to quote on.' },
    { title: 'Compare and negotiate', text: 'Compare quotes side by side, or bargain on price with a supplier.' },
    { title: 'Order and track', text: 'Confirm the deal, follow the order to delivery and see your spend in reports.' },
  ],
  supplier: [
    { title: 'Create a supplier account', text: 'Register your company and get your own supplier workspace.' },
    { title: 'List your products', text: 'Add photos, a short video, price, stock and minimum order quantity.' },
    { title: 'Answer buyers', text: 'Reply to buying requests with quotes and respond to price offers.' },
    { title: 'Fulfil and grow', text: 'Confirm orders, update shipping and watch your sales in reports.' },
  ],
}

const FAQ = [
  { q: 'Who can use ProcureAI?', a: 'Any business that buys or sells products. Choose “Buyer” if you purchase goods, or “Supplier” if you sell them. One email = one account.' },
  { q: 'Is it free to register?', a: 'Yes. Creating a buyer or supplier account is free during the beta.' },
  { q: 'How does negotiation work?', a: 'On any product, a buyer can offer a price and quantity. The supplier can accept, reject or send a counter-offer. When one side accepts, an order is created automatically at the agreed price.' },
  { q: 'What is a buying request?', a: 'If you can’t find what you need, post a request with the quantity and your budget. All suppliers are notified and can send you a quote with photos or a video.' },
  { q: 'Who confirms an order?', a: 'The supplier confirms it (stock is reserved), then marks it shipped. The buyer marks it delivered when the goods arrive.' },
  { q: 'Can I see my history?', a: 'Yes. The Reports page shows orders and amounts by date, supplier, buyer and product for any period you choose.' },
]

export function LandingPage() {
  const [stats, setStats] = useState<PublicStats | null>(null)
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [tab, setTab] = useState<'buyer' | 'supplier'>('buyer')

  useEffect(() => {
    publicApi.stats().then(setStats).catch(() => {})
    publicApi.latestProducts(8).then(setProducts).catch(() => {})
    publicApi.categories().then(setCategories).catch(() => {})
  }, [])

  return (
    <>
      {/* ---------- Hero ---------- */}
      <section className="hero">
        <div className="container hero-grid">
          <div className="hero-copy">
            <span className="eyebrow"><BadgeCheck size={16} aria-hidden="true" /> B2B procurement for Qatar SMEs</span>
            <h1>
              Source smarter.<br />
              <span className="accent">Buy with confidence.</span>
            </h1>
            <p className="lead">
              One place to find suppliers, compare quotes, negotiate prices and track every order, from the first request to delivery.
            </p>
            <div className="hero-cta">
              <Link className="btn btn-primary btn-lg" to="/register">
                Start buying <ArrowRight size={18} aria-hidden="true" />
              </Link>
              <Link className="btn btn-outline btn-lg" to="/register?role=SUPPLIER">
                Join as a supplier
              </Link>
            </div>
            <ul className="hero-points">
              <li><CheckCircle2 size={16} aria-hidden="true" /> Free to register</li>
              <li><CheckCircle2 size={16} aria-hidden="true" /> Photos & video for every product</li>
              <li><CheckCircle2 size={16} aria-hidden="true" /> Live order tracking</li>
            </ul>
          </div>

          {/* Illustration of the product itself: a quote comparison card */}
          <div className="hero-visual" aria-hidden="true">
            <div className="mock-card mock-main">
              <div className="mock-head">
                <span className="mock-dot" />
                <b>Quotes for: Kraft food boxes · 200 cartons</b>
              </div>
              {[
                { name: 'Gulf Pack Trading', price: '44.00', days: '14 days', best: true },
                { name: 'Shenzhen Office Mart', price: '46.50', days: '25 days' },
                { name: 'Al Noor Supplies', price: '49.00', days: '7 days' },
              ].map((q) => (
                <div key={q.name} className={`mock-row${q.best ? ' best' : ''}`}>
                  <span className="mock-avatar">{q.name[0]}</span>
                  <span className="mock-name">{q.name}<small>Delivery {q.days}</small></span>
                  <span className="mock-price">QAR {q.price}</span>
                  {q.best && <span className="mock-tag">Best price</span>}
                </div>
              ))}
              <div className="mock-actions">
                <span className="mock-btn">Accept quote</span>
                <span className="mock-btn ghost">Negotiate</span>
              </div>
            </div>
            <div className="mock-card mock-float mock-float-1">
              <Bell size={16} />
              <span><b>New counter-offer</b><small>QAR 82.00 per carton</small></span>
            </div>
            <div className="mock-card mock-float mock-float-2">
              <Truck size={16} />
              <span><b>Order ORD-000124</b><small>Shipped · arriving Thursday</small></span>
            </div>
          </div>
        </div>

        <div className="container">
          <div className="stats-strip">
            <Stat icon={Factory} value={stats?.suppliers} label="Suppliers" />
            <Stat icon={Users} value={stats?.buyers} label="Buyers" />
            <Stat icon={Store} value={stats?.products} label="Products listed" />
            <Stat icon={ClipboardList} value={stats?.openRequests} label="Open buying requests" />
            <Stat icon={ShoppingCart} value={stats?.orders} label="Orders placed" />
          </div>
        </div>
      </section>

      {/* ---------- Categories ---------- */}
      {categories.length > 0 && (
        <section className="section section-tight">
          <div className="container">
            <p className="section-kicker center">Popular categories</p>
            <div className="chips center">
              {categories.map((c) => (
                <span className="chip" key={c.id}>{c.name}</span>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ---------- Features ---------- */}
      <section className="section" id="features">
        <div className="container">
          <div className="section-head">
            <p className="section-kicker">Features</p>
            <h2>Everything procurement needs, nothing it doesn’t</h2>
            <p>Built for small and medium businesses that are tired of chasing suppliers over phone calls and spreadsheets.</p>
          </div>
          <div className="feature-grid">
            {FEATURES.map((f) => (
              <div className="feature" key={f.title}>
                <span className="feature-icon"><f.icon size={22} aria-hidden="true" /></span>
                <h3>{f.title}</h3>
                <p>{f.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ---------- How it works ---------- */}
      <section className="section section-alt" id="how-it-works">
        <div className="container">
          <div className="section-head">
            <p className="section-kicker">How it works</p>
            <h2>Four simple steps</h2>
          </div>
          <div className="tabs center" role="tablist" aria-label="Choose your role">
            <button role="tab" aria-selected={tab === 'buyer'} className={`tab${tab === 'buyer' ? ' active' : ''}`} onClick={() => setTab('buyer')}>For buyers</button>
            <button role="tab" aria-selected={tab === 'supplier'} className={`tab${tab === 'supplier' ? ' active' : ''}`} onClick={() => setTab('supplier')}>For suppliers</button>
          </div>
          <ol className="steps-grid">
            {STEPS[tab].map((s, i) => (
              <li className="step" key={s.title}>
                <span className="step-no">{i + 1}</span>
                <h3>{s.title}</h3>
                <p>{s.text}</p>
              </li>
            ))}
          </ol>
          <div className="center" style={{ marginTop: 28 }}>
            <Link className="btn btn-primary" to={tab === 'buyer' ? '/register' : '/register?role=SUPPLIER'}>
              {tab === 'buyer' ? 'Create a buyer account' : 'Create a supplier account'} <ArrowRight size={16} aria-hidden="true" />
            </Link>
          </div>
        </div>
      </section>

      {/* ---------- Marketplace preview ---------- */}
      <section className="section" id="marketplace">
        <div className="container">
          <div className="section-head row">
            <div>
              <p className="section-kicker">Marketplace</p>
              <h2>Fresh in the marketplace</h2>
              <p>The latest products listed by suppliers. Log in to see details, negotiate and order.</p>
            </div>
            <Link className="btn btn-outline" to="/login">Browse all products <ArrowRight size={16} aria-hidden="true" /></Link>
          </div>
          {products.length === 0 ? (
            <p className="muted center">Products will appear here as soon as suppliers list them.</p>
          ) : (
            <div className="product-grid">
              {products.map((p) => (
                <Link to="/login" className="product-card" key={p.id}>
                  <Thumb src={p.imageUrl} alt={p.name} />
                  <div className="product-card-body">
                    {p.category && <span className="product-cat">{p.category.name}</span>}
                    <h3>{p.name}</h3>
                    <p className="product-price">{formatMoney(p.price)} <small>/ {p.unit}</small></p>
                    <p className="product-meta">MOQ {p.minOrderQty} {p.unit} · {p.supplier.companyName}{p.supplier.city ? `, ${p.supplier.city}` : ''}</p>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* ---------- Two audiences ---------- */}
      <section className="section section-alt">
        <div className="container split">
          <div className="audience">
            <span className="audience-icon"><ShoppingCart size={22} aria-hidden="true" /></span>
            <h3>For buyers</h3>
            <ul>
              <li><CheckCircle2 size={16} aria-hidden="true" /> Compare several suppliers on one screen</li>
              <li><CheckCircle2 size={16} aria-hidden="true" /> Negotiate prices without endless phone calls</li>
              <li><CheckCircle2 size={16} aria-hidden="true" /> Know exactly where every order is</li>
              <li><CheckCircle2 size={16} aria-hidden="true" /> See what you spent, by supplier and by month</li>
            </ul>
            <Link className="btn btn-primary" to="/register">Register as a buyer</Link>
          </div>
          <div className="audience">
            <span className="audience-icon purple"><Factory size={22} aria-hidden="true" /></span>
            <h3>For suppliers</h3>
            <ul>
              <li><CheckCircle2 size={16} aria-hidden="true" /> Show your products with photos and video</li>
              <li><CheckCircle2 size={16} aria-hidden="true" /> Get buying requests from real businesses</li>
              <li><CheckCircle2 size={16} aria-hidden="true" /> Stock updates automatically when you confirm orders</li>
              <li><CheckCircle2 size={16} aria-hidden="true" /> Track sales by buyer, product and date</li>
            </ul>
            <Link className="btn btn-purple" to="/register?role=SUPPLIER">Register as a supplier</Link>
          </div>
        </div>
      </section>

      {/* ---------- FAQ ---------- */}
      <section className="section" id="faq">
        <div className="container narrow">
          <div className="section-head">
            <p className="section-kicker">FAQ</p>
            <h2>Common questions</h2>
          </div>
          <div className="faq">
            {FAQ.map((f) => (
              <details key={f.q}>
                <summary>{f.q}</summary>
                <p>{f.a}</p>
              </details>
            ))}
          </div>
        </div>
      </section>

      {/* ---------- Contact ---------- */}
      <section className="section section-alt" id="contact">
        <div className="container contact-grid">
          <div>
            <p className="section-kicker">Contact</p>
            <h2>Talk to our team</h2>
            <p className="muted">Questions about onboarding, pricing or a partnership? Send us a message and we’ll get back to you within one working day.</p>
            <ul className="contact-list">
              <li><MapPin size={18} aria-hidden="true" /> West Bay, Doha, Qatar</li>
              <li><Phone size={18} aria-hidden="true" /> +974 4400 0000</li>
              <li><Mail size={18} aria-hidden="true" /> hello@procureai.qa</li>
              <li><Clock size={18} aria-hidden="true" /> Sun–Thu, 8:00–17:00</li>
            </ul>
          </div>
          <ContactForm />
        </div>
      </section>

      {/* ---------- CTA ---------- */}
      <section className="cta-band">
        <div className="container cta-inner">
          <div>
            <h2>Ready to simplify your purchasing?</h2>
            <p>Create your free account and post your first buying request today.</p>
          </div>
          <div className="hero-cta">
            <Link className="btn btn-light btn-lg" to="/register">Create free account</Link>
            <Link className="btn btn-outline-light btn-lg" to="/login">Log in</Link>
          </div>
        </div>
      </section>
    </>
  )
}

function Stat({ icon: I, value, label }: { icon: typeof Store; value: number | undefined; label: string }) {
  return (
    <div className="stat">
      <I size={20} aria-hidden="true" />
      <b>{value === undefined ? '—' : formatNumber(value)}</b>
      <span>{label}</span>
    </div>
  )
}

function ContactForm() {
  const [form, setForm] = useState({ name: '', email: '', subject: '', message: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [state, setState] = useState<'idle' | 'sending' | 'sent'>('idle')
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    const errs: Record<string, string> = {}
    if (!form.name.trim()) errs.name = 'Enter your name.'
    if (!/^\S+@\S+\.\S+$/.test(form.email)) errs.email = 'Enter a valid email.'
    if (!form.message.trim()) errs.message = 'Write a message.'
    setErrors(errs)
    if (Object.keys(errs).length) return
    setState('sending')
    setError(null)
    try {
      await publicApi.contact(form)
      setState('sent')
      setForm({ name: '', email: '', subject: '', message: '' })
    } catch (err) {
      setError(errorMessage(err))
      setState('idle')
    }
  }

  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [k]: e.target.value }))
  return (
    <form className="card form" onSubmit={submit} noValidate>
      {state === 'sent' && <Alert tone="success">Thank you! Your message was sent.</Alert>}
      {error && <Alert tone="error">{error}</Alert>}
      <div className="form-row">
        <TextField label="Your name" value={form.name} onChange={set('name')} error={errors.name} required />
        <TextField label="Email" type="email" value={form.email} onChange={set('email')} error={errors.email} required />
      </div>
      <TextField label="Subject" value={form.subject} onChange={set('subject')} />
      <TextArea label="Message" value={form.message} onChange={set('message')} error={errors.message} required />
      <button className="btn btn-primary" disabled={state === 'sending'}>{state === 'sending' ? 'Sending…' : 'Send message'}</button>
    </form>
  )
}

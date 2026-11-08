// TypeScript copies of the backend responses. Money comes as a number (e.g. 85.5).

export type Role = 'ADMIN' | 'BUYER' | 'SUPPLIER'
export type UserStatus = 'ACTIVE' | 'INACTIVE'
export type ProductStatus = 'ACTIVE' | 'INACTIVE'
export type RequestStatus = 'OPEN' | 'CLOSED'
export type QuoteStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN'
export type NegotiationStatus = 'OPEN' | 'ACCEPTED' | 'REJECTED'
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
export type OrderSource = 'DIRECT' | 'NEGOTIATION' | 'QUOTE'
export type ReturnStatus = 'NONE' | 'PENDING' | 'CONFIRMED'
export type ReactionType = 'LIKE' | 'LOVE' | 'HELPFUL' | 'DISLIKE'

/** One page of a list. page starts at 0. */
export interface PageResult<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface Review {
  id: number
  rating: number
  comment: string | null
  reviewerName: string
  mine: boolean
  reactions: Record<ReactionType, number>
  myReaction: ReactionType | null
  createdAt: string
  updatedAt: string
}

export interface ReviewList {
  average: number
  count: number
  breakdown: Record<number, number>
  canReview: boolean
  myReview: Review | null
  items: Review[]
}

export interface User {
  id: number
  fullName: string
  email: string
  role: Role
  status: UserStatus
  phone: string | null
  companyName: string | null
  address: string | null
  city: string | null
  country: string | null
  about: string | null
  createdAt: string
}

export interface Party {
  id: number
  fullName: string
  companyName: string
  phone: string | null
  email: string
  city: string | null
  country: string | null
}

export interface Category {
  id: number
  name: string
}

export interface Media {
  id: number
  url: string
  type: 'IMAGE' | 'VIDEO'
  originalName: string | null
}

export interface MenuItem {
  key: string
  label: string
  path: string
  icon: string
}

export interface Product {
  id: number
  name: string
  description: string | null
  unit: string
  price: number
  stockQty: number
  minOrderQty: number
  ratingAvg: number
  ratingCount: number
  soldQty: number
  status: ProductStatus
  category: Category | null
  supplier: Party
  imageUrl: string | null
  media: Media[]
  createdAt: string
  updatedAt: string
}

export interface ProductHighlights {
  topRated: Product[]
  topSelling: Product[]
}

export interface ProductForm {
  name: string
  description: string
  categoryId: number | null
  unit: string
  price: number
  stockQty: number
  minOrderQty: number
  status: ProductStatus
}

export interface Quote {
  id: number
  requestId: number
  requestTitle: string
  requestUnit: string
  supplier: Party
  buyer: Party
  productId: number | null
  productName: string | null
  pricePerUnit: number
  quantity: number
  total: number
  deliveryDays: number | null
  message: string | null
  status: QuoteStatus
  media: Media[]
  createdAt: string
}

export interface BuyRequest {
  id: number
  title: string
  description: string | null
  quantity: number
  unit: string
  targetPrice: number | null
  neededBy: string | null
  deliveryCity: string | null
  status: RequestStatus
  category: Category | null
  buyer: Party
  imageUrl: string | null
  media: Media[]
  quoteCount: number
  quotes: Quote[]
  createdAt: string
}

export interface BuyRequestForm {
  title: string
  description: string
  categoryId: number | null
  quantity: number
  unit: string
  targetPrice: number | null
  neededBy: string | null
  deliveryCity: string
}

export interface QuoteForm {
  pricePerUnit: number
  quantity: number
  deliveryDays: number | null
  productId: number | null
  message: string
}

export interface NegotiationMessage {
  id: number
  senderId: number
  senderName: string
  senderRole: Role
  action: 'OFFER' | 'COUNTER' | 'ACCEPT' | 'REJECT'
  pricePerUnit: number | null
  quantity: number | null
  total: number | null
  message: string | null
  createdAt: string
}

export interface Negotiation {
  id: number
  productId: number
  productName: string
  productImage: string | null
  unit: string
  listPrice: number
  stockQty: number
  minOrderQty: number
  buyer: Party
  supplier: Party
  currentPrice: number
  currentQty: number
  currentTotal: number
  lastOfferBy: Role
  status: NegotiationStatus
  myTurn: boolean
  orderId: number | null
  messages: NegotiationMessage[]
  createdAt: string
  updatedAt: string
}

export interface Order {
  id: number
  orderNo: string
  source: OrderSource
  status: OrderStatus
  productId: number | null
  productName: string
  productImage: string | null
  unit: string
  quantity: number
  unitPrice: number
  /** What the buyer pays. After a partial delivery this covers only the received units. */
  totalAmount: number
  orderedAmount: number
  receivedQty: number | null
  returnedQty: number
  returnReason: string | null
  returnStatus: ReturnStatus
  returnConfirmedAt: string | null
  canConfirmReturn: boolean
  note: string | null
  buyer: Party
  supplier: Party
  requestId: number | null
  quoteId: number | null
  negotiationId: number | null
  nextStatuses: OrderStatus[]
  createdAt: string
  updatedAt: string
}

export interface NotificationItem {
  id: number
  message: string
  link: string | null
  read: boolean
  createdAt: string
}

export interface DayPoint {
  date: string
  orders: number
  amount: number
}

export interface StatCardData {
  label: string
  value: number
  format: 'money' | 'count'
  hint: string
  icon: string
  tone: 'blue' | 'purple' | 'green' | 'orange'
}

export interface Dashboard {
  cards: StatCardData[]
  chartTitle: string
  chart: DayPoint[]
  recentOrders: Order[]
  productsTitle: string | null
  products: Product[]
  recentUsers: User[]
}

export interface PartyRow {
  id: number
  name: string
  email: string
  orders: number
  amount: number
}

export interface Report {
  from: string
  to: string
  summary: {
    orderCount: number
    totalAmount: number
    deliveredAmount: number
    pendingCount: number
    deliveredCount: number
    cancelledCount: number
    averageOrder: number
  }
  daily: DayPoint[]
  byStatus: { status: OrderStatus; orders: number; amount: number }[]
  bySupplier: PartyRow[]
  byBuyer: PartyRow[]
  byProduct: { productName: string; unit: string; quantity: number; orders: number; amount: number }[]
  orders: Order[]
}

export interface MenuAccessRow {
  key: string
  label: string
  icon: string
  forBuyer: boolean
  forSupplier: boolean
  locked: boolean
  buyerEnabled: boolean
  supplierEnabled: boolean
}

export interface ContactMessage {
  id: number
  name: string
  email: string
  subject: string | null
  message: string
  createdAt: string
}

export interface PublicStats {
  suppliers: number
  buyers: number
  products: number
  openRequests: number
  orders: number
}

export interface UserForm {
  fullName: string
  email: string
  password: string
  role: Role
  status: UserStatus
  phone: string
  companyName: string
  address: string
  city: string
  country: string
  about: string
}

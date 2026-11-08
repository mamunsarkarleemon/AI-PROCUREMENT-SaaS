import { api } from './client'
import type {
  BuyRequest,
  BuyRequestForm,
  Category,
  ContactMessage,
  Dashboard,
  MenuAccessRow,
  MenuItem,
  Negotiation,
  NotificationItem,
  Order,
  OrderStatus,
  Product,
  ProductForm,
  ProductStatus,
  PublicStats,
  Quote,
  QuoteForm,
  Report,
  Role,
  User,
  UserForm,
  UserStatus,
} from './types'

/** Every backend call in one place. Paths are relative to /api/v1. */

const qs = (params: Record<string, string | number | null | undefined>) => {
  const s = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') s.set(k, String(v))
  })
  const out = s.toString()
  return out ? `?${out}` : ''
}

const files = (list: File[]) => {
  const fd = new FormData()
  list.forEach((f) => fd.append('files', f))
  return fd
}

export const publicApi = {
  stats: () => api<PublicStats>('GET', '/public/stats'),
  latestProducts: (limit = 8) => api<Product[]>('GET', `/public/products?limit=${limit}`),
  categories: () => api<Category[]>('GET', '/public/categories'),
  contact: (data: { name: string; email: string; subject: string; message: string }) =>
    api<void>('POST', '/public/contact', data),
}

export const authApi = {
  register: (data: {
    fullName: string
    email: string
    password: string
    role: Role
    phone: string
    companyName: string
    city: string
    country: string
  }) => api<User>('POST', '/auth/register', data),
  login: (email: string, password: string) =>
    api<{ token: string; user: User }>('POST', '/auth/login', { email, password }),
  me: () => api<User>('GET', '/me'),
  menus: () => api<MenuItem[]>('GET', '/me/menus'),
  updateProfile: (data: Partial<User>) => api<User>('PUT', '/me', data),
  changePassword: (currentPassword: string, newPassword: string) =>
    api<void>('PUT', '/me/password', { currentPassword, newPassword }),
}

export const dashboardApi = {
  get: () => api<Dashboard>('GET', '/dashboard'),
  report: (p: { from?: string; to?: string; supplierId?: number | null; buyerId?: number | null }) =>
    api<Report>('GET', `/reports${qs(p)}`),
}

export const productApi = {
  marketplace: (q = '', categoryId: number | null = null) => api<Product[]>('GET', `/products${qs({ q, categoryId })}`),
  get: (id: number) => api<Product>('GET', `/products/${id}`),
  mine: () => api<Product[]>('GET', '/products/mine'),
  all: () => api<Product[]>('GET', '/admin/products'),
  create: (data: ProductForm) => api<Product>('POST', '/products', data),
  update: (id: number, data: ProductForm) => api<Product>('PUT', `/products/${id}`, data),
  setStatus: (id: number, status: ProductStatus) => api<Product>('PUT', `/products/${id}/status`, { status }),
  remove: (id: number) => api<void>('DELETE', `/products/${id}`),
  upload: (id: number, list: File[]) => api<Product>('POST', `/products/${id}/media`, files(list)),
}

export const mediaApi = {
  remove: (id: number) => api<void>('DELETE', `/media/${id}`),
}

export const requestApi = {
  open: (q = '', categoryId: number | null = null) => api<BuyRequest[]>('GET', `/requests${qs({ q, categoryId })}`),
  mine: () => api<BuyRequest[]>('GET', '/requests/mine'),
  all: () => api<BuyRequest[]>('GET', '/admin/requests'),
  get: (id: number) => api<BuyRequest>('GET', `/requests/${id}`),
  create: (data: BuyRequestForm) => api<BuyRequest>('POST', '/requests', data),
  update: (id: number, data: BuyRequestForm) => api<BuyRequest>('PUT', `/requests/${id}`, data),
  close: (id: number) => api<BuyRequest>('POST', `/requests/${id}/close`),
  remove: (id: number) => api<void>('DELETE', `/requests/${id}`),
  upload: (id: number, list: File[]) => api<BuyRequest>('POST', `/requests/${id}/media`, files(list)),
}

export const quoteApi = {
  submit: (requestId: number, data: QuoteForm) => api<Quote>('POST', `/requests/${requestId}/quotes`, data),
  mine: () => api<Quote[]>('GET', '/quotes/mine'),
  upload: (id: number, list: File[]) => api<Quote>('POST', `/quotes/${id}/media`, files(list)),
  accept: (id: number) => api<Order>('POST', `/quotes/${id}/accept`),
  reject: (id: number) => api<Quote>('POST', `/quotes/${id}/reject`),
  withdraw: (id: number) => api<Quote>('POST', `/quotes/${id}/withdraw`),
}

export const negotiationApi = {
  list: () => api<Negotiation[]>('GET', '/negotiations'),
  get: (id: number) => api<Negotiation>('GET', `/negotiations/${id}`),
  start: (data: { productId: number; quantity: number; pricePerUnit: number; message: string }) =>
    api<Negotiation>('POST', '/negotiations', data),
  counter: (id: number, data: { quantity: number; pricePerUnit: number; message: string }) =>
    api<Negotiation>('POST', `/negotiations/${id}/counter`, data),
  accept: (id: number, message = '') => api<Negotiation>('POST', `/negotiations/${id}/accept`, { message }),
  reject: (id: number, message = '') => api<Negotiation>('POST', `/negotiations/${id}/reject`, { message }),
}

export const orderApi = {
  list: (status: OrderStatus | '' = '') => api<Order[]>('GET', `/orders${qs({ status })}`),
  get: (id: number) => api<Order>('GET', `/orders/${id}`),
  buyNow: (productId: number, quantity: number, note: string) =>
    api<Order>('POST', '/orders', { productId, quantity, note }),
  setStatus: (id: number, status: OrderStatus) => api<Order>('PUT', `/orders/${id}/status`, { status }),
}

export const notificationApi = {
  list: () => api<{ unread: number; items: NotificationItem[] }>('GET', '/notifications'),
  read: (id: number) => api<void>('POST', `/notifications/${id}/read`),
  readAll: () => api<void>('POST', '/notifications/read-all'),
}

export const adminApi = {
  users: (p: { role?: Role | ''; status?: UserStatus | ''; q?: string } = {}) =>
    api<User[]>('GET', `/admin/users${qs(p)}`),
  createUser: (data: UserForm) => api<User>('POST', '/admin/users', data),
  updateUser: (id: number, data: UserForm) => api<User>('PUT', `/admin/users/${id}`, data),
  setUserStatus: (id: number, status: UserStatus) => api<User>('PUT', `/admin/users/${id}/status`, { status }),
  deleteUser: (id: number) => api<void>('DELETE', `/admin/users/${id}`),
  menuAccess: () => api<MenuAccessRow[]>('GET', '/admin/menu-access'),
  setMenuAccess: (key: string, role: 'BUYER' | 'SUPPLIER', enabled: boolean) =>
    api<MenuAccessRow[]>('PUT', `/admin/menu-access/${key}/${role}`, { enabled }),
  createCategory: (name: string) => api<Category>('POST', '/admin/categories', { name }),
  renameCategory: (id: number, name: string) => api<Category>('PUT', `/admin/categories/${id}`, { name }),
  deleteCategory: (id: number) => api<void>('DELETE', `/admin/categories/${id}`),
  messages: () => api<ContactMessage[]>('GET', '/admin/messages'),
  deleteMessage: (id: number) => api<void>('DELETE', `/admin/messages/${id}`),
}

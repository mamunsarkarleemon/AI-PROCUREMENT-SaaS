import { apiRequest } from './client'
import type { AccountType, DashboardResponse, LoginRequest, LoginResponse, RegisterRequest, User } from './types'

export const authApi = {
  register: (data: RegisterRequest) =>
    apiRequest<User>('/api/v1/auth/register', { method: 'POST', body: data }),

  login: (data: LoginRequest) =>
    apiRequest<LoginResponse>('/api/v1/auth/login', { method: 'POST', body: data }),

  me: (token: string, signal?: AbortSignal) =>
    apiRequest<User>('/api/v1/users/me', { token, signal }),

  dashboard: (area: AccountType, token: string, signal?: AbortSignal) =>
    apiRequest<DashboardResponse>(`/api/v1/${area.toLowerCase()}/dashboard`, { token, signal }),
}

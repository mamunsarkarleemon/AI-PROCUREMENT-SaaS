// TypeScript mirrors of the backend DTOs. Keep in sync with com.saas.procurement.*.dto.

export type AccountType = 'BUYER' | 'SUPPLIER'
export type PlatformRole = 'USER' | 'PLATFORM_ADMIN'
export type UserStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'DISABLED'
export type CompanyStatus = 'ACTIVE' | 'PENDING_REVIEW' | 'SUSPENDED'
export type CompanyRole = 'BUYER_OWNER' | 'BUYER_MEMBER' | 'SUPPLIER_OWNER' | 'SUPPLIER_MEMBER'

export interface Membership {
  companyId: string
  companyName: string
  companyType: AccountType
  companyStatus: CompanyStatus
  countryCode: string
  role: CompanyRole
}

export interface User {
  id: string
  email: string
  fullName: string
  platformRole: PlatformRole
  status: UserStatus
  emailVerified: boolean
  createdAt: string
  memberships: Membership[]
}

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
  accountType: AccountType
  companyName: string
  countryCode: string
  registrationNo?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: User
}

export interface DashboardResponse {
  area: AccountType
  message: string
}

/** Error body returned by every backend error (blueprint 6.2). */
export interface ApiErrorBody {
  timestamp: string
  status: number
  code: string
  message: string
  fieldErrors: { field: string; reason: string }[]
  traceId: string | null
}

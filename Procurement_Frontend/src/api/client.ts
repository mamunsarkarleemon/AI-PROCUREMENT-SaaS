import type { ApiErrorBody } from './types'

/** Empty in development: requests go to /api/... and Vite proxies them to Spring Boot. */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/** Thrown for every failed request so pages can show one consistent error UI. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: Record<string, string>
  readonly traceId: string | null

  constructor(status: number, code: string, message: string, fieldErrors: Record<string, string> = {}, traceId: string | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
    this.traceId = traceId
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  token?: string | null
  signal?: AbortSignal
}

export async function apiRequest<T>(path: string, { method = 'GET', body, token, signal }: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') throw err
    throw new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server. Is the backend running?')
  }

  if (response.ok) {
    return (response.status === 204 ? undefined : await response.json()) as T
  }

  throw await toApiError(response)
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as ApiErrorBody
    // A field can fail several rules; show the first message per field.
    const fieldErrors: Record<string, string> = {}
    for (const fe of body.fieldErrors ?? []) {
      fieldErrors[fe.field] ??= fe.reason
    }
    return new ApiError(response.status, body.code, body.message, fieldErrors, body.traceId)
  } catch {
    return new ApiError(response.status, 'UNEXPECTED_RESPONSE', `Request failed with status ${response.status}.`)
  }
}

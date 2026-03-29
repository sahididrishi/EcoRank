export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'QUEUED'
  | 'FULFILLED'
  | 'REFUNDED'
  | 'FAILED'

export interface Product {
  id: number
  slug: string
  name: string
  description: string | null
  priceCents: number
  rankGroup: string | null
  category: string | null
  imageUrl: string | null
  active: boolean
  sortOrder: number
}

export interface Order {
  id: number
  playerName: string
  playerUuid: string
  productName: string
  productSlug: string
  amountCents: number
  status: OrderStatus
  createdAt: string
  fulfilledAt: string | null
}

export interface Player {
  id: number
  minecraftUuid: string
  username: string
  createdAt: string
}

export interface DashboardStats {
  totalRevenueCents: number
  totalOrders: number
  totalPlayers: number
  pendingOrders: number
}

export interface AuthResponse {
  accessToken: string
  expiresIn: number
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ErrorResponse {
  status: number
  error: string
  message: string
  timestamp: string
  fieldErrors: Record<string, string> | null
}

export interface CreateProductRequest {
  name: string
  slug: string
  priceCents: number
  description?: string
  rankGroup?: string
  category?: string
  imageUrl?: string
}

export interface UpdateProductRequest {
  name?: string
  slug?: string
  priceCents?: number
  description?: string
  rankGroup?: string
  category?: string
  imageUrl?: string
}

export interface CheckoutRequest {
  productSlug: string
  playerName: string
  idempotencyKey: string
}

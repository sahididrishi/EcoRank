import api from '@/lib/axios'
import type { Order, PaginatedResponse } from '@/types/api'

export async function getOrders(
  page: number = 0,
  size: number = 20,
  status?: string,
): Promise<PaginatedResponse<Order>> {
  const params: Record<string, string | number> = { page, size }
  if (status) params.status = status
  const { data } = await api.get<PaginatedResponse<Order>>('/admin/orders', { params })
  return data
}

export async function getOrder(id: number): Promise<Order> {
  const { data } = await api.get<Order>(`/admin/orders/${id}`)
  return data
}

export async function refundOrder(id: number): Promise<void> {
  await api.post(`/admin/orders/${id}/refund`)
}

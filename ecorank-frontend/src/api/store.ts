import api from '@/lib/axios'
import type { Product, CheckoutRequest } from '@/types/api'

export async function getStoreProducts(): Promise<Product[]> {
  const { data } = await api.get<Product[]>('/store/products')
  return data
}

export async function createCheckout(req: CheckoutRequest): Promise<{ checkoutUrl: string }> {
  const { data } = await api.post<{ checkoutUrl: string }>('/store/checkout', req)
  return data
}

import api from '@/lib/axios'
import type { Product, CreateProductRequest, UpdateProductRequest } from '@/types/api'

export async function getAdminProducts(): Promise<Product[]> {
  const { data } = await api.get<Product[]>('/admin/products')
  return data
}

export async function getProduct(id: number): Promise<Product> {
  const { data } = await api.get<Product>(`/admin/products/${id}`)
  return data
}

export async function createProduct(req: CreateProductRequest): Promise<Product> {
  const { data } = await api.post<Product>('/admin/products', req)
  return data
}

export async function updateProduct(id: number, req: UpdateProductRequest): Promise<Product> {
  const { data } = await api.put<Product>(`/admin/products/${id}`, req)
  return data
}

export async function toggleProductActive(id: number, active: boolean): Promise<void> {
  await api.patch(`/admin/products/${id}/active`, { active })
}

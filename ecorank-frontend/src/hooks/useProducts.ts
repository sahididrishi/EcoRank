import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as productsApi from '@/api/products'
import type { CreateProductRequest, UpdateProductRequest } from '@/types/api'

export function useProducts() {
  return useQuery({
    queryKey: ['admin-products'],
    queryFn: productsApi.getAdminProducts,
  })
}

export function useProduct(id: number) {
  return useQuery({
    queryKey: ['admin-products', id],
    queryFn: () => productsApi.getProduct(id),
    enabled: id > 0,
  })
}

export function useCreateProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (req: CreateProductRequest) => productsApi.createProduct(req),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-products'] }),
  })
}

export function useUpdateProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, req }: { id: number; req: UpdateProductRequest }) =>
      productsApi.updateProduct(id, req),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-products'] }),
  })
}

export function useToggleProductActive() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      productsApi.toggleProductActive(id, active),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-products'] }),
  })
}

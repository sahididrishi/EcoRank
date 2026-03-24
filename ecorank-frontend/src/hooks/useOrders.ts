import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as ordersApi from '@/api/orders'

export function useOrders(page: number = 0, size: number = 20, status?: string) {
  return useQuery({
    queryKey: ['admin-orders', page, size, status],
    queryFn: () => ordersApi.getOrders(page, size, status),
  })
}

export function useOrder(id: number) {
  return useQuery({
    queryKey: ['admin-orders', id],
    queryFn: () => ordersApi.getOrder(id),
    enabled: id > 0,
  })
}

export function useRefundOrder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => ordersApi.refundOrder(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-orders'] }),
  })
}

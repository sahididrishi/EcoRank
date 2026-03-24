import { useQuery } from '@tanstack/react-query'
import { getDashboardStats } from '@/api/stats'

export function useDashboardStats() {
  return useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: getDashboardStats,
    refetchInterval: 30000,
  })
}

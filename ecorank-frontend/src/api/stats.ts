import api from '@/lib/axios'
import type { DashboardStats } from '@/types/api'

export async function getDashboardStats(): Promise<DashboardStats> {
  const { data } = await api.get<DashboardStats>('/admin/stats')
  return data
}

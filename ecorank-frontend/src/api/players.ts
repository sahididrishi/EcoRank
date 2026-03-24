import api from '@/lib/axios'
import type { Player, Order } from '@/types/api'

export async function searchPlayers(query: string): Promise<Player[]> {
  const { data } = await api.get<Player[]>('/admin/players', { params: { q: query } })
  return data
}

export async function getPlayer(uuid: string): Promise<Player> {
  const { data } = await api.get<Player>(`/admin/players/${uuid}`)
  return data
}

export async function getPlayerOrders(uuid: string): Promise<Order[]> {
  const { data } = await api.get<Order[]>(`/admin/players/${uuid}/orders`)
  return data
}

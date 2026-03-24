import { useQuery } from '@tanstack/react-query'
import * as playersApi from '@/api/players'

export function usePlayerSearch(query: string) {
  return useQuery({
    queryKey: ['player-search', query],
    queryFn: () => playersApi.searchPlayers(query),
    enabled: query.length >= 2,
  })
}

export function usePlayer(uuid: string) {
  return useQuery({
    queryKey: ['player', uuid],
    queryFn: () => playersApi.getPlayer(uuid),
    enabled: !!uuid,
  })
}

export function usePlayerOrders(uuid: string) {
  return useQuery({
    queryKey: ['player-orders', uuid],
    queryFn: () => playersApi.getPlayerOrders(uuid),
    enabled: !!uuid,
  })
}

import api from '@/lib/axios'
import type { AuthResponse } from '@/types/api'

export async function login(username: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/admin/auth/login', { username, password })
  return data
}

export async function refresh(): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/admin/auth/refresh')
  return data
}

export async function logout(): Promise<void> {
  await api.post('/admin/auth/logout')
}

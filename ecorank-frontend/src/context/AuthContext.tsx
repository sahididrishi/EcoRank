import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import type { ReactNode } from 'react'
import { setAccessToken } from '@/lib/axios'
import * as authApi from '@/api/auth'

interface AuthContextType {
  isAuthenticated: boolean
  isLoading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    authApi
      .refresh()
      .then((res) => {
        setAccessToken(res.accessToken)
        setIsAuthenticated(true)
      })
      .catch(() => {
        setAccessToken(null)
        setIsAuthenticated(false)
      })
      .finally(() => setIsLoading(false))
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password)
    setAccessToken(res.accessToken)
    setIsAuthenticated(true)
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      setAccessToken(null)
      setIsAuthenticated(false)
    }
  }, [])

  return (
    <AuthContext.Provider value={{ isAuthenticated, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

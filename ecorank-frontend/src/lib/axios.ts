import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

let accessToken: string | null = null
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken(): string | null {
  return accessToken
}

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((p) => {
    if (error) {
      p.reject(error)
    } else {
      p.resolve(token!)
    }
  })
  failedQueue = []
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return api(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const { data } = await axios.post<{ accessToken: string }>('/api/v1/admin/auth/refresh')
        accessToken = data.accessToken
        processQueue(null, data.accessToken)
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        accessToken = null
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(error)
  },
)

export default api

import axios from 'axios'
import type { ApiResponse } from '@/types/api'
import { ADMIN_PROFILE_KEY, ADMIN_TOKEN_KEY } from '@/stores/auth'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(ADMIN_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(ADMIN_TOKEN_KEY)
      localStorage.removeItem(ADMIN_PROFILE_KEY)
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    const payload = error.response?.data as ApiResponse<unknown> | undefined
    const message = payload?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  },
)

export default http

import http from './request'
import type { ApiResponse } from '@/types/api'
import type { AdminProfile, AdminToken } from '@/types/admin'

export async function login(username: string, password: string): Promise<AdminToken> {
  const response = await http.post<ApiResponse<AdminToken>>('/admin/auth/login', {
    username,
    password,
  })
  return response.data.data
}

export async function getMe(): Promise<AdminProfile> {
  const response = await http.get<ApiResponse<AdminProfile>>('/admin/me')
  return response.data.data
}

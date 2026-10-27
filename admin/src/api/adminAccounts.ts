import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminProfile } from '@/types/admin'

export interface AdminAccountQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: string
}

export async function fetchAdmins(params: AdminAccountQuery): Promise<PageResult<AdminProfile>> {
  const response = await http.get<ApiResponse<PageResult<AdminProfile>>>('/admin/admins', { params })
  return response.data.data
}

export async function fetchAdminRoles(): Promise<string[]> {
  const response = await http.get<ApiResponse<string[]>>('/admin/admins/roles')
  return response.data.data
}

export async function createAdmin(payload: {
  username: string
  password: string
  displayName: string
  role: string
}): Promise<AdminProfile> {
  const response = await http.post<ApiResponse<AdminProfile>>('/admin/admins', payload)
  return response.data.data
}

export async function updateAdmin(
  id: number,
  payload: { displayName: string; role: string },
): Promise<AdminProfile> {
  const response = await http.put<ApiResponse<AdminProfile>>(`/admin/admins/${id}`, payload)
  return response.data.data
}

export async function updateAdminStatus(id: number, status: 'ACTIVE' | 'DISABLED'): Promise<void> {
  await http.patch<ApiResponse<void>>(`/admin/admins/${id}/status`, { status })
}

export async function resetAdminPassword(id: number, password: string): Promise<void> {
  await http.post<ApiResponse<void>>(`/admin/admins/${id}/reset-password`, { password })
}

import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminUser } from '@/types/admin'

export interface UserQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: string
}

export async function fetchUsers(params: UserQuery): Promise<PageResult<AdminUser>> {
  const response = await http.get<ApiResponse<PageResult<AdminUser>>>('/admin/users', { params })
  return response.data.data
}

export async function updateUserStatus(id: number, status: string): Promise<void> {
  await http.patch(`/admin/users/${id}/status`, { status })
}

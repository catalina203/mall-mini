import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminCategory } from '@/types/admin'

export interface CategoryQuery {
  page: number
  pageSize: number
  status?: string
  keyword?: string
}

export interface CategoryPayload {
  name: string
  parentId: number
  iconUrl: string
  sortOrder: number
  status: string
}

export async function fetchCategories(params: CategoryQuery): Promise<PageResult<AdminCategory>> {
  const response = await http.get<ApiResponse<PageResult<AdminCategory>>>('/admin/categories', {
    params,
  })
  return response.data.data
}

export async function fetchCategoryOptions(): Promise<AdminCategory[]> {
  const response = await http.get<ApiResponse<AdminCategory[]>>('/admin/categories/options')
  return response.data.data
}

export async function createCategory(payload: CategoryPayload): Promise<AdminCategory> {
  const response = await http.post<ApiResponse<AdminCategory>>('/admin/categories', payload)
  return response.data.data
}

export async function updateCategory(id: number, payload: CategoryPayload): Promise<AdminCategory> {
  const response = await http.put<ApiResponse<AdminCategory>>(`/admin/categories/${id}`, payload)
  return response.data.data
}

export async function updateCategoryStatus(id: number, status: string): Promise<void> {
  await http.patch(`/admin/categories/${id}/status`, { status })
}

export async function deleteCategory(id: number): Promise<void> {
  await http.delete(`/admin/categories/${id}`)
}

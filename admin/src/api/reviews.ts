import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminReview } from '@/types/admin'

export async function fetchReviews(params: {
  page: number
  pageSize: number
  status?: string
  productId?: number
}): Promise<PageResult<AdminReview>> {
  const response = await http.get<ApiResponse<PageResult<AdminReview>>>('/admin/reviews', { params })
  return response.data.data
}

export async function approveReview(id: number): Promise<AdminReview> {
  const response = await http.post<ApiResponse<AdminReview>>(`/admin/reviews/${id}/approve`)
  return response.data.data
}

export async function rejectReview(id: number): Promise<AdminReview> {
  const response = await http.post<ApiResponse<AdminReview>>(`/admin/reviews/${id}/reject`)
  return response.data.data
}

export async function replyReview(id: number, reply: string): Promise<AdminReview> {
  const response = await http.post<ApiResponse<AdminReview>>(`/admin/reviews/${id}/reply`, { reply })
  return response.data.data
}

import http from './request'
import type { ApiResponse } from '@/types/api'
import type { AdminBanner } from '@/types/admin'

export interface BannerPayload {
  title: string
  imageUrl: string
  linkType: string
  linkTarget: string
  sortOrder: number
  status: string
}

export async function fetchBanners(status?: string): Promise<AdminBanner[]> {
  const response = await http.get<ApiResponse<AdminBanner[]>>('/admin/banners', {
    params: status ? { status } : undefined,
  })
  return response.data.data
}

export async function createBanner(payload: BannerPayload): Promise<AdminBanner> {
  const response = await http.post<ApiResponse<AdminBanner>>('/admin/banners', payload)
  return response.data.data
}

export async function updateBanner(id: number, payload: BannerPayload): Promise<AdminBanner> {
  const response = await http.put<ApiResponse<AdminBanner>>(`/admin/banners/${id}`, payload)
  return response.data.data
}

export async function updateBannerStatus(id: number, status: string): Promise<void> {
  await http.patch(`/admin/banners/${id}/status`, { status })
}

export async function deleteBanner(id: number): Promise<void> {
  await http.delete(`/admin/banners/${id}`)
}

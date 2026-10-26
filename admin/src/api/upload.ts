import http from './request'
import type { ApiResponse } from '@/types/api'

export async function uploadImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await http.post<ApiResponse<{ url: string }>>('/admin/uploads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data.data.url
}

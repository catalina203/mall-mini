import http from './request'
import type { ApiResponse } from '@/types/api'
import type { AdminShippingRule, AdminShippingTemplate } from '@/types/admin'

export interface ShippingPayload {
  name: string
  defaultFee: number
  freeThreshold: number
  status: 'ENABLED' | 'DISABLED'
  rules: AdminShippingRule[]
}

export async function fetchShippingTemplates(): Promise<AdminShippingTemplate[]> {
  const response = await http.get<ApiResponse<AdminShippingTemplate[]>>('/admin/shipping-templates')
  return response.data.data
}

export async function fetchShippingOptions(): Promise<Array<{ id: number; name: string }>> {
  const response = await http.get<ApiResponse<Array<{ id: number; name: string }>>>(
    '/admin/shipping-templates/options',
  )
  return response.data.data
}

export async function createShippingTemplate(payload: ShippingPayload): Promise<AdminShippingTemplate> {
  const response = await http.post<ApiResponse<AdminShippingTemplate>>(
    '/admin/shipping-templates',
    payload,
  )
  return response.data.data
}

export async function updateShippingTemplate(
  id: number,
  payload: ShippingPayload,
): Promise<AdminShippingTemplate> {
  const response = await http.put<ApiResponse<AdminShippingTemplate>>(
    `/admin/shipping-templates/${id}`,
    payload,
  )
  return response.data.data
}

export async function updateShippingStatus(
  id: number,
  status: 'ENABLED' | 'DISABLED',
): Promise<void> {
  await http.patch(`/admin/shipping-templates/${id}/status`, { status })
}

export async function deleteShippingTemplate(id: number): Promise<void> {
  await http.delete(`/admin/shipping-templates/${id}`)
}

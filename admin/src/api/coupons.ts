import http from './request'
import type { ApiResponse } from '@/types/api'
import type { AdminCouponTemplate } from '@/types/admin'

export interface CouponPayload {
  name: string
  type: 'FIXED' | 'PERCENT'
  value: number
  minAmount: number
  scope: 'ALL' | 'CATEGORY' | 'PRODUCT'
  scopeId?: number
  totalCount: number
  perUserLimit: number
  validFrom: string
  validTo: string
  status: 'ENABLED' | 'DISABLED'
}

export async function fetchCoupons(): Promise<AdminCouponTemplate[]> {
  const response = await http.get<ApiResponse<AdminCouponTemplate[]>>('/admin/coupons')
  return response.data.data
}

export async function createCoupon(payload: CouponPayload): Promise<AdminCouponTemplate> {
  const response = await http.post<ApiResponse<AdminCouponTemplate>>('/admin/coupons', payload)
  return response.data.data
}

export async function updateCoupon(
  id: number,
  payload: CouponPayload,
): Promise<AdminCouponTemplate> {
  const response = await http.put<ApiResponse<AdminCouponTemplate>>(`/admin/coupons/${id}`, payload)
  return response.data.data
}

export async function updateCouponStatus(id: number, status: 'ENABLED' | 'DISABLED'): Promise<void> {
  await http.patch(`/admin/coupons/${id}/status`, { status })
}

export async function deleteCoupon(id: number): Promise<void> {
  await http.delete(`/admin/coupons/${id}`)
}

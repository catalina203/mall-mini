import { request } from './request'
import type { CouponTemplate, UserCoupon } from '../types/api'

export function listClaimableCoupons(): Promise<CouponTemplate[]> {
  return request<CouponTemplate[]>({ url: '/me/coupons/claimable' })
}

export function claimCoupon(templateId: number): Promise<UserCoupon> {
  return request<UserCoupon>({
    url: `/me/coupons/${templateId}/claim`,
    method: 'POST',
  })
}

export function listMyCoupons(status?: string): Promise<UserCoupon[]> {
  const query = status ? `?status=${encodeURIComponent(status)}` : ''
  return request<UserCoupon[]>({ url: `/me/coupons${query}` })
}

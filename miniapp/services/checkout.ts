import { request } from './request'
import type { CheckoutPayload, CheckoutPreview } from '../types/api'

export function previewCheckout(payload: CheckoutPayload): Promise<CheckoutPreview> {
  return request<CheckoutPreview>({
    url: '/me/checkout/preview',
    method: 'POST',
    data: payload,
  })
}

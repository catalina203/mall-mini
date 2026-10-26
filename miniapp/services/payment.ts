import { request } from './request'
import type { PaymentConfig, PaymentResult } from '../types/api'

export function getPaymentConfig(): Promise<PaymentConfig> {
  return request<PaymentConfig>({ url: '/payment/config' })
}

export function payOrder(orderNo: string): Promise<PaymentResult> {
  return request<PaymentResult>({
    url: `/me/orders/${orderNo}/pay`,
    method: 'POST',
  })
}

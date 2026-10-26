import { request } from './request'
import type { CreateOrderPayload, OrderDetail, OrderSummary, PageResult } from '../types/api'

export interface OrderListParams {
  status?: string
  page?: number
  pageSize?: number
}

export function createOrder(payload: CreateOrderPayload): Promise<OrderDetail> {
  return request<OrderDetail>({ url: '/me/orders', method: 'POST', data: payload })
}

export function listOrders(params: OrderListParams = {}): Promise<PageResult<OrderSummary>> {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
  return request<PageResult<OrderSummary>>({
    url: query ? `/me/orders?${query}` : '/me/orders',
  })
}

export function getOrder(orderNo: string): Promise<OrderDetail> {
  return request<OrderDetail>({ url: `/me/orders/${orderNo}` })
}

export function cancelOrder(orderNo: string, reason: string): Promise<OrderDetail> {
  return request<OrderDetail>({
    url: `/me/orders/${orderNo}/cancel`,
    method: 'POST',
    data: { reason },
  })
}

export function confirmReceipt(orderNo: string): Promise<OrderDetail> {
  return request<OrderDetail>({
    url: `/me/orders/${orderNo}/confirm-receipt`,
    method: 'POST',
  })
}

import { request } from './request'
import type {
  AfterSaleDetail,
  AfterSaleStatus,
  AfterSaleSummary,
  CreateAfterSalePayload,
  PageResult,
} from '../types/api'

export function createAfterSale(
  orderNo: string,
  payload: CreateAfterSalePayload,
): Promise<AfterSaleDetail> {
  return request<AfterSaleDetail>({
    url: `/me/orders/${orderNo}/after-sales`,
    method: 'POST',
    data: payload,
  })
}

export function listAfterSales(params: {
  status?: AfterSaleStatus
  page?: number
  pageSize?: number
} = {}): Promise<PageResult<AfterSaleSummary>> {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && String(value) !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
  return request<PageResult<AfterSaleSummary>>({
    url: query ? `/me/after-sales?${query}` : '/me/after-sales',
  })
}

export function getAfterSale(afterSaleNo: string): Promise<AfterSaleDetail> {
  return request<AfterSaleDetail>({ url: `/me/after-sales/${afterSaleNo}` })
}

export function cancelAfterSale(afterSaleNo: string): Promise<AfterSaleDetail> {
  return request<AfterSaleDetail>({
    url: `/me/after-sales/${afterSaleNo}/cancel`,
    method: 'POST',
  })
}

export function submitReturnShipment(
  afterSaleNo: string,
  company: string,
  trackingNo: string,
): Promise<AfterSaleDetail> {
  return request<AfterSaleDetail>({
    url: `/me/after-sales/${afterSaleNo}/return-shipment`,
    method: 'POST',
    data: { company, trackingNo },
  })
}

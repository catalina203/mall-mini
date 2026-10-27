import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminAfterSaleDetail, AdminAfterSaleSummary } from '@/types/admin'

export interface AfterSaleQuery {
  page: number
  pageSize: number
  status?: string
  afterSaleNo?: string
  orderNo?: string
}

export async function fetchAfterSales(
  params: AfterSaleQuery,
): Promise<PageResult<AdminAfterSaleSummary>> {
  const response = await http.get<ApiResponse<PageResult<AdminAfterSaleSummary>>>(
    '/admin/after-sales',
    { params },
  )
  return response.data.data
}

export async function fetchAfterSaleDetail(
  afterSaleNo: string,
): Promise<AdminAfterSaleDetail> {
  const response = await http.get<ApiResponse<AdminAfterSaleDetail>>(
    `/admin/after-sales/${afterSaleNo}`,
  )
  return response.data.data
}

export async function approveAfterSale(
  afterSaleNo: string,
): Promise<AdminAfterSaleDetail> {
  const response = await http.post<ApiResponse<AdminAfterSaleDetail>>(
    `/admin/after-sales/${afterSaleNo}/approve`,
  )
  return response.data.data
}

export async function rejectAfterSale(
  afterSaleNo: string,
  reason: string,
): Promise<AdminAfterSaleDetail> {
  const response = await http.post<ApiResponse<AdminAfterSaleDetail>>(
    `/admin/after-sales/${afterSaleNo}/reject`,
    { reason },
  )
  return response.data.data
}

export async function confirmAfterSaleReturn(
  afterSaleNo: string,
): Promise<AdminAfterSaleDetail> {
  const response = await http.post<ApiResponse<AdminAfterSaleDetail>>(
    `/admin/after-sales/${afterSaleNo}/confirm-return`,
  )
  return response.data.data
}

export async function refundAfterSale(
  afterSaleNo: string,
  manualRefundNo: string,
  remark: string,
): Promise<AdminAfterSaleDetail> {
  const response = await http.post<ApiResponse<AdminAfterSaleDetail>>(
    `/admin/after-sales/${afterSaleNo}/refund`,
    {
      manualRefundNo: manualRefundNo.trim() || undefined,
      remark: remark.trim() || undefined,
    },
  )
  return response.data.data
}

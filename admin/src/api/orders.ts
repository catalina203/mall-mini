import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminOrderDetail, AdminOrderSummary } from '@/types/admin'

export interface OrderQuery {
  page: number
  pageSize: number
  status?: string
  orderNo?: string
  userId?: number
}

export async function fetchOrders(params: OrderQuery): Promise<PageResult<AdminOrderSummary>> {
  const response = await http.get<ApiResponse<PageResult<AdminOrderSummary>>>('/admin/orders', {
    params,
  })
  return response.data.data
}

export async function fetchOrderDetail(orderNo: string): Promise<AdminOrderDetail> {
  const response = await http.get<ApiResponse<AdminOrderDetail>>(`/admin/orders/${orderNo}`)
  return response.data.data
}

export async function shipOrder(
  orderNo: string,
  logisticsCompany: string,
  logisticsNo: string,
): Promise<AdminOrderDetail> {
  const response = await http.post<ApiResponse<AdminOrderDetail>>(`/admin/orders/${orderNo}/ship`, {
    logisticsCompany,
    logisticsNo,
  })
  return response.data.data
}

export async function cancelOrder(orderNo: string, reason: string): Promise<AdminOrderDetail> {
  const response = await http.post<ApiResponse<AdminOrderDetail>>(`/admin/orders/${orderNo}/cancel`, {
    reason,
  })
  return response.data.data
}

export async function updateOrderRemark(
  orderNo: string,
  remark: string,
): Promise<AdminOrderDetail> {
  const response = await http.patch<ApiResponse<AdminOrderDetail>>(
    `/admin/orders/${orderNo}/remark`,
    { remark },
  )
  return response.data.data
}

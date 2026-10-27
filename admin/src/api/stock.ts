import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminStockAlert, AdminStockLog } from '@/types/admin'

export interface StockLogQuery {
  page: number
  pageSize: number
  skuId?: number
  bizNo?: string
  changeType?: string
}

export async function fetchStockLogs(
  params: StockLogQuery,
): Promise<PageResult<AdminStockLog>> {
  const response = await http.get<ApiResponse<PageResult<AdminStockLog>>>('/admin/stock/logs', {
    params,
  })
  return response.data.data
}

export async function fetchStockAlerts(threshold?: number): Promise<AdminStockAlert[]> {
  const response = await http.get<ApiResponse<AdminStockAlert[]>>('/admin/stock/alerts', {
    params: threshold === undefined ? undefined : { threshold },
  })
  return response.data.data
}

export async function adjustStock(
  skuId: number,
  stock: number,
  remark: string,
): Promise<void> {
  await http.post<ApiResponse<void>>(`/admin/stock/skus/${skuId}/adjust`, {
    stock,
    remark: remark.trim() || undefined,
  })
}

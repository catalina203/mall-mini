import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminOperationLog } from '@/types/admin'

export interface OperationLogQuery {
  page: number
  pageSize: number
  adminId?: number
  action?: string
  targetType?: string
}

export async function fetchOperationLogs(
  params: OperationLogQuery,
): Promise<PageResult<AdminOperationLog>> {
  const response = await http.get<ApiResponse<PageResult<AdminOperationLog>>>(
    '/admin/operation-logs',
    { params },
  )
  return response.data.data
}

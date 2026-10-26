import http from './request'
import type { ApiResponse } from '@/types/api'
import type { DashboardSummary } from '@/types/admin'

export async function fetchDashboardSummary(days: number): Promise<DashboardSummary> {
  const response = await http.get<ApiResponse<DashboardSummary>>('/admin/dashboard/summary', {
    params: { days },
  })
  return response.data.data
}

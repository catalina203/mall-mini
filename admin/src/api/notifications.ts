import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminNotification } from '@/types/admin'

export interface NotificationQuery {
  page: number
  pageSize: number
  status?: string
  templateCode?: string
}

export async function fetchNotifications(
  params: NotificationQuery,
): Promise<PageResult<AdminNotification>> {
  const response = await http.get<ApiResponse<PageResult<AdminNotification>>>(
    '/admin/notification-logs',
    { params },
  )
  return response.data.data
}

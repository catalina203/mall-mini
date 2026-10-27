import http from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { AdminSupportTicket } from '@/types/admin'

export interface SupportQuery {
  page: number
  pageSize: number
  status?: string
  type?: string
  keyword?: string
}

export async function fetchSupportTickets(
  params: SupportQuery,
): Promise<PageResult<AdminSupportTicket>> {
  const response = await http.get<ApiResponse<PageResult<AdminSupportTicket>>>(
    '/admin/support-tickets',
    { params },
  )
  return response.data.data
}

export async function replySupportTicket(
  ticketNo: string,
  reply: string,
): Promise<AdminSupportTicket> {
  const response = await http.post<ApiResponse<AdminSupportTicket>>(
    `/admin/support-tickets/${ticketNo}/reply`,
    { reply },
  )
  return response.data.data
}

export async function closeSupportTicket(ticketNo: string): Promise<void> {
  await http.post(`/admin/support-tickets/${ticketNo}/close`)
}

import { request } from './request'
import type { PageResult, SupportTicket } from '../types/api'

export function createTicket(payload: {
  orderNo?: string
  type: string
  subject: string
  content: string
}): Promise<SupportTicket> {
  return request<SupportTicket>({ url: '/me/support-tickets', method: 'POST', data: payload })
}

export function listTickets(page = 1, pageSize = 20): Promise<PageResult<SupportTicket>> {
  return request<PageResult<SupportTicket>>({
    url: `/me/support-tickets?page=${page}&pageSize=${pageSize}`,
  })
}

export function closeTicket(ticketNo: string): Promise<void> {
  return request<void>({ url: `/me/support-tickets/${ticketNo}/close`, method: 'POST' })
}

import { request } from './request'
import type { PageResult, UserNotification } from '../types/api'

export function listNotifications(page = 1, pageSize = 20): Promise<PageResult<UserNotification>> {
  return request<PageResult<UserNotification>>({
    url: `/me/notifications?page=${page}&pageSize=${pageSize}`,
  })
}

export function unreadNotificationCount(): Promise<number> {
  return request<number>({ url: '/me/notifications/unread-count' })
}

export function markNotificationRead(id: number): Promise<void> {
  return request<void>({ url: `/me/notifications/${id}/read`, method: 'POST' })
}

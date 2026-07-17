import client from './client'
import type { Notification, Page, PageParams } from '../types/api'

// 알림 REST API (/api/notifications) — 로그인 사용자 본인 알림만.

export interface NotificationSearchParams extends PageParams {
  unreadOnly?: boolean
}

export async function listNotifications(
  params: NotificationSearchParams = {},
): Promise<Page<Notification>> {
  const { data } = await client.get<Page<Notification>>('/notifications', { params })
  return data
}

export async function getUnreadCount(): Promise<number> {
  const { data } = await client.get<{ count: number }>('/notifications/unread-count')
  return data.count
}

export async function markNotificationRead(id: number): Promise<void> {
  await client.post(`/notifications/${id}/read`)
}

export async function markAllNotificationsRead(): Promise<void> {
  await client.post('/notifications/read-all')
}

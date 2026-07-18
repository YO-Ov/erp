import client from './client'
import type { AdminRole, AdminUser } from '../types/api'

// 관리자 REST API (/api/admin) — ADMIN 전용. 사용자·역할 관리.
// 목록은 둘 다 배열(Page 아님).

export async function listAdminUsers(): Promise<AdminUser[]> {
  const { data } = await client.get<AdminUser[]>('/admin/users')
  return data
}

export async function listAdminRoles(): Promise<AdminRole[]> {
  const { data } = await client.get<AdminRole[]>('/admin/roles')
  return data
}

// 사용자 역할 통째 교체 — 선택된 roleIds 로(비면 모든 역할 회수).
export async function updateUserRoles(userId: number, roleIds: number[]): Promise<void> {
  await client.put(`/admin/users/${userId}/roles`, { roleIds })
}

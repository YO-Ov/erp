import client from './client'
import type { Account } from '../types/api'

// 계정과목 REST API (/api/accounts) — 백엔드 AccountController 와 1:1. FINANCE/ADMIN 전용.
// ⚠️ 다른 마스터(고객·품목·창고)와 달리 목록이 Page 가 아니라 배열 그대로 온다.

export async function listAccounts(): Promise<Account[]> {
  const { data } = await client.get<Account[]>('/accounts')
  return data
}

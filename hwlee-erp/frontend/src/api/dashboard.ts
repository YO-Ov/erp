import client from './client'
import type { SdDashboard } from '../types/api'

// 영업(SD) 대시보드 집계 — SALES/ADMIN 만.
// 목록 API 를 프론트에서 합산하면 페이징 때문에 부정확해서 서버가 집계해 내려준다.
export async function getSalesDashboard(): Promise<SdDashboard> {
  const { data } = await client.get<SdDashboard>('/sd/dashboard')
  return data
}

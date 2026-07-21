import client from './client'
import type { SalesOrderStatus, SdDashboard } from '../types/api'

// 영업(SD) 대시보드 집계 — SALES/ADMIN 만.
// 목록 API 를 프론트에서 합산하면 페이징 때문에 부정확해서 서버가 집계해 내려준다.
export async function getSalesDashboard(): Promise<SdDashboard> {
  const { data } = await client.get<SdDashboard>('/sd/dashboard')
  return data
}

// 상태별 수주 건수(파이프라인) — 기간 필터. '수주 진행 현황' 차트용.
// dateFrom·dateTo 를 둘 다 주면 그 기간, 없으면(=전체) 파라미터 없이 호출한다.
export type OrderStatusCounts = Record<SalesOrderStatus, number>
export async function getOrderStatus(params: {
  dateFrom?: string
  dateTo?: string
}): Promise<OrderStatusCounts> {
  const { data } = await client.get<OrderStatusCounts>('/sd/dashboard/order-status', { params })
  return data
}

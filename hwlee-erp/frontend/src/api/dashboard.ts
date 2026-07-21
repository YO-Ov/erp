import client from './client'
import type {
  FiDashboard,
  HrDashboard,
  MmDashboard,
  PpDashboard,
  SalesOrderStatus,
  SdDashboard,
} from '../types/api'

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

// 구매(MM) 대시보드 — PURCHASING/ADMIN.
export async function getPurchasingDashboard(): Promise<MmDashboard> {
  const { data } = await client.get<MmDashboard>('/mm/dashboard')
  return data
}

// 생산(PP) 대시보드 — PRODUCTION/ADMIN.
export async function getProductionDashboard(): Promise<PpDashboard> {
  const { data } = await client.get<PpDashboard>('/pp/dashboard')
  return data
}

// 재무(FI) 대시보드 — FINANCE/ADMIN.
export async function getFinanceDashboard(): Promise<FiDashboard> {
  const { data } = await client.get<FiDashboard>('/fi/dashboard')
  return data
}

// 인사(HR) 대시보드 — HR/ADMIN.
export async function getHrDashboard(): Promise<HrDashboard> {
  const { data } = await client.get<HrDashboard>('/hr/dashboard')
  return data
}

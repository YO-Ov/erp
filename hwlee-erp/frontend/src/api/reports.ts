import client from './client'
import type {
  IncomeStatement,
  InventoryReport,
  SalesReport,
  SalesReportUnit,
} from '../types/api'

// 리포트 REST API (/api/reports) — 조회 전용. FINANCE/DIRECTOR/ADMIN.

export async function getSalesReport(
  from: string,
  to: string,
  unit: SalesReportUnit = 'DAY',
): Promise<SalesReport> {
  const { data } = await client.get<SalesReport>('/reports/sales', {
    params: { from, to, unit },
  })
  return data
}

export async function getInventoryReport(params: {
  itemId?: number
  warehouseId?: number
} = {}): Promise<InventoryReport> {
  const { data } = await client.get<InventoryReport>('/reports/inventory', { params })
  return data
}

export async function getIncomeStatement(from: string, to: string): Promise<IncomeStatement> {
  const { data } = await client.get<IncomeStatement>('/reports/income-statement', {
    params: { from, to },
  })
  return data
}

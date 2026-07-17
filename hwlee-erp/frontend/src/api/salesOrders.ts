import client from './client'
import type {
  CreditStatus,
  Page,
  PageParams,
  SalesOrder,
  SalesOrderAction,
  SalesOrderCreateRequest,
  SalesOrderStatus,
  SalesOrderUpdateRequest,
} from '../types/api'

// 수주 REST API (/api/sales-orders) — 백엔드 SalesOrderController 와 1:1.

export interface SalesOrderSearchParams extends PageParams {
  customerId?: number
  salespersonId?: number
  status?: SalesOrderStatus
  dateFrom?: string
  dateTo?: string
}

export async function searchSalesOrders(
  params: SalesOrderSearchParams = {},
): Promise<Page<SalesOrder>> {
  const { data } = await client.get<Page<SalesOrder>>('/sales-orders', { params })
  return data
}

export async function getSalesOrder(id: string | number): Promise<SalesOrder> {
  const { data } = await client.get<SalesOrder>(`/sales-orders/${id}`)
  return data
}

export async function createSalesOrder(body: SalesOrderCreateRequest): Promise<SalesOrder> {
  const { data } = await client.post<SalesOrder>('/sales-orders', body)
  return data
}

export async function updateSalesOrder(
  id: string | number,
  body: SalesOrderUpdateRequest,
): Promise<SalesOrder> {
  const { data } = await client.put<SalesOrder>(`/sales-orders/${id}`, body)
  return data
}

export async function salesOrderAction(
  id: string | number,
  action: SalesOrderAction,
): Promise<SalesOrder> {
  const { data } = await client.post<SalesOrder>(`/sales-orders/${id}/${action}`)
  return data
}

// 고객 여신 현황 (수주 생성 시 참고).
export async function getCreditStatus(customerId: number): Promise<CreditStatus> {
  const { data } = await client.get<CreditStatus>('/sales-orders/credit-status', {
    params: { customerId },
  })
  return data
}

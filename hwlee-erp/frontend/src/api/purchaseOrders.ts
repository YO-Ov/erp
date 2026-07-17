import client from './client'
import type {
  Page,
  PageParams,
  PurchaseOrder,
  PurchaseOrderAction,
  PurchaseOrderCreateRequest,
  PurchaseOrderStatus,
  PurchaseOrderUpdateRequest,
} from '../types/api'

// 발주 REST API (/api/purchase-orders) — 백엔드 PurchaseOrderController 와 1:1.
// 조회는 여러 부서, 쓰기(생성/수정/상신/종료/취소)는 PURCHASING/ADMIN 만 허용된다.

export interface PurchaseOrderSearchParams extends PageParams {
  vendorId?: number
  status?: PurchaseOrderStatus
  dateFrom?: string
  dateTo?: string
}

export async function searchPurchaseOrders(
  params: PurchaseOrderSearchParams = {},
): Promise<Page<PurchaseOrder>> {
  const { data } = await client.get<Page<PurchaseOrder>>('/purchase-orders', { params })
  return data
}

export async function getPurchaseOrder(id: string | number): Promise<PurchaseOrder> {
  const { data } = await client.get<PurchaseOrder>(`/purchase-orders/${id}`)
  return data
}

export async function createPurchaseOrder(
  body: PurchaseOrderCreateRequest,
): Promise<PurchaseOrder> {
  const { data } = await client.post<PurchaseOrder>('/purchase-orders', body)
  return data
}

export async function updatePurchaseOrder(
  id: string | number,
  body: PurchaseOrderUpdateRequest,
): Promise<PurchaseOrder> {
  const { data } = await client.put<PurchaseOrder>(`/purchase-orders/${id}`, body)
  return data
}

export async function purchaseOrderAction(
  id: string | number,
  action: PurchaseOrderAction,
): Promise<PurchaseOrder> {
  const { data } = await client.post<PurchaseOrder>(`/purchase-orders/${id}/${action}`)
  return data
}

import client from './client'
import type {
  MaterialAvailability,
  Page,
  PageParams,
  ProductionOrder,
  ProductionOrderAction,
  ProductionOrderCreateRequest,
  ProductionOrderStatus,
} from '../types/api'

// 생산 작업지시 REST API (/api/production-orders) — ProductionController 와 1:1.
// 권한: PRODUCTION/ADMIN.

export interface ProductionOrderSearchParams extends PageParams {
  status?: ProductionOrderStatus
  dateFrom?: string
  dateTo?: string
}

export async function searchProductionOrders(
  params: ProductionOrderSearchParams = {},
): Promise<Page<ProductionOrder>> {
  const { data } = await client.get<Page<ProductionOrder>>('/production-orders', { params })
  return data
}

export async function getProductionOrder(id: string | number): Promise<ProductionOrder> {
  const { data } = await client.get<ProductionOrder>(`/production-orders/${id}`)
  return data
}

// 완제품 BOM 을 전개해 소요 자재 라인이 자동 생성된다(BOM 없으면 409).
export async function createProductionOrder(
  body: ProductionOrderCreateRequest,
): Promise<ProductionOrder> {
  const { data } = await client.post<ProductionOrder>('/production-orders', body)
  return data
}

// action: release(착수) | complete(완료) | cancel | dispatch(MES 전송)
export async function productionOrderAction(
  id: string | number,
  action: ProductionOrderAction,
): Promise<ProductionOrder> {
  const { data } = await client.post<ProductionOrder>(`/production-orders/${id}/${action}`)
  return data
}

// 자재 가용성(BOM 소요량 vs 현재고) — 생산 가능 여부 미리보기.
export async function getMaterialAvailability(
  id: string | number,
): Promise<MaterialAvailability> {
  const { data } = await client.get<MaterialAvailability>(
    `/production-orders/${id}/material-availability`,
  )
  return data
}

import client from './client'
import type {
  Delivery,
  DeliveryCreateRequest,
  DeliveryStatus,
  Page,
  PageParams,
} from '../types/api'

// 출하 REST API (/api/deliveries) — 백엔드 DeliveryController 와 1:1.

export interface DeliverySearchParams extends PageParams {
  salesOrderId?: number
  status?: DeliveryStatus
  dateFrom?: string
  dateTo?: string
}

export async function searchDeliveries(
  params: DeliverySearchParams = {},
): Promise<Page<Delivery>> {
  const { data } = await client.get<Page<Delivery>>('/deliveries', { params })
  return data
}

export async function getDelivery(id: string | number): Promise<Delivery> {
  const { data } = await client.get<Delivery>(`/deliveries/${id}`)
  return data
}

export async function createDelivery(body: DeliveryCreateRequest): Promise<Delivery> {
  const { data } = await client.post<Delivery>('/deliveries', body)
  return data
}

// 출하는 취소만 가능하다(전기 개념이 없다).
export async function deliveryAction(id: string | number, action: 'cancel'): Promise<Delivery> {
  const { data } = await client.post<Delivery>(`/deliveries/${id}/${action}`)
  return data
}

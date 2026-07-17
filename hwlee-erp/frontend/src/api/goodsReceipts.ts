import client from './client'
import type {
  GoodsReceipt,
  GoodsReceiptAction,
  GoodsReceiptCreateRequest,
  GoodsReceiptStatus,
  Page,
  PageParams,
} from '../types/api'

// 입고 REST API (/api/goods-receipts) — 백엔드 GoodsReceiptController 와 1:1.
// 조회·쓰기 모두 PURCHASING/ADMIN 만 허용된다.

export interface GoodsReceiptSearchParams extends PageParams {
  vendorId?: number
  status?: GoodsReceiptStatus
  dateFrom?: string
  dateTo?: string
}

export async function searchGoodsReceipts(
  params: GoodsReceiptSearchParams = {},
): Promise<Page<GoodsReceipt>> {
  const { data } = await client.get<Page<GoodsReceipt>>('/goods-receipts', { params })
  return data
}

export async function getGoodsReceipt(id: string | number): Promise<GoodsReceipt> {
  const { data } = await client.get<GoodsReceipt>(`/goods-receipts/${id}`)
  return data
}

export async function createGoodsReceipt(
  body: GoodsReceiptCreateRequest,
): Promise<GoodsReceipt> {
  const { data } = await client.post<GoodsReceipt>('/goods-receipts', body)
  return data
}

// action: post(전기 — 재고 가중평균 갱신·발주 RECEIVED 전이) | cancel
export async function goodsReceiptAction(
  id: string | number,
  action: GoodsReceiptAction,
): Promise<GoodsReceipt> {
  const { data } = await client.post<GoodsReceipt>(`/goods-receipts/${id}/${action}`)
  return data
}

import client from './client'
import type {
  MovementReason,
  Page,
  PageParams,
  Stock,
  StockMovement,
} from '../types/api'

// 재고 조회 REST API — 전부 읽기 전용. 변경은 입고/출고/조정 트랜잭션만 수행한다.
//  - 현재고(/stocks)        : SALES/PURCHASING/ADMIN (영업도 출하 위해 재고를 봐야 함)
//  - 이동이력(/stock-movements): PURCHASING/ADMIN

export interface StockSearchParams extends PageParams {
  itemId?: number
  warehouseId?: number
  /** 재고 수량이 이 값보다 큰 것만 — 0 을 주면 '재고 있는 것만'. */
  qtyGt?: number
}

export async function searchStocks(params: StockSearchParams = {}): Promise<Page<Stock>> {
  const { data } = await client.get<Page<Stock>>('/stocks', { params })
  return data
}

export interface StockMovementSearchParams extends PageParams {
  itemId?: number
  warehouseId?: number
  reason?: MovementReason
  dateFrom?: string
  dateTo?: string
}

export async function searchStockMovements(
  params: StockMovementSearchParams = {},
): Promise<Page<StockMovement>> {
  const { data } = await client.get<Page<StockMovement>>('/stock-movements', { params })
  return data
}

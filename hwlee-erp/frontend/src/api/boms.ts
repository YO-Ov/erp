import client from './client'
import type { Bom } from '../types/api'

// BOM 조회 REST API (/api/boms) — PRODUCTION/ADMIN. 완제품별 부품 소요량.
// 응답은 배열(완제품의 부품 라인 목록).
export async function listBomsByProduct(productItemId: number): Promise<Bom[]> {
  const { data } = await client.get<Bom[]>('/boms', { params: { productItemId } })
  return data
}

import client from './client'
import type {
  Page,
  PageParams,
  Quotation,
  QuotationAction,
  QuotationCreateRequest,
  QuotationStatus,
  QuotationUpdateRequest,
} from '../types/api'

// 견적 REST API (/api/quotations) — 백엔드 QuotationController 와 1:1.

export interface QuotationSearchParams extends PageParams {
  customerId?: number
  status?: QuotationStatus
  dateFrom?: string
  dateTo?: string
}

export async function searchQuotations(
  params: QuotationSearchParams = {},
): Promise<Page<Quotation>> {
  const { data } = await client.get<Page<Quotation>>('/quotations', { params })
  return data
}

export async function getQuotation(id: string | number): Promise<Quotation> {
  const { data } = await client.get<Quotation>(`/quotations/${id}`)
  return data
}

export async function createQuotation(body: QuotationCreateRequest): Promise<Quotation> {
  const { data } = await client.post<Quotation>('/quotations', body)
  return data
}

export async function updateQuotation(
  id: string | number,
  body: QuotationUpdateRequest,
): Promise<Quotation> {
  const { data } = await client.put<Quotation>(`/quotations/${id}`, body)
  return data
}

// 상태 전이 액션들. 모두 POST /api/quotations/{id}/{action}
export async function quotationAction(
  id: string | number,
  action: QuotationAction,
): Promise<Quotation> {
  const { data } = await client.post<Quotation>(`/quotations/${id}/${action}`)
  return data
}

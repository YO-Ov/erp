import client from './client'
import type {
  Approval,
  Page,
  PageParams,
  Payment,
  PaymentCreateRequest,
  PaymentType,
} from '../types/api'

// 입금/출금 REST API (/api/payments) — 백엔드 PaymentController 와 1:1.
// 전부 FINANCE/ADMIN 전용.

export interface PaymentSearchParams extends PageParams {
  type?: PaymentType
  customerId?: number
  vendorId?: number
  dateFrom?: string
  dateTo?: string
}

export async function searchPayments(params: PaymentSearchParams = {}): Promise<Page<Payment>> {
  const { data } = await client.get<Page<Payment>>('/payments', { params })
  return data
}

export async function getPayment(id: string | number): Promise<Payment> {
  const { data } = await client.get<Payment>(`/payments/${id}`)
  return data
}

// 즉시 전기(POSTED) — 등록과 동시에 분개까지 생성된다.
// body 의 type/party 조합은 PaymentCreateRequest 유니온이 강제한다.
export async function createPayment(body: PaymentCreateRequest): Promise<Payment> {
  const { data } = await client.post<Payment>('/payments', body)
  return data
}

// 결재용 초안(DRAFT) — 전기는 결재 최종 승인 시점에.
export async function createPaymentDraft(body: PaymentCreateRequest): Promise<Payment> {
  const { data } = await client.post<Payment>('/payments/draft', body)
  return data
}

// 출금(DISBURSEMENT) + DRAFT 만 상신 가능. 응답은 생성된 결재 문서.
export async function submitPaymentApproval(id: string | number): Promise<Approval> {
  const { data } = await client.post<Approval>(`/payments/${id}/submit-approval`)
  return data
}

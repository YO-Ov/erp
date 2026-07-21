import client from './client'
import type {
  CreditLimitRequest,
  CreditLimitRequestCreateRequest,
  CreditRequestStatus,
  Page,
  PageParams,
} from '../types/api'

// 여신 상향 요청 REST API (/api/credit-limit-requests).
// 조회 SALES/FINANCE/ADMIN, 생성 SALES/ADMIN. 승인/거부는 전자결재로 이관.

export interface CreditRequestSearchParams extends PageParams {
  status?: CreditRequestStatus
}

export async function searchCreditRequests(
  params: CreditRequestSearchParams = {},
): Promise<Page<CreditLimitRequest>> {
  const { data } = await client.get<Page<CreditLimitRequest>>('/credit-limit-requests', { params })
  return data
}

export async function getCreditRequest(id: string | number): Promise<CreditLimitRequest> {
  const { data } = await client.get<CreditLimitRequest>(`/credit-limit-requests/${id}`)
  return data
}

// 특정 고객에 검토 대기(PENDING) 중인 여신 요청이 있으면 반환, 없으면 204 → null.
// 고객 상세에서 "여신 검토 중" 배지로 중복 신청을 막는 데 쓴다.
export async function getPendingCreditRequest(
  customerId: number,
): Promise<CreditLimitRequest | null> {
  const res = await client.get<CreditLimitRequest>('/credit-limit-requests/pending', {
    params: { customerId },
  })
  return res.status === 204 ? null : res.data
}

// 생성하면 서버가 전자결재를 상신한다 — 재무팀장이 결재함에서 승인/거부한다.
export async function createCreditRequest(
  body: CreditLimitRequestCreateRequest,
): Promise<CreditLimitRequest> {
  const { data } = await client.post<CreditLimitRequest>('/credit-limit-requests', body)
  return data
}

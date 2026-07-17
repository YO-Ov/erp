import client from './client'
import type {
  Approval,
  ApprovalAction,
  ApprovalActionRequest,
  ApprovalDocType,
  Page,
  PageParams,
} from '../types/api'

// 전자결재 REST API (/api/approvals) — 백엔드 ApprovalController 와 1:1.
// 역할 제한이 없다(isAuthenticated) — 누가 처리할 수 있는지는 결재선이 정하고,
// 서버가 응답의 myTurn 으로 알려준다.

// 내가 처리해야 할 결재(결재함).
export async function getInbox(params: PageParams = {}): Promise<Page<Approval>> {
  const { data } = await client.get<Page<Approval>>('/approvals/inbox', { params })
  return data
}

// 내가 올린 결재(상신함).
export async function getOutbox(params: PageParams = {}): Promise<Page<Approval>> {
  const { data } = await client.get<Page<Approval>>('/approvals/outbox', { params })
  return data
}

export async function getApproval(id: string | number): Promise<Approval> {
  const { data } = await client.get<Approval>(`/approvals/${id}`)
  return data
}

// 원본 문서의 최신 결재 — 없으면 204(본문 없음)라 null 을 돌려준다.
export async function getApprovalForDoc(
  docType: ApprovalDocType,
  refId: number,
): Promise<Approval | null> {
  const { data } = await client.get<Approval | ''>('/approvals/for', {
    params: { docType, refId },
  })
  return data || null
}

// comment 는 반려/반송 사유로 쓰인다(회수·재상신은 본문 없음).
export async function approvalAction(
  id: string | number,
  action: ApprovalAction,
  comment?: string,
): Promise<Approval> {
  const body: ApprovalActionRequest | undefined = ['approve', 'reject', 'return'].includes(action)
    ? { comment: comment || null }
    : undefined
  const { data } = await client.post<Approval>(`/approvals/${id}/${action}`, body)
  return data
}

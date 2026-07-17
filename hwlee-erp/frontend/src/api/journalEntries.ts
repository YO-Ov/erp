import client from './client'
import type {
  Approval,
  JournalEntry,
  JournalEntryCreateRequest,
  JournalEntryStatus,
  JournalSource,
  Page,
  PageParams,
} from '../types/api'

// 전표 REST API (/api/journal-entries) — 백엔드 JournalEntryController 와 1:1.
// 전부 FINANCE/ADMIN 전용.

export interface JournalEntrySearchParams extends PageParams {
  sourceType?: JournalSource
  sourceId?: number | string
  status?: JournalEntryStatus
  dateFrom?: string
  dateTo?: string
}

export async function searchJournalEntries(
  params: JournalEntrySearchParams = {},
): Promise<Page<JournalEntry>> {
  const { data } = await client.get<Page<JournalEntry>>('/journal-entries', { params })
  return data
}

export async function getJournalEntry(id: string | number): Promise<JournalEntry> {
  const { data } = await client.get<JournalEntry>(`/journal-entries/${id}`)
  return data
}

// 즉시 전기(POSTED)로 생성. 차/대가 안 맞으면 422 UNBALANCED_JOURNAL.
export async function createJournalEntry(
  body: JournalEntryCreateRequest,
): Promise<JournalEntry> {
  const { data } = await client.post<JournalEntry>('/journal-entries', body)
  return data
}

// 결재용 초안(DRAFT)으로만 저장 — 전기는 결재 최종 승인 시점에 일어난다.
export async function createJournalEntryDraft(
  body: JournalEntryCreateRequest,
): Promise<JournalEntry> {
  const { data } = await client.post<JournalEntry>('/journal-entries/draft', body)
  return data
}

// DRAFT + 수동(MANUAL) 전표만 상신 가능. 응답은 전표가 아니라 생성된 결재 문서다.
export async function submitJournalEntryApproval(id: string | number): Promise<Approval> {
  const { data } = await client.post<Approval>(`/journal-entries/${id}/submit-approval`)
  return data
}

// 취소는 POSTED 전표만 가능하다(DRAFT는 취소 대상이 아니다).
export async function cancelJournalEntry(id: string | number): Promise<JournalEntry> {
  const { data } = await client.post<JournalEntry>(`/journal-entries/${id}/cancel`)
  return data
}

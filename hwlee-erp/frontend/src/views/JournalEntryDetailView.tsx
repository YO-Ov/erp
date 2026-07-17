import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getJournalEntry,
  cancelJournalEntry,
  submitJournalEntryApproval,
} from '../api/journalEntries'
import {
  JOURNAL_ENTRY_STATUS,
  JOURNAL_SOURCE,
  journalEntryActions,
  ACTION_LABEL,
  formatMoney,
  type JournalEntryButton,
} from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import type { Approval, JournalEntry } from '../types/api'

export default function JournalEntryDetailView() {
  // 라우트가 /journal-entries/:id 라 항상 있지만 타입상으론 undefined 가능 — 빈 문자열로 좁힌다.
  const { id = '' } = useParams()
  const queryClient = useQueryClient()

  const { data: je, isLoading, isError, error } = useQuery({
    queryKey: ['journal-entry', id],
    queryFn: () => getJournalEntry(id),
  })

  // 취소는 전표를, 상신은 결재 문서를 돌려주므로 반환 타입이 갈린다 — 유니온으로 명시한다.
  const mutation = useMutation<JournalEntry | Approval, Error, JournalEntryButton>({
    mutationFn: (action) =>
      action === 'cancel' ? cancelJournalEntry(id) : submitJournalEntryApproval(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['journal-entry', id] })
      queryClient.invalidateQueries({ queryKey: ['journal-entries'] })
    },
  })

  function onAction(action: JournalEntryButton) {
    if (
      action === 'cancel' &&
      !window.confirm('전기된 전표를 취소하시겠습니까? 원장 반영이 되돌려집니다.')
    ) {
      return
    }
    mutation.mutate(action)
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  // 로딩·에러가 아니면 데이터가 있지만 TS는 모른다 — 아래에서 je 를 그냥 쓰기 위해 좁힌다.
  if (!je) return null

  const actions = journalEntryActions(je.status, je.sourceType)
  // 차/대가 맞는지는 복식부기의 핵심 불변식 — 전기된 전표는 항상 맞아야 한다.
  const balanced = Number(je.totalDebit) === Number(je.totalCredit)

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/journal-entries" className="muted">
            ← 전표 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{je.number}</span>{' '}
            <StatusBadge map={JOURNAL_ENTRY_STATUS} status={je.status} />
          </h1>
        </div>
        <div className="actions">
          {actions.map((a) => (
            <button
              key={a}
              className={a === 'cancel' ? 'danger' : 'primary'}
              disabled={mutation.isPending}
              onClick={() => onAction(a)}
            >
              {ACTION_LABEL[a]}
            </button>
          ))}
        </div>
      </div>

      {mutation.isError && <p className="error">{mutation.error.message}</p>}

      <div className="panel">
        <div className="info-grid">
          <div>
            <div className="k">전표일</div>
            <div className="v">{je.entryDate}</div>
          </div>
          <div>
            <div className="k">출처</div>
            <div className="v">
              {JOURNAL_SOURCE[je.sourceType] || je.sourceType}
              {je.sourceId && <span className="muted mono"> #{je.sourceId}</span>}
            </div>
          </div>
          <div>
            <div className="k">전기일시</div>
            <div className="v">{je.postedAt || '-'}</div>
          </div>
          <div>
            <div className="k">차변합 / 대변합</div>
            <div className="v mono">
              {formatMoney(je.totalDebit)} / {formatMoney(je.totalCredit)}{' '}
              {balanced ? (
                <span style={{ color: 'var(--ok, green)' }}>✓ 균형</span>
              ) : (
                <span className="error">불균형</span>
              )}
            </div>
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            <div className="k">적요</div>
            <div className="v">{je.description}</div>
          </div>
        </div>
      </div>

      <div className="section-title">분개 라인</div>
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th className="num" style={{ width: 60 }}>#</th>
              <th>계정과목</th>
              <th className="num">차변</th>
              <th className="num">대변</th>
            </tr>
          </thead>
          <tbody>
            {(je.lines || []).map((l) => (
              <tr key={l.id}>
                <td className="num mono">{l.lineNo}</td>
                <td>
                  {l.accountName} <span className="muted mono">{l.accountCode}</span>
                </td>
                <td className="num mono">
                  {Number(l.debit) > 0 ? formatMoney(l.debit) : '-'}
                </td>
                <td className="num mono">
                  {Number(l.credit) > 0 ? formatMoney(l.credit) : '-'}
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={2} style={{ textAlign: 'right' }}>
                <strong>합계</strong>
              </td>
              <td className="num mono">
                <strong>{formatMoney(je.totalDebit)}</strong>
              </td>
              <td className="num mono">
                <strong>{formatMoney(je.totalCredit)}</strong>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        작성 {je.createdBy} · {je.createdAt} / 수정 {je.updatedBy} · {je.updatedAt}
      </p>
    </div>
  )
}

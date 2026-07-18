import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { createJournalEntry, createJournalEntryDraft } from '../api/journalEntries'
import { listAccounts } from '../api/accounts'
import { ACCOUNT_TYPE, formatMoney } from '../domain/status'
import type { JournalEntry, JournalEntryCreateRequest } from '../types/api'

/** 저장 방식 — 즉시 전기(POSTED)냐, 결재용 초안(DRAFT)이냐. */
type SaveMode = 'post' | 'draft'

/** 분개 라인 입력값. 차/대 중 한 쪽만 금액을 가지므로 '방향 + 금액'으로 받는다. */
interface LineInput {
  accountCode: string
  side: 'DEBIT' | 'CREDIT'
  amount: number | string
}

function today() {
  return new Date().toISOString().slice(0, 10)
}

// 라인은 차변 또는 대변 중 한 쪽만 금액을 갖는다(백엔드가 강제).
// 그래서 금액칸을 둘로 나누는 대신 '방향 + 금액'으로 입력받고 전송 시 debit/credit 으로 편다.
const emptyLine = (): LineInput => ({ accountCode: '', side: 'DEBIT', amount: '' })

// 수동 전표 작성.
//  - '전기'      → POST /journal-entries      (즉시 POSTED. 차/대 불일치면 422)
//  - '초안 저장' → POST /journal-entries/draft (DRAFT. 결재 상신해서 승인 시 자동 전기)
export default function JournalEntryCreateView() {
  const navigate = useNavigate()

  const { data: accounts = [] } = useQuery({
    queryKey: ['accounts'],
    queryFn: listAccounts,
  })

  const [entryDate, setEntryDate] = useState(today())
  const [description, setDescription] = useState('')
  const [lines, setLines] = useState<LineInput[]>([
    { ...emptyLine(), side: 'DEBIT' },
    { ...emptyLine(), side: 'CREDIT' },
  ])

  // 전표를 실제로 끊을 수 있는 건 말단(postable) 계정뿐 —
  // '자산'·'부채' 같은 상위 분류 계정에는 분개할 수 없다.
  const postableAccounts = accounts.filter((a) => a.postable && a.status === 'ACTIVE')

  const mutation = useMutation<JournalEntry, Error, { body: JournalEntryCreateRequest; mode: SaveMode }>({
    mutationFn: ({ body, mode }) =>
      mode === 'post' ? createJournalEntry(body) : createJournalEntryDraft(body),
    onSuccess: (saved) => navigate(`/journal-entries/${saved.id}`),
  })

  function updateLine(idx: number, patch: Partial<LineInput>) {
    setLines((ls) => ls.map((l, i) => (i === idx ? { ...l, ...patch } : l)))
  }
  function addLine() {
    setLines((ls) => [...ls, emptyLine()])
  }
  function removeLine(idx: number) {
    setLines((ls) => (ls.length > 2 ? ls.filter((_, i) => i !== idx) : ls))
  }

  const totalDebit = lines
    .filter((l) => l.side === 'DEBIT')
    .reduce((s, l) => s + (Number(l.amount) || 0), 0)
  const totalCredit = lines
    .filter((l) => l.side === 'CREDIT')
    .reduce((s, l) => s + (Number(l.amount) || 0), 0)
  const diff = totalDebit - totalCredit
  const balanced = diff === 0 && totalDebit > 0

  const linesFilled = lines.every((l) => l.accountCode && Number(l.amount) > 0)
  const canSubmit = entryDate && description.trim() && linesFilled

  function buildBody(): JournalEntryCreateRequest {
    return {
      entryDate,
      description: description.trim(),
      lines: lines.map((l) => ({
        accountCode: l.accountCode,
        debit: l.side === 'DEBIT' ? Number(l.amount) : 0,
        credit: l.side === 'CREDIT' ? Number(l.amount) : 0,
      })),
    }
  }

  function onSubmit(e: FormEvent, mode: SaveMode) {
    e.preventDefault()
    mutation.mutate({ body: buildBody(), mode })
  }

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/journal-entries" className="muted">
            ← 전표 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>수동 전표 작성</h1>
        </div>
      </div>

      <form onSubmit={(e) => onSubmit(e, 'post')}>
        <div className="panel">
          <div className="row">
            <div className="field">
              <label>전표일 *</label>
              <input
                type="date"
                value={entryDate}
                onChange={(e) => setEntryDate(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>
            <div className="field" style={{ flex: 2 }}>
              <label>적요 *</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="예) 사무용품 구입"
                style={{ width: '100%' }}
                required
              />
            </div>
          </div>
        </div>

        <div className="section-title">분개 라인</div>
        <div className="panel">
          <table>
            <thead>
              <tr>
                <th style={{ width: '50%' }}>계정과목</th>
                <th style={{ width: 110 }}>차/대</th>
                <th className="num">금액</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {lines.map((l, idx) => (
                <tr key={idx}>
                  <td>
                    <select
                      value={l.accountCode}
                      onChange={(e) => updateLine(idx, { accountCode: e.target.value })}
                      style={{ width: '100%' }}
                    >
                      <option value="">계정과목 선택</option>
                      {postableAccounts.map((a) => (
                        <option key={a.id} value={a.code}>
                          {a.code} {a.name} ({ACCOUNT_TYPE[a.type] || a.type})
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <select
                      value={l.side}
                      onChange={(e) => updateLine(idx, { side: e.target.value as LineInput['side'] })}
                      style={{ width: '100%' }}
                    >
                      <option value="DEBIT">차변</option>
                      <option value="CREDIT">대변</option>
                    </select>
                  </td>
                  <td className="num">
                    <input
                      type="number"
                      min="0"
                      step="1"
                      value={l.amount}
                      onChange={(e) => updateLine(idx, { amount: e.target.value })}
                      style={{ width: 140, textAlign: 'right' }}
                    />
                  </td>
                  <td>
                    <button
                      type="button"
                      className="sm danger"
                      onClick={() => removeLine(idx)}
                      disabled={lines.length <= 2}
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between' }}>
            <button type="button" className="sm" onClick={addLine}>
              + 라인 추가
            </button>
            <div style={{ textAlign: 'right' }}>
              <div>
                차변 <strong className="mono">{formatMoney(totalDebit)}</strong> · 대변{' '}
                <strong className="mono">{formatMoney(totalCredit)}</strong>
              </div>
              <div style={{ marginTop: 4 }}>
                {balanced ? (
                  <span style={{ color: 'var(--tone-active)' }}>✓ 차/대가 일치합니다</span>
                ) : (
                  <span className="error">
                    차/대 불일치 — 차액 {formatMoney(Math.abs(diff))} (
                    {diff > 0 ? '대변 부족' : '차변 부족'})
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>

        {mutation.isError && (
          <p className="error" style={{ marginTop: 12 }}>
            {mutation.error.message}
          </p>
        )}

        <div className="actions" style={{ marginTop: 16 }}>
          {/* 전기는 차/대가 맞아야만. 안 맞으면 백엔드가 422 로 막지만 프론트가 먼저 알려준다. */}
          <button
            className="primary"
            type="submit"
            disabled={!canSubmit || !balanced || mutation.isPending}
          >
            {mutation.isPending ? '저장 중…' : '전기(확정)'}
          </button>
          {/* 초안은 차/대가 안 맞아도 저장된다 — 균형 검증은 결재 승인·전기 시점에 일어난다. */}
          <button
            type="button"
            disabled={!canSubmit || mutation.isPending}
            onClick={(e) => onSubmit(e, 'draft')}
          >
            초안 저장(결재용)
          </button>
          <Link to="/journal-entries">
            <button type="button">취소</button>
          </Link>
        </div>
        <p className="muted" style={{ marginTop: 10, fontSize: 12 }}>
          전기하면 즉시 원장에 반영됩니다. 초안으로 저장하면 상세 화면에서 결재 상신할 수 있고,
          최종 승인 시점에 자동으로 전기됩니다.
        </p>
      </form>
    </div>
  )
}

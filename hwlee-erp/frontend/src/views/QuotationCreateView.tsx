import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { createQuotation, getQuotation, updateQuotation } from '../api/quotations'
import { listCustomers, listItems } from '../api/masters'
import { formatMoney } from '../domain/status'
import type {
  Quotation,
  QuotationCreateRequest,
  QuotationUpdateRequest,
} from '../types/api'

/** 작성이냐 수정이냐에 따라 보낼 DTO 가 다르다 — 수정 DTO 엔 customerId 가 없다(고객 변경 불가). */
type QuotationFormBody = QuotationCreateRequest | QuotationUpdateRequest

/** 라인 입력값. 수량·단가는 입력 중 문자열이 될 수 있다. */
interface LineInput {
  itemId: number | string
  quantity: number | string
  unitPrice: number | string
}

// 오늘 날짜 yyyy-MM-dd.
function today() {
  return new Date().toISOString().slice(0, 10)
}

const emptyLine = (): LineInput => ({ itemId: '', quantity: 1, unitPrice: 0 })

// 작성/수정 겸용 화면.
//  - /quotations/new       → 신규 작성(POST)
//  - /quotations/:id/edit  → 수정(PUT). 기존 견적을 불러와 프리필하고 고객은 고정.
export default function QuotationCreateView() {
  const navigate = useNavigate()
  const { id = '' } = useParams()
  const editMode = !!id

  const { data: customers = [] } = useQuery({
    queryKey: ['customers'],
    queryFn: () => listCustomers(),
  })
  const { data: items = [] } = useQuery({
    queryKey: ['items'],
    queryFn: () => listItems(),
  })

  // 수정 모드면 기존 견적을 불러온다.
  const { data: existing } = useQuery({
    queryKey: ['quotation', id],
    queryFn: () => getQuotation(id),
    enabled: editMode,
  })

  const [customerId, setCustomerId] = useState('')
  const [issuedDate, setIssuedDate] = useState(today())
  const [validUntil, setValidUntil] = useState('')
  const [lines, setLines] = useState<LineInput[]>([emptyLine()])

  // 기존 견적 로드가 끝나면 폼을 채운다.
  useEffect(() => {
    if (!existing) return
    setCustomerId(String(existing.customerId))
    setIssuedDate(existing.issuedDate)
    setValidUntil(existing.validUntil || '')
    setLines(
      (existing.lines || []).map((l) => ({
        itemId: String(l.itemId),
        quantity: l.quantity,
        unitPrice: l.unitPrice,
      })),
    )
  }, [existing])

  const mutation = useMutation<Quotation, Error, QuotationFormBody>({
    // customerId 유무가 곧 작성/수정 구분이다 — 수정 DTO 엔 그 필드가 없다.
    mutationFn: (body) =>
      'customerId' in body ? createQuotation(body) : updateQuotation(id, body),
    onSuccess: (saved) => navigate(`/quotations/${saved.id}`),
  })

  function updateLine(idx: number, patch: Partial<LineInput>) {
    setLines((ls) => ls.map((l, i) => (i === idx ? { ...l, ...patch } : l)))
  }
  function addLine() {
    setLines((ls) => [...ls, emptyLine()])
  }
  function removeLine(idx: number) {
    setLines((ls) => (ls.length > 1 ? ls.filter((_, i) => i !== idx) : ls))
  }

  const total = lines.reduce(
    (sum, l) => sum + (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0),
    0,
  )

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const lineBody = lines.map((l) => ({
      itemId: Number(l.itemId),
      quantity: Number(l.quantity),
      unitPrice: Number(l.unitPrice),
    }))
    // 수정 DTO엔 고객이 없다(고객 변경 불가). 신규만 customerId를 보낸다.
    const body: QuotationFormBody = editMode
      ? { issuedDate, validUntil: validUntil || null, lines: lineBody }
      : {
          customerId: Number(customerId),
          issuedDate,
          validUntil: validUntil || null,
          lines: lineBody,
        }
    mutation.mutate(body)
  }

  const canSubmit =
    customerId && issuedDate && lines.every((l) => l.itemId && Number(l.quantity) > 0)

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to={editMode ? `/quotations/${id}` : '/quotations'} className="muted">
            ← {editMode ? '견적 상세' : '견적 목록'}
          </Link>
          <h1 style={{ marginTop: 6 }}>
            {editMode ? `견적 수정 ${existing?.number || ''}` : '견적 작성'}
          </h1>
        </div>
      </div>

      <form onSubmit={onSubmit}>
        <div className="panel">
          <div className="row">
            <div className="field">
              <label>고객 *</label>
              <select
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                style={{ width: '100%' }}
                disabled={editMode}
                required
              >
                <option value="">고객 선택</option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.code})
                  </option>
                ))}
              </select>
              {editMode && (
                <span className="muted" style={{ fontSize: 12 }}>
                  고객은 수정할 수 없습니다.
                </span>
              )}
            </div>
            <div className="field">
              <label>발행일 *</label>
              <input
                type="date"
                value={issuedDate}
                onChange={(e) => setIssuedDate(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>
            <div className="field">
              <label>유효기한</label>
              <input
                type="date"
                value={validUntil}
                onChange={(e) => setValidUntil(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
          </div>
        </div>

        <div className="section-title">견적 라인</div>
        <div className="panel">
          <table>
            <thead>
              <tr>
                <th style={{ width: '45%' }}>품목</th>
                <th className="num">수량</th>
                <th className="num">단가</th>
                <th className="num">금액</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {lines.map((l, idx) => (
                <tr key={idx}>
                  <td>
                    <select
                      value={l.itemId}
                      onChange={(e) => updateLine(idx, { itemId: e.target.value })}
                      style={{ width: '100%' }}
                    >
                      <option value="">품목 선택</option>
                      {items.map((it) => (
                        <option key={it.id} value={it.id}>
                          {it.name} ({it.code})
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="num">
                    <input
                      type="number"
                      min="1"
                      value={l.quantity}
                      onChange={(e) => updateLine(idx, { quantity: e.target.value })}
                      style={{ width: 90, textAlign: 'right' }}
                    />
                  </td>
                  <td className="num">
                    <input
                      type="number"
                      min="0"
                      value={l.unitPrice}
                      onChange={(e) => updateLine(idx, { unitPrice: e.target.value })}
                      style={{ width: 120, textAlign: 'right' }}
                    />
                  </td>
                  <td className="num mono">
                    {formatMoney((Number(l.quantity) || 0) * (Number(l.unitPrice) || 0))}
                  </td>
                  <td>
                    <button
                      type="button"
                      className="sm danger"
                      onClick={() => removeLine(idx)}
                      disabled={lines.length <= 1}
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
            <div>
              합계 <strong className="mono">{formatMoney(total)}</strong>
            </div>
          </div>
        </div>

        {mutation.isError && (
          <p className="error" style={{ marginTop: 12 }}>
            {mutation.error.message}
          </p>
        )}

        <div className="actions" style={{ marginTop: 16 }}>
          <button
            className="primary"
            type="submit"
            disabled={!canSubmit || mutation.isPending}
          >
            {mutation.isPending ? '저장 중…' : editMode ? '수정 저장' : '견적 생성'}
          </button>
          <Link to={editMode ? `/quotations/${id}` : '/quotations'}>
            <button type="button">취소</button>
          </Link>
        </div>
      </form>
    </div>
  )
}

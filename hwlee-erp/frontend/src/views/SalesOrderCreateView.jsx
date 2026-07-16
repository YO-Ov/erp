import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  createSalesOrder,
  getCreditStatus,
  getSalesOrder,
  updateSalesOrder,
} from '../api/salesOrders'
import { getQuotation } from '../api/quotations'
import { listCustomers, listItems } from '../api/masters'
import { formatMoney } from '../domain/status'

function today() {
  return new Date().toISOString().slice(0, 10)
}
const emptyLine = () => ({ itemId: '', orderQty: 1, unitPrice: 0 })

// 작성/수정 겸용 화면.
//  - /sales-orders/new                    → 신규 등록(POST). ?quotationId= 있으면 견적에서 전환.
//  - /sales-orders/:id/edit               → 수정(PUT). 고객·원본견적 고정, 영업담당은 기존값 보존.
export default function SalesOrderCreateView() {
  const navigate = useNavigate()
  const { id } = useParams()
  const editMode = !!id
  const [searchParams] = useSearchParams()
  const quotationId = searchParams.get('quotationId')

  const { data: customers = [] } = useQuery({
    queryKey: ['customers'],
    queryFn: () => listCustomers(),
  })
  const { data: items = [] } = useQuery({
    queryKey: ['items'],
    queryFn: () => listItems(),
  })

  // 견적→수주 전환 시 원본 견적(신규 등록에서만).
  const { data: srcQuotation } = useQuery({
    queryKey: ['quotation', quotationId],
    queryFn: () => getQuotation(quotationId),
    enabled: !editMode && !!quotationId,
  })

  // 수정 모드면 기존 수주.
  const { data: existing } = useQuery({
    queryKey: ['sales-order', id],
    queryFn: () => getSalesOrder(id),
    enabled: editMode,
  })

  const [customerId, setCustomerId] = useState('')
  const [salespersonId, setSalespersonId] = useState(null) // 폼엔 없지만 수정 시 보존
  const [orderDate, setOrderDate] = useState(today())
  const [lines, setLines] = useState([emptyLine()])

  // 견적에서 전환된 경우 프리필.
  useEffect(() => {
    if (!srcQuotation) return
    setCustomerId(String(srcQuotation.customerId))
    setLines(
      (srcQuotation.lines || []).map((l) => ({
        itemId: String(l.itemId),
        orderQty: l.quantity,
        unitPrice: l.unitPrice,
      })),
    )
  }, [srcQuotation])

  // 기존 수주 로드가 끝나면 프리필.
  useEffect(() => {
    if (!existing) return
    setCustomerId(String(existing.customerId))
    setSalespersonId(existing.salespersonId ?? null)
    setOrderDate(existing.orderDate)
    setLines(
      (existing.lines || []).map((l) => ({
        itemId: String(l.itemId),
        orderQty: l.orderQty,
        unitPrice: l.unitPrice,
      })),
    )
  }, [existing])

  const { data: credit } = useQuery({
    queryKey: ['credit-status', customerId],
    queryFn: () => getCreditStatus(customerId),
    enabled: !!customerId,
  })

  const mutation = useMutation({
    mutationFn: (body) => (editMode ? updateSalesOrder(id, body) : createSalesOrder(body)),
    onSuccess: (saved) => navigate(`/sales-orders/${saved.id}`),
  })

  function updateLine(idx, patch) {
    setLines((ls) => ls.map((l, i) => (i === idx ? { ...l, ...patch } : l)))
  }
  function addLine() {
    setLines((ls) => [...ls, emptyLine()])
  }
  function removeLine(idx) {
    setLines((ls) => (ls.length > 1 ? ls.filter((_, i) => i !== idx) : ls))
  }

  const total = lines.reduce(
    (sum, l) => sum + (Number(l.orderQty) || 0) * (Number(l.unitPrice) || 0),
    0,
  )

  function onSubmit(e) {
    e.preventDefault()
    const lineBody = lines.map((l) => ({
      itemId: Number(l.itemId),
      orderQty: Number(l.orderQty),
      unitPrice: Number(l.unitPrice),
    }))
    // 수정 DTO엔 고객·견적이 없다. 영업담당은 기존값을 유지해 보낸다.
    const body = editMode
      ? { salespersonId, orderDate, lines: lineBody }
      : {
          customerId: Number(customerId),
          quotationId: quotationId ? Number(quotationId) : null,
          orderDate,
          lines: lineBody,
        }
    mutation.mutate(body)
  }

  const canSubmit =
    customerId && orderDate && lines.every((l) => l.itemId && Number(l.orderQty) > 0)

  const backTo = editMode ? `/sales-orders/${id}` : '/sales-orders'

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to={backTo} className="muted">
            ← {editMode ? '수주 상세' : '수주 목록'}
          </Link>
          <h1 style={{ marginTop: 6 }}>
            {editMode ? `수주 수정 ${existing?.number || ''}` : '수주 등록'}
          </h1>
        </div>
      </div>

      {!editMode && quotationId && (
        <p className="muted" style={{ marginTop: -6 }}>
          견적 <span className="mono">{srcQuotation?.number || `#${quotationId}`}</span>{' '}
          에서 전환 중 — 고객·라인이 자동 입력됩니다.
        </p>
      )}

      <form onSubmit={onSubmit}>
        <div className="panel">
          <div className="row">
            <div className="field">
              <label>고객 *</label>
              <select
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                style={{ width: '100%' }}
                disabled={!!quotationId || editMode}
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
              <label>수주일 *</label>
              <input
                type="date"
                value={orderDate}
                onChange={(e) => setOrderDate(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>
          </div>

          {credit && (
            <p className="muted" style={{ margin: '4px 0 0', fontSize: 13 }}>
              여신 한도 <span className="mono">{formatMoney(credit.creditLimit)}</span> ·
              가용 <span className="mono">{formatMoney(credit.remaining)}</span>
            </p>
          )}
        </div>

        <div className="section-title">수주 라인</div>
        <div className="panel">
          <table>
            <thead>
              <tr>
                <th style={{ width: '45%' }}>품목</th>
                <th className="num">수주량</th>
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
                      value={l.orderQty}
                      onChange={(e) => updateLine(idx, { orderQty: e.target.value })}
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
                    {formatMoney((Number(l.orderQty) || 0) * (Number(l.unitPrice) || 0))}
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
            {mutation.isPending ? '저장 중…' : editMode ? '수정 저장' : '수주 등록'}
          </button>
          <Link to={backTo}>
            <button type="button">취소</button>
          </Link>
        </div>
      </form>
    </div>
  )
}

import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { createInvoice } from '../api/invoices'
import { getSalesOrder } from '../api/salesOrders'
import { formatMoney } from '../domain/status'

function today() {
  return new Date().toISOString().slice(0, 10)
}

// 청구 생성 — 특정 수주(?salesOrderId=)의 라인 기반. 잔여(수주량-기청구량)가 기본값.
export default function InvoiceCreateView() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const salesOrderId = searchParams.get('salesOrderId')

  const { data: so, isLoading } = useQuery({
    queryKey: ['sales-order', salesOrderId],
    queryFn: () => getSalesOrder(salesOrderId),
    enabled: !!salesOrderId,
  })

  const [invoiceDate, setInvoiceDate] = useState(today())
  const [qtys, setQtys] = useState({})

  useEffect(() => {
    if (!so) return
    const init = {}
    for (const l of so.lines || []) {
      const remain = Number(l.orderQty) - Number(l.invoicedQty || 0)
      init[l.id] = remain > 0 ? remain : 0
    }
    setQtys(init)
  }, [so])

  const mutation = useMutation({
    mutationFn: createInvoice,
    onSuccess: (saved) => navigate(`/invoices/${saved.id}`),
  })

  function onSubmit(e) {
    e.preventDefault()
    const lines = (so.lines || [])
      .map((l) => ({ salesOrderLineId: l.id, quantity: Number(qtys[l.id]) || 0 }))
      .filter((l) => l.quantity > 0)
    mutation.mutate({ salesOrderId: Number(salesOrderId), invoiceDate, lines })
  }

  if (!salesOrderId)
    return <div className="container"><p className="error">수주가 지정되지 않았습니다.</p></div>
  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>

  // 미리보기 합계(공급가액). 세액은 서버가 계산하므로 참고용.
  const previewSubtotal = (so?.lines || []).reduce(
    (sum, l) => sum + (Number(qtys[l.id]) || 0) * Number(l.unitPrice || 0),
    0,
  )
  const anyQty = Object.values(qtys).some((q) => Number(q) > 0)
  const canSubmit = invoiceDate && anyQty

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to={`/sales-orders/${salesOrderId}`} className="muted">
            ← 수주 상세
          </Link>
          <h1 style={{ marginTop: 6 }}>
            청구 생성 <span className="mono muted">{so?.number}</span>
          </h1>
        </div>
      </div>

      <form onSubmit={onSubmit}>
        <div className="panel">
          <div className="row">
            <div className="field">
              <label>청구일 *</label>
              <input
                type="date"
                value={invoiceDate}
                onChange={(e) => setInvoiceDate(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>
          </div>
          <p className="muted" style={{ margin: 0, fontSize: 13 }}>
            고객 {so?.customerName} · 수주일 {so?.orderDate}
          </p>
        </div>

        <div className="section-title">청구 라인 (수주량 - 기청구량 = 잔여)</div>
        <div className="panel">
          <table>
            <thead>
              <tr>
                <th>품목</th>
                <th className="num">수주량</th>
                <th className="num">기청구</th>
                <th className="num">잔여</th>
                <th className="num">단가</th>
                <th className="num">이번 청구</th>
              </tr>
            </thead>
            <tbody>
              {(so?.lines || []).map((l) => {
                const remain = Number(l.orderQty) - Number(l.invoicedQty || 0)
                return (
                  <tr key={l.id}>
                    <td>
                      {l.itemName} <span className="muted mono">{l.itemCode}</span>
                    </td>
                    <td className="num mono">{formatMoney(l.orderQty)}</td>
                    <td className="num mono">{formatMoney(l.invoicedQty)}</td>
                    <td className="num mono">{formatMoney(remain)}</td>
                    <td className="num mono">{formatMoney(l.unitPrice)}</td>
                    <td className="num">
                      <input
                        type="number"
                        min="0"
                        max={remain}
                        value={qtys[l.id] ?? 0}
                        onChange={(e) => setQtys((q) => ({ ...q, [l.id]: e.target.value }))}
                        style={{ width: 90, textAlign: 'right' }}
                      />
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          <div style={{ marginTop: 12, textAlign: 'right' }}>
            공급가액(미리보기) <strong className="mono">{formatMoney(previewSubtotal)}</strong>
            <span className="muted" style={{ fontSize: 12 }}> · 세액은 저장 시 계산</span>
          </div>
        </div>

        {mutation.isError && (
          <p className="error" style={{ marginTop: 12 }}>
            {mutation.error.message}
          </p>
        )}

        <div className="actions" style={{ marginTop: 16 }}>
          <button className="primary" type="submit" disabled={!canSubmit || mutation.isPending}>
            {mutation.isPending ? '저장 중…' : '청구 생성'}
          </button>
          <Link to={`/sales-orders/${salesOrderId}`}>
            <button type="button">취소</button>
          </Link>
        </div>
      </form>
    </div>
  )
}

import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getInvoice, invoiceAction } from '../api/invoices'
import { INVOICE_STATUS, invoiceActions, ACTION_LABEL, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'

export default function InvoiceDetailView() {
  const { id } = useParams()
  const queryClient = useQueryClient()

  const { data: v, isLoading, isError, error } = useQuery({
    queryKey: ['invoice', id],
    queryFn: () => getInvoice(id),
  })

  const mutation = useMutation({
    mutationFn: (action) => invoiceAction(id, action),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', id] })
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
    },
  })

  function onAction(action) {
    if (action === 'cancel' && !window.confirm('이 청구를 취소하시겠습니까?')) return
    mutation.mutate(action)
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>

  const actions = invoiceActions(v.status)

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/invoices" className="muted">
            ← 청구 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{v.number}</span>{' '}
            <StatusBadge map={INVOICE_STATUS} status={v.status} />
          </h1>
        </div>
        {actions.length > 0 && (
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
        )}
      </div>

      {mutation.isError && <p className="error">{mutation.error.message}</p>}

      <div className="panel">
        <div className="info-grid">
          <div>
            <div className="k">수주</div>
            <div className="v">
              <Link to={`/sales-orders/${v.salesOrderId}`} className="mono">
                {v.salesOrderNumber}
              </Link>
            </div>
          </div>
          <div>
            <div className="k">청구일</div>
            <div className="v">{v.invoiceDate}</div>
          </div>
          <div>
            <div className="k">공급가액</div>
            <div className="v mono">{formatMoney(v.subtotal)}</div>
          </div>
          <div>
            <div className="k">세액</div>
            <div className="v mono">{formatMoney(v.taxAmount)}</div>
          </div>
          <div>
            <div className="k">청구액(합계)</div>
            <div className="v mono">{formatMoney(v.totalAmount)}</div>
          </div>
        </div>
      </div>

      <div className="section-title">청구 라인</div>
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>품목</th>
              <th className="num">수량</th>
              <th className="num">단가</th>
              <th className="num">금액</th>
            </tr>
          </thead>
          <tbody>
            {(v.lines || []).map((l) => (
              <tr key={l.id}>
                <td>
                  {l.itemName} <span className="muted mono">{l.itemCode}</span>
                </td>
                <td className="num mono">{formatMoney(l.quantity)}</td>
                <td className="num mono">{formatMoney(l.unitPrice)}</td>
                <td className="num mono">{formatMoney(l.lineTotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        작성 {v.createdBy} · {v.createdAt} / 수정 {v.updatedBy} · {v.updatedAt}
      </p>
    </div>
  )
}

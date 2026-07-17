import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getSalesOrder, salesOrderAction } from '../api/salesOrders'
import {
  SALES_ORDER_STATUS,
  salesOrderActions,
  canCreateDownstream,
  ACTION_LABEL,
  formatMoney,
} from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import type { SalesOrderAction } from '../types/api'

export default function SalesOrderDetailView() {
  // 라우트에 :id 가 있어 항상 존재하지만 타입상 undefined 가능 — 빈 문자열로 좁힌다.
  const { id = '' } = useParams()
  const queryClient = useQueryClient()

  const { data: o, isLoading, isError, error } = useQuery({
    queryKey: ['sales-order', id],
    queryFn: () => getSalesOrder(id),
  })

  const mutation = useMutation({
    mutationFn: (action: SalesOrderAction) => salesOrderAction(id, action),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-order', id] })
      queryClient.invalidateQueries({ queryKey: ['sales-orders'] })
    },
  })

  function onAction(action: SalesOrderAction) {
    if (action === 'cancel' && !window.confirm('이 수주를 취소하시겠습니까?')) return
    mutation.mutate(action)
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  // 로딩·에러가 아니면 데이터가 있지만 TS는 모른다 — 아래에서 그냥 쓰기 위해 좁힌다.
  if (!o) return null

  const actions = salesOrderActions(o.status)

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/sales-orders" className="muted">
            ← 수주 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{o.number}</span>{' '}
            <StatusBadge map={SALES_ORDER_STATUS} status={o.status} />
          </h1>
        </div>
        <div className="actions">
          {o.status === 'DRAFT' && (
            <Link to={`/sales-orders/${id}/edit`}>
              <button>수정</button>
            </Link>
          )}
          {canCreateDownstream(o.status) && (
            <>
              <Link to={`/deliveries/new?salesOrderId=${id}`}>
                <button>출하 생성</button>
              </Link>
              <Link to={`/invoices/new?salesOrderId=${id}`}>
                <button>청구 생성</button>
              </Link>
            </>
          )}
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
            <div className="k">고객</div>
            <div className="v">
              {o.customerName} <span className="muted mono">{o.customerCode}</span>
            </div>
          </div>
          <div>
            <div className="k">영업담당</div>
            <div className="v">{o.salespersonName || '-'}</div>
          </div>
          <div>
            <div className="k">원본 견적</div>
            <div className="v">
              {o.quotationId ? (
                <Link to={`/quotations/${o.quotationId}`} className="mono">
                  {o.quotationNumber}
                </Link>
              ) : (
                '-'
              )}
            </div>
          </div>
          <div>
            <div className="k">수주일</div>
            <div className="v">{o.orderDate}</div>
          </div>
          <div>
            <div className="k">확정일시</div>
            <div className="v">{o.confirmedAt || '-'}</div>
          </div>
          <div>
            <div className="k">합계금액</div>
            <div className="v mono">{formatMoney(o.totalAmount)}</div>
          </div>
        </div>
      </div>

      <div className="section-title">수주 라인</div>
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>품목</th>
              <th className="num">수주량</th>
              <th className="num">출하량</th>
              <th className="num">청구량</th>
              <th className="num">단가</th>
              <th className="num">금액</th>
            </tr>
          </thead>
          <tbody>
            {(o.lines || []).map((l) => (
              <tr key={l.id}>
                <td>{l.lineNo}</td>
                <td>
                  {l.itemName} <span className="muted mono">{l.itemCode}</span>
                </td>
                <td className="num mono">{formatMoney(l.orderQty)}</td>
                <td className="num mono">{formatMoney(l.shippedQty)}</td>
                <td className="num mono">{formatMoney(l.invoicedQty)}</td>
                <td className="num mono">{formatMoney(l.unitPrice)}</td>
                <td className="num mono">{formatMoney(l.lineTotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        작성 {o.createdBy} · {o.createdAt} / 수정 {o.updatedBy} · {o.updatedAt}
      </p>
    </div>
  )
}

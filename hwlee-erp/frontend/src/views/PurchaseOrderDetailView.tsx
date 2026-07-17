import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getPurchaseOrder, purchaseOrderAction } from '../api/purchaseOrders'
import {
  PURCHASE_ORDER_STATUS,
  purchaseOrderActions,
  canReceive,
  ACTION_LABEL,
  formatMoney,
} from '../domain/status'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'
import type { PurchaseOrderAction } from '../types/api'

export default function PurchaseOrderDetailView() {
  // 라우트에 :id 가 있어 항상 존재하지만 타입상 undefined 가능 — 빈 문자열로 좁힌다.
  const { id = '' } = useParams()
  const queryClient = useQueryClient()
  const { hasRole } = useAuth()
  const canWrite = hasRole('PURCHASING', 'ADMIN') // 쓰기 액션은 구매/관리자만

  const { data: po, isLoading, isError, error } = useQuery({
    queryKey: ['purchase-order', id],
    queryFn: () => getPurchaseOrder(id),
  })

  const mutation = useMutation({
    mutationFn: (action: PurchaseOrderAction) => purchaseOrderAction(id, action),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['purchase-order', id] })
      queryClient.invalidateQueries({ queryKey: ['purchase-orders'] })
    },
  })

  function onAction(action: PurchaseOrderAction) {
    if (action === 'cancel' && !window.confirm('이 발주를 취소하시겠습니까?')) return
    mutation.mutate(action)
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  // 로딩·에러가 아니면 데이터가 있지만 TS는 모른다 — 아래에서 그냥 쓰기 위해 좁힌다.
  if (!po) return null

  const actions = purchaseOrderActions(po.status)

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/purchase-orders" className="muted">
            ← 발주 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{po.number}</span>{' '}
            <StatusBadge map={PURCHASE_ORDER_STATUS} status={po.status} />
          </h1>
        </div>
        {canWrite && (
          <div className="actions">
            {po.status === 'DRAFT' && (
              <Link to={`/purchase-orders/${id}/edit`}>
                <button>수정</button>
              </Link>
            )}
            {canReceive(po.status) && (
              <Link to={`/goods-receipts/new?purchaseOrderId=${id}`}>
                <button>입고 처리</button>
              </Link>
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
        )}
      </div>

      {mutation.isError && <p className="error">{mutation.error.message}</p>}

      <div className="panel">
        <div className="info-grid">
          <div>
            <div className="k">공급처</div>
            <div className="v">
              {po.vendorName} <span className="muted mono">{po.vendorCode}</span>
            </div>
          </div>
          <div>
            <div className="k">입고 창고</div>
            <div className="v">{po.warehouseName}</div>
          </div>
          <div>
            <div className="k">발주일</div>
            <div className="v">{po.orderDate}</div>
          </div>
          <div>
            <div className="k">입고 예정일</div>
            <div className="v">{po.expectedDate || '-'}</div>
          </div>
          <div>
            <div className="k">합계금액</div>
            <div className="v mono">{formatMoney(po.totalAmount)}</div>
          </div>
          {po.remark && (
            <div style={{ gridColumn: '1 / -1' }}>
              <div className="k">비고</div>
              <div className="v">{po.remark}</div>
            </div>
          )}
        </div>
      </div>

      <div className="section-title">발주 라인</div>
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>품목</th>
              <th className="num">발주량</th>
              <th className="num">입고량</th>
              <th className="num">미입고</th>
              <th className="num">단가</th>
              <th className="num">금액</th>
            </tr>
          </thead>
          <tbody>
            {(po.lines || []).map((l) => (
              <tr key={l.id}>
                <td>
                  {l.itemName} <span className="muted mono">{l.itemCode}</span>
                </td>
                <td className="num mono">{formatMoney(l.quantity)}</td>
                <td className="num mono">{formatMoney(l.receivedQuantity)}</td>
                <td className="num mono">{formatMoney(l.openQuantity)}</td>
                <td className="num mono">{formatMoney(l.unitPrice)}</td>
                <td className="num mono">{formatMoney(l.lineTotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        작성 {po.createdBy} · {po.createdAt} / 수정 {po.updatedBy} · {po.updatedAt}
      </p>
    </div>
  )
}

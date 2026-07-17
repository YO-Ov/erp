import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getGoodsReceipt, goodsReceiptAction } from '../api/goodsReceipts'
import {
  GOODS_RECEIPT_STATUS,
  goodsReceiptActions,
  ACTION_LABEL,
  formatMoney,
} from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import type { GoodsReceiptAction } from '../types/api'

export default function GoodsReceiptDetailView() {
  // 라우트에 :id 가 있어 항상 존재하지만 타입상 undefined 가능 — 빈 문자열로 좁힌다.
  const { id = '' } = useParams()
  const queryClient = useQueryClient()

  const { data: gr, isLoading, isError, error } = useQuery({
    queryKey: ['goods-receipt', id],
    queryFn: () => getGoodsReceipt(id),
  })

  const mutation = useMutation({
    mutationFn: (action: GoodsReceiptAction) => goodsReceiptAction(id, action),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goods-receipt', id] })
      queryClient.invalidateQueries({ queryKey: ['goods-receipts'] })
      // 전기하면 발주 상태(RECEIVED)도 바뀌므로 발주 캐시도 무효화.
      queryClient.invalidateQueries({ queryKey: ['purchase-order'] })
      queryClient.invalidateQueries({ queryKey: ['purchase-orders'] })
    },
  })

  function onAction(action: GoodsReceiptAction) {
    if (action === 'post' && !window.confirm('전기하면 재고에 반영됩니다. 진행할까요?')) return
    if (action === 'cancel' && !window.confirm('이 입고를 취소하시겠습니까?')) return
    mutation.mutate(action)
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  // 로딩·에러가 아니면 데이터가 있지만 TS는 모른다 — 아래에서 그냥 쓰기 위해 좁힌다.
  if (!gr) return null

  const actions = goodsReceiptActions(gr.status)

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/goods-receipts" className="muted">
            ← 입고 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{gr.number}</span>{' '}
            <StatusBadge map={GOODS_RECEIPT_STATUS} status={gr.status} />
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
            <div className="k">공급처</div>
            <div className="v">
              {gr.vendorName} <span className="muted mono">{gr.vendorCode}</span>
            </div>
          </div>
          <div>
            <div className="k">입고 창고</div>
            <div className="v">{gr.warehouseName}</div>
          </div>
          <div>
            <div className="k">발주</div>
            <div className="v">
              {gr.purchaseOrderId ? (
                <Link to={`/purchase-orders/${gr.purchaseOrderId}`} className="mono">
                  {gr.purchaseOrderNumber}
                </Link>
              ) : (
                '-'
              )}
            </div>
          </div>
          <div>
            <div className="k">입고일</div>
            <div className="v">{gr.receiptDate}</div>
          </div>
          <div>
            <div className="k">전기일시</div>
            <div className="v">{gr.postedAt || '-'}</div>
          </div>
        </div>
      </div>

      <div className="section-title">입고 라인</div>
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>품목</th>
              <th className="num">입고수량</th>
              <th className="num">입고단가</th>
              <th className="num">금액</th>
            </tr>
          </thead>
          <tbody>
            {(gr.lines || []).map((l) => (
              <tr key={l.id}>
                <td>
                  {l.itemName} <span className="muted mono">{l.itemCode}</span>
                </td>
                <td className="num mono">{formatMoney(l.quantity)}</td>
                <td className="num mono">{formatMoney(l.unitCost)}</td>
                <td className="num mono">{formatMoney(l.lineTotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        작성 {gr.createdBy} · {gr.createdAt}
      </p>
    </div>
  )
}

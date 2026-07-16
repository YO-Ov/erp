import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getDelivery, deliveryAction } from '../api/deliveries'
import { listWarehouses } from '../api/masters'
import { DELIVERY_STATUS, deliveryActions, ACTION_LABEL, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'

export default function DeliveryDetailView() {
  const { id } = useParams()
  const queryClient = useQueryClient()

  const { data: d, isLoading, isError, error } = useQuery({
    queryKey: ['delivery', id],
    queryFn: () => getDelivery(id),
  })

  // 출하 응답엔 창고 id만 있어 이름을 별도로 매핑한다.
  const { data: warehouses = [] } = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => listWarehouses(),
  })
  const whName = Object.fromEntries(warehouses.map((w) => [w.id, w.name]))

  const mutation = useMutation({
    mutationFn: (action) => deliveryAction(id, action),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['delivery', id] })
      queryClient.invalidateQueries({ queryKey: ['deliveries'] })
    },
  })

  function onAction(action) {
    if (action === 'cancel' && !window.confirm('이 출하를 취소하시겠습니까?')) return
    mutation.mutate(action)
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>

  const actions = deliveryActions(d.status)

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/deliveries" className="muted">
            ← 출하 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{d.number}</span>{' '}
            <StatusBadge map={DELIVERY_STATUS} status={d.status} />
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
              <Link to={`/sales-orders/${d.salesOrderId}`} className="mono">
                {d.salesOrderNumber}
              </Link>
            </div>
          </div>
          <div>
            <div className="k">출고 창고</div>
            <div className="v">{whName[d.warehouseId] || d.warehouseId}</div>
          </div>
          <div>
            <div className="k">출하일</div>
            <div className="v">{d.shippedDate}</div>
          </div>
        </div>
      </div>

      <div className="section-title">출하 라인</div>
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>품목</th>
              <th className="num">출하수량</th>
            </tr>
          </thead>
          <tbody>
            {(d.lines || []).map((l) => (
              <tr key={l.id}>
                <td>
                  {l.itemName} <span className="muted mono">{l.itemCode}</span>
                </td>
                <td className="num mono">{formatMoney(l.quantity)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        작성 {d.createdBy} · {d.createdAt} / 수정 {d.updatedBy} · {d.updatedAt}
      </p>
    </div>
  )
}

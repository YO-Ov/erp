import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getProductionOrder,
  productionOrderAction,
  getMaterialAvailability,
} from '../api/productionOrders'
import {
  PRODUCTION_ORDER_STATUS,
  productionOrderActions,
  ACTION_LABEL,
  formatMoney,
} from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import type { ProductionOrderAction } from '../types/api'

/**
 * 자재 표의 한 행. 작업지시 소요자재 라인과 가용성 라인을 합친 모양이다.
 * 재고 정보(onHandQty·sufficient)는 가용성 조회가 가능한 상태(PLANNED·RELEASED)에서만 온다.
 */
interface MaterialRow {
  componentItemId: number
  componentCode: string
  componentName: string
  requiredQty: number
  onHandQty: number | null
  sufficient: boolean | null
}

export default function ProductionOrderDetailView() {
  // 라우트에 :id 가 있어 항상 존재하지만 타입상 undefined 가능 — 빈 문자열로 좁힌다.
  const { id = '' } = useParams()
  const queryClient = useQueryClient()

  const { data: po, isLoading, isError, error } = useQuery({
    queryKey: ['production-order', id],
    queryFn: () => getProductionOrder(id),
  })

  // 자재 가용성은 계획/착수 단계에서만 의미가 있다.
  const showAvailability = po && (po.status === 'PLANNED' || po.status === 'RELEASED')
  const { data: avail } = useQuery({
    queryKey: ['material-availability', id],
    queryFn: () => getMaterialAvailability(id),
    enabled: !!showAvailability,
  })

  const mutation = useMutation({
    mutationFn: (action: ProductionOrderAction) => productionOrderAction(id, action),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production-order', id] })
      queryClient.invalidateQueries({ queryKey: ['production-orders'] })
      queryClient.invalidateQueries({ queryKey: ['material-availability', id] })
    },
  })

  function onAction(action: ProductionOrderAction) {
    if (action === 'cancel' && !window.confirm('이 작업지시를 취소하시겠습니까?')) return
    if (action === 'complete' && !window.confirm('완료하면 부품이 출고되고 완제품이 입고됩니다. 진행할까요?'))
      return
    mutation.mutate(action)
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  // 로딩·에러가 아니면 데이터가 있지만 TS는 모른다 — 아래에서 그냥 쓰기 위해 좁힌다.
  if (!po) return null

  const actions = productionOrderActions(po.status)
  // MES 전송은 착수(RELEASED) 상태 + 아직 미전송일 때만.
  const canDispatch = po.status === 'RELEASED' && !po.mesWorkOrderNo

  // 자재 라인: 가용성 응답이 있으면 재고/충분여부까지, 없으면 기본 소요자재.
  // 두 응답의 라인 타입이 달라(가용성에만 onHandQty·sufficient 존재) 한 모양으로 정규화한다.
  // 재고 정보가 없는 경우를 null 로 명시해야, 표에서 빼먹지 않고 다룰 수 있다.
  const materialLines: MaterialRow[] = avail
    ? avail.lines.map((l) => ({
        componentItemId: l.componentItemId,
        componentCode: l.componentCode,
        componentName: l.componentName,
        requiredQty: l.requiredQty,
        onHandQty: l.onHandQty,
        sufficient: l.sufficient,
      }))
    : po.lines.map((l) => ({
        componentItemId: l.componentItemId,
        componentCode: l.componentCode,
        componentName: l.componentName,
        requiredQty: l.requiredQty,
        onHandQty: null,
        sufficient: null,
      }))

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/production-orders" className="muted">
            ← 작업지시 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{po.number}</span>{' '}
            <StatusBadge map={PRODUCTION_ORDER_STATUS} status={po.status} />
          </h1>
        </div>
        <div className="actions">
          {canDispatch && (
            <button
              className="primary"
              disabled={mutation.isPending}
              onClick={() => onAction('dispatch')}
            >
              {ACTION_LABEL.dispatch}
            </button>
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
            <div className="k">완제품</div>
            <div className="v">
              {po.productName} <span className="muted mono">{po.productCode}</span>
            </div>
          </div>
          <div>
            <div className="k">생산 수량</div>
            <div className="v mono">{formatMoney(po.quantity)}</div>
          </div>
          <div>
            <div className="k">입고 창고</div>
            <div className="v">{po.warehouseName}</div>
          </div>
          <div>
            <div className="k">지시일 / 납기일</div>
            <div className="v">
              {po.orderDate} / {po.dueDate || '-'}
            </div>
          </div>
          <div>
            <div className="k">MES 작업번호</div>
            <div className="v mono">
              {po.mesWorkOrderNo || '미전송'}
              {po.mesDispatchedAt && (
                <span className="muted" style={{ fontSize: 12 }}>
                  {' '}
                  ({po.mesDispatchedAt})
                </span>
              )}
            </div>
          </div>
          <div>
            <div className="k">완료일시</div>
            <div className="v">{po.completedAt || '-'}</div>
          </div>
        </div>
      </div>

      <div className="section-title">
        소요 자재 (BOM 전개)
        {avail && (
          <span
            className={`badge tone-${avail.producible ? 'active' : 'danger'}`}
            style={{ marginLeft: 8 }}
          >
            {avail.producible ? '생산 가능' : '자재 부족'}
          </span>
        )}
      </div>
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>자재</th>
              <th className="num">소요량</th>
              {avail && <th className="num">현재고</th>}
              {avail && <th className="num">충분</th>}
            </tr>
          </thead>
          <tbody>
            {materialLines.map((l) => (
              <tr key={l.componentItemId}>
                <td>
                  {l.componentName} <span className="muted mono">{l.componentCode}</span>
                </td>
                <td className="num mono">{formatMoney(l.requiredQty)}</td>
                {avail && <td className="num mono">{formatMoney(l.onHandQty)}</td>}
                {avail && (
                  <td className="num">
                    {l.sufficient ? (
                      <span style={{ color: 'var(--tone-active)' }}>충분</span>
                    ) : (
                      <span className="error">부족</span>
                    )}
                  </td>
                )}
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

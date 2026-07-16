import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { createDelivery } from '../api/deliveries'
import { getSalesOrder } from '../api/salesOrders'
import { listWarehouses } from '../api/masters'
import { formatMoney } from '../domain/status'

function today() {
  return new Date().toISOString().slice(0, 10)
}

// 출하 생성 — 특정 수주(?salesOrderId=)의 라인 기반. 잔여 수량이 기본값.
export default function DeliveryCreateView() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const salesOrderId = searchParams.get('salesOrderId')

  const { data: so, isLoading } = useQuery({
    queryKey: ['sales-order', salesOrderId],
    queryFn: () => getSalesOrder(salesOrderId),
    enabled: !!salesOrderId,
  })
  const { data: warehouses = [] } = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => listWarehouses(),
  })

  const [warehouseId, setWarehouseId] = useState('')
  const [shippedDate, setShippedDate] = useState(today())
  // 라인별 이번 출하 수량. { [salesOrderLineId]: qty }
  const [qtys, setQtys] = useState({})

  // 수주 로드 후 각 라인의 잔여수량(수주량-기출하량)을 기본값으로.
  useEffect(() => {
    if (!so) return
    const init = {}
    for (const l of so.lines || []) {
      const remain = Number(l.orderQty) - Number(l.shippedQty || 0)
      init[l.id] = remain > 0 ? remain : 0
    }
    setQtys(init)
  }, [so])

  const mutation = useMutation({
    mutationFn: createDelivery,
    onSuccess: (saved) => navigate(`/deliveries/${saved.id}`),
  })

  function onSubmit(e) {
    e.preventDefault()
    const lines = (so.lines || [])
      .map((l) => ({ salesOrderLineId: l.id, quantity: Number(qtys[l.id]) || 0 }))
      .filter((l) => l.quantity > 0)
    mutation.mutate({
      salesOrderId: Number(salesOrderId),
      warehouseId: Number(warehouseId),
      shippedDate,
      lines,
    })
  }

  if (!salesOrderId)
    return <div className="container"><p className="error">수주가 지정되지 않았습니다.</p></div>
  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>

  const anyQty = Object.values(qtys).some((q) => Number(q) > 0)
  const canSubmit = warehouseId && shippedDate && anyQty

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to={`/sales-orders/${salesOrderId}`} className="muted">
            ← 수주 상세
          </Link>
          <h1 style={{ marginTop: 6 }}>
            출하 생성 <span className="mono muted">{so?.number}</span>
          </h1>
        </div>
      </div>

      <form onSubmit={onSubmit}>
        <div className="panel">
          <div className="row">
            <div className="field">
              <label>출고 창고 *</label>
              <select
                value={warehouseId}
                onChange={(e) => setWarehouseId(e.target.value)}
                style={{ width: '100%' }}
                required
              >
                <option value="">창고 선택</option>
                {warehouses.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name} ({w.code})
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>출하일 *</label>
              <input
                type="date"
                value={shippedDate}
                onChange={(e) => setShippedDate(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>
          </div>
          <p className="muted" style={{ margin: 0, fontSize: 13 }}>
            고객 {so?.customerName} · 수주일 {so?.orderDate}
          </p>
        </div>

        <div className="section-title">출하 라인 (수주량 - 기출하량 = 잔여)</div>
        <div className="panel">
          <table>
            <thead>
              <tr>
                <th>품목</th>
                <th className="num">수주량</th>
                <th className="num">기출하</th>
                <th className="num">잔여</th>
                <th className="num">이번 출하</th>
              </tr>
            </thead>
            <tbody>
              {(so?.lines || []).map((l) => {
                const remain = Number(l.orderQty) - Number(l.shippedQty || 0)
                return (
                  <tr key={l.id}>
                    <td>
                      {l.itemName} <span className="muted mono">{l.itemCode}</span>
                    </td>
                    <td className="num mono">{formatMoney(l.orderQty)}</td>
                    <td className="num mono">{formatMoney(l.shippedQty)}</td>
                    <td className="num mono">{formatMoney(remain)}</td>
                    <td className="num">
                      <input
                        type="number"
                        min="0"
                        max={remain}
                        value={qtys[l.id] ?? 0}
                        onChange={(e) =>
                          setQtys((q) => ({ ...q, [l.id]: e.target.value }))
                        }
                        style={{ width: 90, textAlign: 'right' }}
                      />
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        {mutation.isError && (
          <p className="error" style={{ marginTop: 12 }}>
            {mutation.error.message}
          </p>
        )}

        <div className="actions" style={{ marginTop: 16 }}>
          <button className="primary" type="submit" disabled={!canSubmit || mutation.isPending}>
            {mutation.isPending ? '저장 중…' : '출하 생성'}
          </button>
          <Link to={`/sales-orders/${salesOrderId}`}>
            <button type="button">취소</button>
          </Link>
        </div>
      </form>
    </div>
  )
}

import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { createProductionOrder } from '../api/productionOrders'
import { listItems, listWarehouses } from '../api/masters'

function today() {
  return new Date().toISOString().slice(0, 10)
}

// 생산 작업지시 생성 — 완제품·수량·창고·일정만 입력하면
// 서버가 그 완제품의 BOM 을 전개해 소요 자재 라인을 자동 생성한다(BOM 없으면 409).
export default function ProductionOrderCreateView() {
  const navigate = useNavigate()

  const { data: items = [] } = useQuery({ queryKey: ['items'], queryFn: () => listItems() })
  const { data: warehouses = [] } = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => listWarehouses(),
  })

  const [productItemId, setProductItemId] = useState('')
  const [warehouseId, setWarehouseId] = useState('')
  const [quantity, setQuantity] = useState<number | string>(1)
  const [orderDate, setOrderDate] = useState(today())
  const [dueDate, setDueDate] = useState('')

  const mutation = useMutation({
    mutationFn: createProductionOrder,
    onSuccess: (saved) => navigate(`/production-orders/${saved.id}`),
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    mutation.mutate({
      productItemId: Number(productItemId),
      warehouseId: Number(warehouseId),
      quantity: Number(quantity),
      orderDate,
      dueDate: dueDate || null,
    })
  }

  const canSubmit = productItemId && warehouseId && Number(quantity) > 0 && orderDate

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/production-orders" className="muted">
            ← 작업지시 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>작업지시 생성</h1>
        </div>
      </div>

      <form onSubmit={onSubmit}>
        <div className="panel">
          <div className="row">
            <div className="field">
              <label>완제품 *</label>
              <select
                value={productItemId}
                onChange={(e) => setProductItemId(e.target.value)}
                style={{ width: '100%' }}
                required
              >
                <option value="">완제품 선택</option>
                {items.map((it) => (
                  <option key={it.id} value={it.id}>
                    {it.name} ({it.code})
                  </option>
                ))}
              </select>
              <span className="muted" style={{ fontSize: 12 }}>
                BOM 이 등록된 완제품만 지시할 수 있습니다.
              </span>
            </div>
            <div className="field">
              <label>입고 창고 *</label>
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
          </div>
          <div className="row">
            <div className="field">
              <label>생산 수량 *</label>
              <input
                type="number"
                min="1"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>
            <div className="field">
              <label>지시일 *</label>
              <input
                type="date"
                value={orderDate}
                onChange={(e) => setOrderDate(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>
            <div className="field">
              <label>납기일</label>
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
          </div>
        </div>

        {mutation.isError && (
          <p className="error" style={{ marginTop: 12 }}>
            {mutation.error.message}
          </p>
        )}

        <div className="actions" style={{ marginTop: 16 }}>
          <button className="primary" type="submit" disabled={!canSubmit || mutation.isPending}>
            {mutation.isPending ? '저장 중…' : '작업지시 생성'}
          </button>
          <Link to="/production-orders">
            <button type="button">취소</button>
          </Link>
        </div>
      </form>
    </div>
  )
}

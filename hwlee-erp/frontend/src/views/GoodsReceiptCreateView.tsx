import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { createGoodsReceipt } from '../api/goodsReceipts'
import { getPurchaseOrder } from '../api/purchaseOrders'
import { formatMoney } from '../domain/status'
import type { GoodsReceiptCreateRequest } from '../types/api'

/** 발주 라인 하나에 대한 입고 입력값. 수량·단가는 입력 중 문자열이 될 수 있다. */
interface ReceiptRow {
  itemId: number
  quantity: number | string
  unitCost: number | string
}

/** 발주 라인 id → 입력값. */
type ReceiptRows = Record<number, ReceiptRow>

function today() {
  return new Date().toISOString().slice(0, 10)
}

// 입고 생성 — 특정 발주(?purchaseOrderId=)의 미입고 라인 기반.
// 공급처·창고는 발주에서 가져와 고정, 수량은 미입고량(openQuantity)이 기본.
export default function GoodsReceiptCreateView() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const purchaseOrderId = searchParams.get('purchaseOrderId')

  const { data: po, isLoading } = useQuery({
    // enabled 로 막지만 TS는 모르므로 빈 문자열로 좁힌다(그 경우 호출되지 않는다).
    queryKey: ['purchase-order', purchaseOrderId],
    queryFn: () => getPurchaseOrder(purchaseOrderId ?? ''),
    enabled: !!purchaseOrderId,
  })

  const [receiptDate, setReceiptDate] = useState(today())
  const [rows, setRows] = useState<ReceiptRows>({})

  useEffect(() => {
    if (!po) return
    const init: ReceiptRows = {}
    for (const l of po.lines || []) {
      const open = Number(l.openQuantity ?? 0)
      init[l.id] = {
        itemId: l.itemId,
        quantity: open > 0 ? open : 0,
        unitCost: l.unitPrice, // 입고단가 기본 = 발주단가
      }
    }
    setRows(init)
  }, [po])

  const mutation = useMutation({
    mutationFn: createGoodsReceipt,
    onSuccess: (saved) => navigate(`/goods-receipts/${saved.id}`),
  })

  function setRow(lineId: number, patch: Partial<ReceiptRow>) {
    setRows((r) => ({ ...r, [lineId]: { ...r[lineId], ...patch } }))
  }

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!po) return
    const lines = (po.lines || [])
      .map((l) => ({
        itemId: rows[l.id]?.itemId ?? l.itemId,
        quantity: Number(rows[l.id]?.quantity) || 0,
        unitCost: Number(rows[l.id]?.unitCost) || 0,
      }))
      .filter((l) => l.quantity > 0)
    const body: GoodsReceiptCreateRequest = {
      vendorId: po.vendorId,
      warehouseId: po.warehouseId,
      receiptDate,
      purchaseOrderId: Number(purchaseOrderId),
      lines,
    }
    mutation.mutate(body)
  }

  if (!purchaseOrderId)
    return <div className="container"><p className="error">발주가 지정되지 않았습니다.</p></div>
  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (!po) return null

  const anyQty = Object.values(rows).some((r) => Number(r.quantity) > 0)
  const canSubmit = receiptDate && anyQty

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to={`/purchase-orders/${purchaseOrderId}`} className="muted">
            ← 발주 상세
          </Link>
          <h1 style={{ marginTop: 6 }}>
            입고 처리 <span className="mono muted">{po?.number}</span>
          </h1>
        </div>
      </div>

      <form onSubmit={onSubmit}>
        <div className="panel">
          <div className="row">
            <div className="field">
              <label>입고일 *</label>
              <input
                type="date"
                value={receiptDate}
                onChange={(e) => setReceiptDate(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>
          </div>
          <p className="muted" style={{ margin: 0, fontSize: 13 }}>
            공급처 {po?.vendorName} · 입고창고 {po?.warehouseName}
          </p>
        </div>

        <div className="section-title">입고 라인 (발주량 - 입고량 = 미입고)</div>
        <div className="panel">
          <table>
            <thead>
              <tr>
                <th>품목</th>
                <th className="num">발주량</th>
                <th className="num">기입고</th>
                <th className="num">미입고</th>
                <th className="num">입고수량</th>
                <th className="num">입고단가</th>
              </tr>
            </thead>
            <tbody>
              {(po?.lines || []).map((l) => {
                const open = Number(l.openQuantity ?? 0)
                return (
                  <tr key={l.id}>
                    <td>
                      {l.itemName} <span className="muted mono">{l.itemCode}</span>
                    </td>
                    <td className="num mono">{formatMoney(l.quantity)}</td>
                    <td className="num mono">{formatMoney(l.receivedQuantity)}</td>
                    <td className="num mono">{formatMoney(open)}</td>
                    <td className="num">
                      <input
                        type="number"
                        min="0"
                        max={open}
                        value={rows[l.id]?.quantity ?? 0}
                        onChange={(e) => setRow(l.id, { quantity: e.target.value })}
                        style={{ width: 90, textAlign: 'right' }}
                      />
                    </td>
                    <td className="num">
                      <input
                        type="number"
                        min="0"
                        value={rows[l.id]?.unitCost ?? 0}
                        onChange={(e) => setRow(l.id, { unitCost: e.target.value })}
                        style={{ width: 110, textAlign: 'right' }}
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
            {mutation.isPending ? '저장 중…' : '입고 생성'}
          </button>
          <Link to={`/purchase-orders/${purchaseOrderId}`}>
            <button type="button">취소</button>
          </Link>
        </div>
      </form>
    </div>
  )
}

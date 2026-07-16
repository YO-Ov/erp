import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  createPurchaseOrder,
  getPurchaseOrder,
  updatePurchaseOrder,
} from '../api/purchaseOrders'
import { listVendors, listWarehouses, listVendorItems } from '../api/masters'
import { formatMoney } from '../domain/status'

function today() {
  return new Date().toISOString().slice(0, 10)
}
const emptyLine = () => ({ itemId: '', quantity: 1, unitPrice: 0 })

// 발주 작성/수정 겸용.
//  - /purchase-orders/new       → 신규(POST)
//  - /purchase-orders/:id/edit  → 수정(PUT, DRAFT만). 공급처·창고도 수정 가능(update DTO 포함).
export default function PurchaseOrderCreateView() {
  const navigate = useNavigate()
  const { id } = useParams()
  const editMode = !!id

  const { data: vendors = [] } = useQuery({ queryKey: ['vendors'], queryFn: () => listVendors() })
  const { data: warehouses = [] } = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => listWarehouses(),
  })

  const { data: existing } = useQuery({
    queryKey: ['purchase-order', id],
    queryFn: () => getPurchaseOrder(id),
    enabled: editMode,
  })

  const [vendorId, setVendorId] = useState('')
  const [warehouseId, setWarehouseId] = useState('')
  const [orderDate, setOrderDate] = useState(today())
  const [expectedDate, setExpectedDate] = useState('')
  const [remark, setRemark] = useState('')
  const [lines, setLines] = useState([emptyLine()])

  // 선택된 공급처의 취급품목만 발주할 수 있다(구매정보레코드 제약).
  const { data: vendorItems = [] } = useQuery({
    queryKey: ['vendor-items', vendorId],
    queryFn: () => listVendorItems(vendorId),
    enabled: !!vendorId,
  })

  useEffect(() => {
    if (!existing) return
    setVendorId(String(existing.vendorId))
    setWarehouseId(String(existing.warehouseId))
    setOrderDate(existing.orderDate)
    setExpectedDate(existing.expectedDate || '')
    setRemark(existing.remark || '')
    setLines(
      (existing.lines || []).map((l) => ({
        itemId: String(l.itemId),
        quantity: l.quantity,
        unitPrice: l.unitPrice,
      })),
    )
  }, [existing])

  const mutation = useMutation({
    mutationFn: (body) =>
      editMode ? updatePurchaseOrder(id, body) : createPurchaseOrder(body),
    onSuccess: (saved) => navigate(`/purchase-orders/${saved.id}`),
  })

  function updateLine(idx, patch) {
    setLines((ls) => ls.map((l, i) => (i === idx ? { ...l, ...patch } : l)))
  }
  // 공급처가 바뀌면 취급품목이 달라지므로 라인을 초기화한다.
  function onVendorChange(newVendorId) {
    setVendorId(newVendorId)
    setLines([emptyLine()])
  }
  // 품목을 고르면 그 공급처의 공급단가를 기본 단가로 채운다.
  function onItemChange(idx, itemId) {
    const vi = vendorItems.find((v) => String(v.itemId) === itemId)
    updateLine(idx, { itemId, unitPrice: vi ? vi.supplyPrice : 0 })
  }
  function addLine() {
    setLines((ls) => [...ls, emptyLine()])
  }
  function removeLine(idx) {
    setLines((ls) => (ls.length > 1 ? ls.filter((_, i) => i !== idx) : ls))
  }

  const total = lines.reduce(
    (sum, l) => sum + (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0),
    0,
  )

  function onSubmit(e) {
    e.preventDefault()
    const body = {
      vendorId: Number(vendorId),
      warehouseId: Number(warehouseId),
      orderDate,
      expectedDate: expectedDate || null,
      remark: remark || null,
      lines: lines.map((l) => ({
        itemId: Number(l.itemId),
        quantity: Number(l.quantity),
        unitPrice: Number(l.unitPrice),
      })),
    }
    mutation.mutate(body)
  }

  const canSubmit =
    vendorId &&
    warehouseId &&
    orderDate &&
    lines.every((l) => l.itemId && Number(l.quantity) > 0)

  const backTo = editMode ? `/purchase-orders/${id}` : '/purchase-orders'

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to={backTo} className="muted">
            ← {editMode ? '발주 상세' : '발주 목록'}
          </Link>
          <h1 style={{ marginTop: 6 }}>
            {editMode ? `발주 수정 ${existing?.number || ''}` : '발주 작성'}
          </h1>
        </div>
      </div>

      <form onSubmit={onSubmit}>
        <div className="panel">
          <div className="row">
            <div className="field">
              <label>공급처 *</label>
              <select
                value={vendorId}
                onChange={(e) => onVendorChange(e.target.value)}
                style={{ width: '100%' }}
                required
              >
                <option value="">공급처 선택</option>
                {vendors.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.name} ({v.code})
                  </option>
                ))}
              </select>
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
              <label>발주일 *</label>
              <input
                type="date"
                value={orderDate}
                onChange={(e) => setOrderDate(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>
            <div className="field">
              <label>입고 예정일</label>
              <input
                type="date"
                value={expectedDate}
                onChange={(e) => setExpectedDate(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
          </div>
          <div className="field">
            <label>비고</label>
            <input
              type="text"
              value={remark}
              maxLength={500}
              onChange={(e) => setRemark(e.target.value)}
              placeholder="특이사항(선택)"
              style={{ width: '100%' }}
            />
          </div>
        </div>

        <div className="section-title">발주 라인</div>
        <div className="panel">
          <table>
            <thead>
              <tr>
                <th style={{ width: '45%' }}>품목</th>
                <th className="num">수량</th>
                <th className="num">단가</th>
                <th className="num">금액</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {lines.map((l, idx) => (
                <tr key={idx}>
                  <td>
                    <select
                      value={l.itemId}
                      onChange={(e) => onItemChange(idx, e.target.value)}
                      style={{ width: '100%' }}
                      disabled={!vendorId}
                    >
                      <option value="">
                        {vendorId ? '품목 선택' : '공급처 먼저 선택'}
                      </option>
                      {vendorItems.map((vi) => (
                        <option key={vi.itemId} value={vi.itemId}>
                          {vi.itemName} ({vi.itemCode})
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="num">
                    <input
                      type="number"
                      min="1"
                      value={l.quantity}
                      onChange={(e) => updateLine(idx, { quantity: e.target.value })}
                      style={{ width: 90, textAlign: 'right' }}
                    />
                  </td>
                  <td className="num">
                    <input
                      type="number"
                      min="0"
                      value={l.unitPrice}
                      onChange={(e) => updateLine(idx, { unitPrice: e.target.value })}
                      style={{ width: 120, textAlign: 'right' }}
                    />
                  </td>
                  <td className="num mono">
                    {formatMoney((Number(l.quantity) || 0) * (Number(l.unitPrice) || 0))}
                  </td>
                  <td>
                    <button
                      type="button"
                      className="sm danger"
                      onClick={() => removeLine(idx)}
                      disabled={lines.length <= 1}
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between' }}>
            <button type="button" className="sm" onClick={addLine}>
              + 라인 추가
            </button>
            <div>
              합계 <strong className="mono">{formatMoney(total)}</strong>
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
            {mutation.isPending ? '저장 중…' : editMode ? '수정 저장' : '발주 생성'}
          </button>
          <Link to={backTo}>
            <button type="button">취소</button>
          </Link>
        </div>
      </form>
    </div>
  )
}

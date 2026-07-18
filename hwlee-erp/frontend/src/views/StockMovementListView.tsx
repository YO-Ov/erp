import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchStockMovements } from '../api/stocks'
import { listItems, listWarehouses } from '../api/masters'
import { MOVEMENT_REASON, movementTone, formatMoney } from '../domain/status'
import type { MovementReason } from '../types/api'

const PAGE_SIZE = 15

// 이동이력 배지 색 tone → CSS 클래스(입고=파랑, 출고=주황).
const TONE_STYLE: Record<'done' | 'warn', { bg: string; color: string; border: string }> = {
  done: { bg: 'rgba(56,189,248,0.15)', color: '#7dd3fc', border: '#38bdf8' },
  warn: { bg: 'rgba(245,158,11,0.15)', color: '#fcd34d', border: '#f59e0b' },
}

// 재고 이동이력 조회 — 읽기 전용. 입고/출고/생산 트랜잭션이 남긴 원장을 본다.
// 부호(qtyDelta)가 방향을 나타낸다: + 입고 / − 출고.
export default function StockMovementListView() {
  const [itemId, setItemId] = useState<number | ''>('')
  const [warehouseId, setWarehouseId] = useState<number | ''>('')
  const [reason, setReason] = useState<MovementReason | ''>('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)

  const { data: items = [] } = useQuery({ queryKey: ['items'], queryFn: () => listItems() })
  const { data: warehouses = [] } = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => listWarehouses(),
  })

  const params = {
    page,
    size: PAGE_SIZE,
    sort: 'id,desc',
    ...(itemId !== '' && { itemId }),
    ...(warehouseId !== '' && { warehouseId }),
    ...(reason !== '' && { reason }),
    ...(dateFrom && { dateFrom }),
    ...(dateTo && { dateTo }),
  }
  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['stock-movements', params],
    queryFn: () => searchStockMovements(params),
    placeholderData: keepPreviousData,
  })

  const rows = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  function resetToFirstPage<T>(setter: (v: T) => void) {
    return (v: T) => {
      setter(v)
      setPage(0)
    }
  }

  return (
    <div className="container">
      <div className="page-head">
        <h1>재고 이동이력</h1>
      </div>

      <div className="toolbar">
        <div>
          <label>품목</label>
          <select
            value={itemId}
            onChange={(e) =>
              resetToFirstPage(setItemId)(e.target.value === '' ? '' : Number(e.target.value))
            }
          >
            <option value="">전체</option>
            {items.map((it) => (
              <option key={it.id} value={it.id}>
                {it.name} ({it.code})
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>창고</label>
          <select
            value={warehouseId}
            onChange={(e) =>
              resetToFirstPage(setWarehouseId)(e.target.value === '' ? '' : Number(e.target.value))
            }
          >
            <option value="">전체</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>사유</label>
          <select
            value={reason}
            onChange={(e) =>
              resetToFirstPage(setReason)(e.target.value as MovementReason | '')
            }
          >
            <option value="">전체</option>
            {Object.entries(MOVEMENT_REASON).map(([code, label]) => (
              <option key={code} value={code}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>이동일 From</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => resetToFirstPage(setDateFrom)(e.target.value)}
          />
        </div>
        <div>
          <label>이동일 To</label>
          <input
            type="date"
            value={dateTo}
            onChange={(e) => resetToFirstPage(setDateTo)(e.target.value)}
          />
        </div>
      </div>

      <div className="panel">
        {isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : rows.length === 0 ? (
          <p className="muted">조건에 맞는 이동이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>이동일시</th>
                <th>사유</th>
                <th>품목</th>
                <th>창고</th>
                <th className="num">수량변동</th>
                <th className="num">단가</th>
                <th>출처</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((m) => {
                const s = TONE_STYLE[movementTone(m.reason)]
                const inbound = m.qtyDelta > 0
                return (
                  <tr key={m.id}>
                    <td className="muted">
                      {(m.movedAt || '').replace('T', ' ').slice(0, 16)}
                    </td>
                    <td>
                      <span
                        className="badge"
                        style={{ background: s.bg, color: s.color, borderColor: s.border }}
                      >
                        {MOVEMENT_REASON[m.reason]}
                      </span>
                    </td>
                    <td>
                      {m.itemName} <span className="muted mono">{m.itemCode}</span>
                    </td>
                    <td>{m.warehouseCode}</td>
                    {/* 부호를 색으로도 강조 — 입고는 +파랑, 출고는 −주황. */}
                    <td className="num mono" style={{ color: s.color }}>
                      {inbound ? '+' : ''}
                      {formatMoney(m.qtyDelta)}
                    </td>
                    <td className="num mono">{formatMoney(m.unitCost)}</td>
                    <td className="muted mono">
                      {m.refType ? `${m.refType}#${m.refId}` : '-'}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}

        {totalPages > 1 && (
          <div className="pager">
            <button className="sm" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
              이전
            </button>
            <span className="muted">
              {page + 1} / {totalPages} · 총 {totalElements}건
              {isFetching && ' · 갱신중…'}
            </span>
            <button
              className="sm"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              다음
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchStocks } from '../api/stocks'
import { listItems, listWarehouses } from '../api/masters'
import { formatMoney } from '../domain/status'

const PAGE_SIZE = 15

// 현재고 조회 — 읽기 전용. 품목·창고로 필터, 재고 있는 것만 토글.
// 재고 변경은 입고/출고/조정 트랜잭션이 하고 여기선 결과만 본다.
export default function StockListView() {
  const [itemId, setItemId] = useState<number | ''>('')
  const [warehouseId, setWarehouseId] = useState<number | ''>('')
  // 기본은 재고 있는 것만(qtyGt=0). 끄면 0 재고 행까지 다 본다.
  const [inStockOnly, setInStockOnly] = useState(true)
  const [page, setPage] = useState(0)

  const { data: items = [] } = useQuery({ queryKey: ['items'], queryFn: () => listItems() })
  const { data: warehouses = [] } = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => listWarehouses(),
  })

  const params = {
    page,
    size: PAGE_SIZE,
    sort: 'qtyOnHand,desc',
    ...(itemId !== '' && { itemId }),
    ...(warehouseId !== '' && { warehouseId }),
    ...(inStockOnly && { qtyGt: 0 }),
  }
  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['stocks', params],
    queryFn: () => searchStocks(params),
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
        <h1>현재고</h1>
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
          <label>
            <input
              type="checkbox"
              checked={inStockOnly}
              onChange={(e) => resetToFirstPage(setInStockOnly)(e.target.checked)}
              style={{ marginRight: 6 }}
            />
            재고 있는 것만
          </label>
        </div>
      </div>

      <div className="panel">
        {isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : rows.length === 0 ? (
          <p className="muted">조건에 맞는 재고가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>품목</th>
                <th>창고</th>
                <th className="num">현재고</th>
                <th className="num">평균단가</th>
                <th className="num">재고금액</th>
                <th>갱신일시</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((s) => (
                <tr key={s.id}>
                  <td>
                    {s.itemName} <span className="muted mono">{s.itemCode}</span>
                  </td>
                  <td>{s.warehouseName}</td>
                  <td className="num mono">{formatMoney(s.qtyOnHand)}</td>
                  <td className="num mono">{formatMoney(s.averageCost)}</td>
                  {/* 재고금액 = 수량 × 평균단가. 서버엔 없어 프론트가 계산한다. */}
                  <td className="num mono">{formatMoney(s.qtyOnHand * s.averageCost)}</td>
                  <td className="muted">{(s.updatedAt || '').replace('T', ' ').slice(0, 16)}</td>
                </tr>
              ))}
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

import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchProductionOrders } from '../api/productionOrders'
import { PRODUCTION_ORDER_STATUS, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import type { ProductionOrderStatus } from '../types/api'

const PAGE_SIZE = 10

export default function ProductionOrderListView() {
  // '' = 전체(필터 없음). 서버 enum 과 빈 문자열만 허용해 오타를 막는다.
  const [status, setStatus] = useState<ProductionOrderStatus | ''>('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)

  const params = {
    page,
    size: PAGE_SIZE,
    sort: 'id,desc',
    ...(status && { status }),
    ...(dateFrom && { dateFrom }),
    ...(dateTo && { dateTo }),
  }
  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['production-orders', params],
    queryFn: () => searchProductionOrders(params),
    placeholderData: keepPreviousData,
  })

  const rows = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  // 필터를 바꾸면 첫 페이지로 되돌린다(3페이지 보다가 조건 바꾸면 빈 화면이 되는 걸 막는다).
  function resetToFirstPage<T>(setter: (v: T) => void) {
    return (v: T) => {
      setter(v)
      setPage(0)
    }
  }

  return (
    <div className="container">
      <div className="page-head">
        <h1>생산 작업지시</h1>
        <Link to="/production-orders/new">
          <button className="primary">+ 작업지시 생성</button>
        </Link>
      </div>

      <div className="toolbar">
        <div>
          <label>상태</label>
          <select
            value={status}
            onChange={(e) => resetToFirstPage(setStatus)(e.target.value as ProductionOrderStatus | '')}
          >
            <option value="">전체</option>
            {Object.entries(PRODUCTION_ORDER_STATUS).map(([code, meta]) => (
              <option key={code} value={code}>
                {meta.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>지시일 From</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => resetToFirstPage(setDateFrom)(e.target.value)}
          />
        </div>
        <div>
          <label>지시일 To</label>
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
          <p className="muted">조건에 맞는 작업지시가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>지시번호</th>
                <th>완제품</th>
                <th className="num">수량</th>
                <th>상태</th>
                <th>지시일</th>
                <th>납기일</th>
                <th>MES</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((po) => (
                <tr key={po.id}>
                  <td>
                    <Link to={`/production-orders/${po.id}`} className="mono">
                      {po.number}
                    </Link>
                  </td>
                  <td>
                    {po.productName}
                    <span className="muted mono" style={{ marginLeft: 6 }}>
                      {po.productCode}
                    </span>
                  </td>
                  <td className="num mono">{formatMoney(po.quantity)}</td>
                  <td>
                    <StatusBadge map={PRODUCTION_ORDER_STATUS} status={po.status} />
                  </td>
                  <td>{po.orderDate}</td>
                  <td>{po.dueDate || '-'}</td>
                  <td className="mono">{po.mesWorkOrderNo || '-'}</td>
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

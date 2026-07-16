import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchSalesOrders } from '../api/salesOrders'
import { SALES_ORDER_STATUS, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'

const PAGE_SIZE = 10

export default function SalesOrderListView() {
  const [status, setStatus] = useState('')
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
    queryKey: ['sales-orders', params],
    queryFn: () => searchSalesOrders(params),
    placeholderData: keepPreviousData,
  })

  const rows = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  function resetToFirstPage(setter) {
    return (v) => {
      setter(v)
      setPage(0)
    }
  }

  return (
    <div className="container">
      <div className="page-head">
        <h1>수주</h1>
        <Link to="/sales-orders/new">
          <button className="primary">+ 수주 등록</button>
        </Link>
      </div>

      <div className="toolbar">
        <div>
          <label>상태</label>
          <select
            value={status}
            onChange={(e) => resetToFirstPage(setStatus)(e.target.value)}
          >
            <option value="">전체</option>
            {Object.entries(SALES_ORDER_STATUS).map(([code, meta]) => (
              <option key={code} value={code}>
                {meta.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>수주일 From</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => resetToFirstPage(setDateFrom)(e.target.value)}
          />
        </div>
        <div>
          <label>수주일 To</label>
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
          <p className="muted">조건에 맞는 수주가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>수주번호</th>
                <th>고객</th>
                <th>담당</th>
                <th>상태</th>
                <th>수주일</th>
                <th className="num">금액</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((o) => (
                <tr key={o.id}>
                  <td>
                    <Link to={`/sales-orders/${o.id}`} className="mono">
                      {o.number}
                    </Link>
                  </td>
                  <td>
                    {o.customerName}
                    <span className="muted mono" style={{ marginLeft: 6 }}>
                      {o.customerCode}
                    </span>
                  </td>
                  <td>{o.salespersonName || '-'}</td>
                  <td>
                    <StatusBadge map={SALES_ORDER_STATUS} status={o.status} />
                  </td>
                  <td>{o.orderDate}</td>
                  <td className="num mono">{formatMoney(o.totalAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {totalPages > 1 && (
          <div className="pager">
            <button
              className="sm"
              disabled={page <= 0}
              onClick={() => setPage((p) => p - 1)}
            >
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

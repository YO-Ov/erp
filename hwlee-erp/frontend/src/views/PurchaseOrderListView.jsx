import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchPurchaseOrders } from '../api/purchaseOrders'
import { PURCHASE_ORDER_STATUS, formatMoney } from '../domain/status'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'

const PAGE_SIZE = 10

export default function PurchaseOrderListView() {
  const { hasRole } = useAuth()
  const canWrite = hasRole('PURCHASING', 'ADMIN') // 발주 작성은 구매/관리자만

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
    queryKey: ['purchase-orders', params],
    queryFn: () => searchPurchaseOrders(params),
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
        <h1>발주</h1>
        {canWrite && (
          <Link to="/purchase-orders/new">
            <button className="primary">+ 발주 작성</button>
          </Link>
        )}
      </div>

      <div className="toolbar">
        <div>
          <label>상태</label>
          <select
            value={status}
            onChange={(e) => resetToFirstPage(setStatus)(e.target.value)}
          >
            <option value="">전체</option>
            {Object.entries(PURCHASE_ORDER_STATUS).map(([code, meta]) => (
              <option key={code} value={code}>
                {meta.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>발주일 From</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => resetToFirstPage(setDateFrom)(e.target.value)}
          />
        </div>
        <div>
          <label>발주일 To</label>
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
          <p className="muted">조건에 맞는 발주가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>발주번호</th>
                <th>공급처</th>
                <th>입고창고</th>
                <th>상태</th>
                <th>발주일</th>
                <th className="num">금액</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((po) => (
                <tr key={po.id}>
                  <td>
                    <Link to={`/purchase-orders/${po.id}`} className="mono">
                      {po.number}
                    </Link>
                  </td>
                  <td>
                    {po.vendorName}
                    <span className="muted mono" style={{ marginLeft: 6 }}>
                      {po.vendorCode}
                    </span>
                  </td>
                  <td>{po.warehouseName}</td>
                  <td>
                    <StatusBadge map={PURCHASE_ORDER_STATUS} status={po.status} />
                  </td>
                  <td>{po.orderDate}</td>
                  <td className="num mono">{formatMoney(po.totalAmount)}</td>
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

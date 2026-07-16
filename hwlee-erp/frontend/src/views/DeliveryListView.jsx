import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchDeliveries } from '../api/deliveries'
import { listWarehouses } from '../api/masters'
import { DELIVERY_STATUS } from '../domain/status'
import StatusBadge from '../components/StatusBadge'

const PAGE_SIZE = 10

export default function DeliveryListView() {
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
    queryKey: ['deliveries', params],
    queryFn: () => searchDeliveries(params),
    placeholderData: keepPreviousData,
  })

  // 출하 응답엔 창고 id만 있어 이름을 별도로 매핑한다(창고 수가 적음).
  const { data: warehouses = [] } = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => listWarehouses(),
  })
  const whName = Object.fromEntries(warehouses.map((w) => [w.id, w.name]))

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
        <h1>출하</h1>
      </div>

      <div className="toolbar">
        <div>
          <label>상태</label>
          <select
            value={status}
            onChange={(e) => resetToFirstPage(setStatus)(e.target.value)}
          >
            <option value="">전체</option>
            {Object.entries(DELIVERY_STATUS).map(([code, meta]) => (
              <option key={code} value={code}>
                {meta.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>출하일 From</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => resetToFirstPage(setDateFrom)(e.target.value)}
          />
        </div>
        <div>
          <label>출하일 To</label>
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
          <p className="muted">조건에 맞는 출하가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>출하번호</th>
                <th>수주번호</th>
                <th>창고</th>
                <th>상태</th>
                <th>출하일</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((d) => (
                <tr key={d.id}>
                  <td>
                    <Link to={`/deliveries/${d.id}`} className="mono">
                      {d.number}
                    </Link>
                  </td>
                  <td className="mono">{d.salesOrderNumber}</td>
                  <td>{whName[d.warehouseId] || d.warehouseId}</td>
                  <td>
                    <StatusBadge map={DELIVERY_STATUS} status={d.status} />
                  </td>
                  <td>{d.shippedDate}</td>
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

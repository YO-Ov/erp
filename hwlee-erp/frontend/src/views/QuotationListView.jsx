import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchQuotations } from '../api/quotations'
import { QUOTATION_STATUS, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'

const PAGE_SIZE = 10

export default function QuotationListView() {
  const [status, setStatus] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)

  // 서버 상태(목록)는 TanStack Query가 관리. 필터/페이지가 바뀌면 자동 재조회.
  const params = {
    page,
    size: PAGE_SIZE,
    sort: 'id,desc',
    ...(status && { status }),
    ...(dateFrom && { dateFrom }),
    ...(dateTo && { dateTo }),
  }
  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['quotations', params],
    queryFn: () => searchQuotations(params),
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
        <h1>견적</h1>
        <Link to="/quotations/new">
          <button className="primary">+ 견적 작성</button>
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
            {Object.entries(QUOTATION_STATUS).map(([code, meta]) => (
              <option key={code} value={code}>
                {meta.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>발행일 From</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => resetToFirstPage(setDateFrom)(e.target.value)}
          />
        </div>
        <div>
          <label>발행일 To</label>
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
          <p className="muted">조건에 맞는 견적이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>견적번호</th>
                <th>고객</th>
                <th>상태</th>
                <th>발행일</th>
                <th>유효기한</th>
                <th className="num">금액</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((q) => (
                <tr key={q.id}>
                  <td>
                    <Link to={`/quotations/${q.id}`} className="mono">
                      {q.number}
                    </Link>
                  </td>
                  <td>
                    {q.customerName}
                    <span className="muted mono" style={{ marginLeft: 6 }}>
                      {q.customerCode}
                    </span>
                  </td>
                  <td>
                    <StatusBadge map={QUOTATION_STATUS} status={q.status} />
                  </td>
                  <td>{q.issuedDate}</td>
                  <td>{q.validUntil || '-'}</td>
                  <td className="num mono">{formatMoney(q.totalAmount)}</td>
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

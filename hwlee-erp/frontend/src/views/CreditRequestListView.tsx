import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchCreditRequests } from '../api/credit'
import { CREDIT_REQUEST_STATUS, formatMoney } from '../domain/status'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'
import type { CreditRequestStatus } from '../types/api'

const PAGE_SIZE = 12

export default function CreditRequestListView() {
  const { hasRole } = useAuth()
  const canCreate = hasRole('SALES', 'ADMIN') // 요청 상향은 영업/관리자만

  const [status, setStatus] = useState<CreditRequestStatus | ''>('')
  const [page, setPage] = useState(0)

  const params = {
    page,
    size: PAGE_SIZE,
    sort: 'id,desc',
    ...(status && { status }),
  }
  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['credit-requests', params],
    queryFn: () => searchCreditRequests(params),
    placeholderData: keepPreviousData,
  })

  const rows = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  return (
    <div className="container">
      <div className="page-head">
        <h1>여신 상향 요청</h1>
        {canCreate && (
          <Link to="/credit-requests/new">
            <button className="primary">+ 여신 상향 요청</button>
          </Link>
        )}
      </div>

      <div className="toolbar">
        <div>
          <label>상태</label>
          <select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as CreditRequestStatus | '')
              setPage(0)
            }}
          >
            <option value="">전체</option>
            {Object.entries(CREDIT_REQUEST_STATUS).map(([code, meta]) => (
              <option key={code} value={code}>
                {meta.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="panel">
        {isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : rows.length === 0 ? (
          <p className="muted">조건에 맞는 요청이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>번호</th>
                <th>고객</th>
                <th className="num">현재 한도</th>
                <th className="num">요청 한도</th>
                <th>상태</th>
                <th>요청자</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td>
                    <Link to={`/credit-requests/${r.id}`} className="mono">
                      {r.number}
                    </Link>
                  </td>
                  <td>{r.customerName}</td>
                  <td className="num mono">{formatMoney(r.currentLimit)}</td>
                  <td className="num mono">{formatMoney(r.requestedLimit)}</td>
                  <td>
                    <StatusBadge map={CREDIT_REQUEST_STATUS} status={r.status} />
                  </td>
                  <td className="muted">{r.requestedBy}</td>
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

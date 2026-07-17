import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchPayments } from '../api/payments'
import { PAYMENT_STATUS, PAYMENT_TYPE, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import type { PaymentType } from '../types/api'

const PAGE_SIZE = 10

export default function PaymentListView() {
  // '' = 전체(필터 없음). 서버 enum 과 빈 문자열만 허용해 오타를 막는다.
  const [type, setType] = useState<PaymentType | ''>('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)

  const params = {
    page,
    size: PAGE_SIZE,
    sort: 'id,desc',
    ...(type && { type }),
    ...(dateFrom && { dateFrom }),
    ...(dateTo && { dateTo }),
  }
  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['payments', params],
    queryFn: () => searchPayments(params),
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
        <h1>입금/출금</h1>
        <Link to="/payments/new">
          <button className="primary">+ 입출금 등록</button>
        </Link>
      </div>

      <div className="toolbar">
        <div>
          <label>구분</label>
          <select value={type} onChange={(e) => resetToFirstPage(setType)(e.target.value as PaymentType | '')}>
            <option value="">전체</option>
            {Object.entries(PAYMENT_TYPE).map(([code, meta]) => (
              <option key={code} value={code}>
                {meta.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>거래일 From</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => resetToFirstPage(setDateFrom)(e.target.value)}
          />
        </div>
        <div>
          <label>거래일 To</label>
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
          <p className="muted">조건에 맞는 입출금이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>번호</th>
                <th>구분</th>
                <th>거래처</th>
                <th>거래일</th>
                <th>상태</th>
                <th className="num">금액</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((p) => (
                <tr key={p.id}>
                  <td>
                    <Link to={`/payments/${p.id}`} className="mono">
                      {p.number}
                    </Link>
                  </td>
                  <td>
                    <StatusBadge map={PAYMENT_TYPE} status={p.type} />
                  </td>
                  <td className="mono muted">{p.customerCode || p.vendorCode || '-'}</td>
                  <td>{p.paymentDate}</td>
                  <td>
                    <StatusBadge map={PAYMENT_STATUS} status={p.status} />
                  </td>
                  <td className="num mono">{formatMoney(p.amount)}</td>
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

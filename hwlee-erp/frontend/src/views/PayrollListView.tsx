import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchPayrollRuns, createPayrollRun } from '../api/hr'
import { PAYROLL_STATUS, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'

const PAGE_SIZE = 12

// 이번 달 'YYYY-MM'.
function thisMonth(): string {
  return new Date().toISOString().slice(0, 7)
}

// 급여대장 목록 + 생성. HR/ADMIN 전용.
export default function PayrollListView() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [period, setPeriod] = useState(thisMonth())

  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['payroll-runs', page],
    queryFn: () => searchPayrollRuns({ page, size: PAGE_SIZE, sort: 'id,desc' }),
    placeholderData: keepPreviousData,
  })

  // 대상 월만 보내면 유효계약 + 근태로 명세를 자동 계산해 DRAFT 로 만든다.
  const createMutation = useMutation({
    mutationFn: () => createPayrollRun({ period }),
    onSuccess: (saved) => navigate(`/payroll-runs/${saved.id}`),
  })

  function onCreate(e: FormEvent) {
    e.preventDefault()
    createMutation.mutate()
  }

  const rows = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  return (
    <div className="container">
      <div className="page-head">
        <h1>급여대장</h1>
      </div>

      <div className="panel" style={{ marginBottom: 16 }}>
        <form onSubmit={onCreate}>
          <div className="row" style={{ alignItems: 'flex-end' }}>
            <div className="field">
              <label>대상 월</label>
              <input type="month" value={period} onChange={(e) => setPeriod(e.target.value)} required />
            </div>
            <div className="field">
              <button className="primary" type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? '생성 중…' : '급여대장 생성'}
              </button>
            </div>
            <div className="field" style={{ flex: 2 }}>
              <span className="muted" style={{ fontSize: 12 }}>
                대상 월의 유효계약 + 근태로 명세가 자동 계산됩니다(작성중 상태).
              </span>
            </div>
          </div>
          {createMutation.isError && (
            <p className="error" style={{ marginTop: 8 }}>
              {createMutation.error.message}
            </p>
          )}
        </form>
      </div>

      <div className="panel">
        {isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : rows.length === 0 ? (
          <p className="muted">생성된 급여대장이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>번호</th>
                <th>대상 월</th>
                <th>상태</th>
                <th className="num">지급총액</th>
                <th className="num">공제총액</th>
                <th className="num">실지급액</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td>
                    <Link to={`/payroll-runs/${r.id}`} className="mono">
                      {r.number}
                    </Link>
                  </td>
                  <td>{r.period}</td>
                  <td>
                    <StatusBadge map={PAYROLL_STATUS} status={r.status} />
                  </td>
                  <td className="num mono">{formatMoney(r.totalGross)}</td>
                  <td className="num mono">{formatMoney(r.totalDeduction)}</td>
                  <td className="num mono">{formatMoney(r.totalNet)}</td>
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

import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchJournalEntries } from '../api/journalEntries'
import { JOURNAL_ENTRY_STATUS, JOURNAL_SOURCE, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import type { JournalEntryStatus, JournalSource } from '../types/api'

const PAGE_SIZE = 10

export default function JournalEntryListView() {
  // '' = 전체(필터 없음). 서버 enum 과 빈 문자열만 허용해 오타를 막는다.
  const [status, setStatus] = useState<JournalEntryStatus | ''>('')
  // '' = 전체(필터 없음). 서버 enum 과 빈 문자열만 허용해 오타를 막는다.
  const [sourceType, setSourceType] = useState<JournalSource | ''>('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)

  const params = {
    page,
    size: PAGE_SIZE,
    sort: 'id,desc',
    ...(status && { status }),
    ...(sourceType && { sourceType }),
    ...(dateFrom && { dateFrom }),
    ...(dateTo && { dateTo }),
  }
  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['journal-entries', params],
    queryFn: () => searchJournalEntries(params),
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
        <h1>전표</h1>
        <Link to="/journal-entries/new">
          <button className="primary">+ 수동 전표 작성</button>
        </Link>
      </div>

      <div className="toolbar">
        <div>
          <label>상태</label>
          <select
            value={status}
            onChange={(e) => resetToFirstPage(setStatus)(e.target.value as JournalEntryStatus | '')}
          >
            <option value="">전체</option>
            {Object.entries(JOURNAL_ENTRY_STATUS).map(([code, meta]) => (
              <option key={code} value={code}>
                {meta.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>출처</label>
          <select
            value={sourceType}
            onChange={(e) => resetToFirstPage(setSourceType)(e.target.value as JournalSource | '')}
          >
            <option value="">전체</option>
            {Object.entries(JOURNAL_SOURCE).map(([code, label]) => (
              <option key={code} value={code}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>전표일 From</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => resetToFirstPage(setDateFrom)(e.target.value)}
          />
        </div>
        <div>
          <label>전표일 To</label>
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
          <p className="muted">조건에 맞는 전표가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>전표번호</th>
                <th>전표일</th>
                <th>적요</th>
                <th>출처</th>
                <th>상태</th>
                <th className="num">차변합</th>
                <th className="num">대변합</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((je) => (
                <tr key={je.id}>
                  <td>
                    <Link to={`/journal-entries/${je.id}`} className="mono">
                      {je.number}
                    </Link>
                  </td>
                  <td>{je.entryDate}</td>
                  <td>{je.description}</td>
                  <td className="muted">{JOURNAL_SOURCE[je.sourceType] || je.sourceType}</td>
                  <td>
                    <StatusBadge map={JOURNAL_ENTRY_STATUS} status={je.status} />
                  </td>
                  <td className="num mono">{formatMoney(je.totalDebit)}</td>
                  <td className="num mono">{formatMoney(je.totalCredit)}</td>
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

import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchCustomers, type CustomerSearchParams } from '../api/customers'
import { MASTER_STATUS, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import { toCsv, downloadCsv, todayStamp } from '../utils/csv'
import type { MasterStatus } from '../types/api'

const PAGE_SIZE = 20

// 정렬 가능한 컬럼(백엔드 Pageable sort 키와 1:1).
type SortKey = 'code' | 'name' | 'creditLimit' | 'status'
type SortDir = 'asc' | 'desc'

// 고객 마스터 목록 — 영업(SD). 이름 검색 + 상태 필터 + 서버 페이징 + 헤더 정렬 + CSV 내보내기.
export default function CustomerListView() {
  const [name, setName] = useState('')
  const [status, setStatus] = useState<MasterStatus | ''>('')
  const [page, setPage] = useState(0)
  const [sortKey, setSortKey] = useState<SortKey>('code')
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [exporting, setExporting] = useState(false)

  const filters: CustomerSearchParams = {
    ...(name.trim() && { name: name.trim() }),
    ...(status && { status }),
  }
  const params: CustomerSearchParams = {
    ...filters,
    page,
    size: PAGE_SIZE,
    sort: `${sortKey},${sortDir}`,
  }
  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['customers', params],
    queryFn: () => searchCustomers(params),
    placeholderData: keepPreviousData,
  })

  const rows = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  // 필터를 바꾸면 첫 페이지로 되돌린다(뒷 페이지 보다가 조건 바꾸면 빈 화면이 되는 걸 막는다).
  function resetToFirstPage<T>(setter: (v: T) => void) {
    return (v: T) => {
      setter(v)
      setPage(0)
    }
  }

  // 헤더 클릭 → 같은 컬럼이면 방향 토글, 다른 컬럼이면 그 컬럼 기준(금액은 내림차순부터).
  function onSort(key: SortKey) {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(key)
      setSortDir(key === 'creditLimit' ? 'desc' : 'asc')
    }
    setPage(0)
  }

  // 정렬 헤더에 붙일 표시(▲▼) 및 클릭 속성.
  function sortHeader(label: string, key: SortKey, extraClass = '') {
    const ind = sortKey === key ? (sortDir === 'asc' ? ' ▲' : ' ▼') : ' ↕'
    return (
      <th
        className={extraClass}
        style={{ cursor: 'pointer', userSelect: 'none' }}
        onClick={() => onSort(key)}
      >
        {label}
        <span className="muted">{ind}</span>
      </th>
    )
  }

  // 현재 검색조건 전체를 한 번에 받아 CSV(엑셀)로 내려받는다(현재 페이지가 아니라 전체).
  async function exportCsv() {
    setExporting(true)
    try {
      const all = await searchCustomers({
        ...filters,
        page: 0,
        size: 10000,
        sort: `${sortKey},${sortDir}`,
      })
      const list = all.content || []
      if (list.length === 0) {
        window.alert('내보낼 고객이 없습니다.')
        return
      }
      const csv = toCsv(list, [
        { label: '코드', value: (c) => c.code },
        { label: '고객명', value: (c) => c.name },
        { label: '사업자번호', value: (c) => c.businessNo },
        { label: '여신한도', value: (c) => c.creditLimit ?? 0 },
        { label: '결제조건', value: (c) => c.paymentTerms },
        { label: '상태', value: (c) => MASTER_STATUS[c.status]?.label ?? c.status },
      ])
      downloadCsv(`고객목록_${todayStamp()}.csv`, csv)
    } catch (e) {
      window.alert(e instanceof Error ? e.message : '내보내기에 실패했습니다.')
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="container">
      <div className="page-head">
        <h1>고객</h1>
        <Link to="/customers/new">
          <button className="primary">+ 신규 고객</button>
        </Link>
      </div>

      <div className="toolbar">
        <div>
          <label>고객명</label>
          <input
            type="text"
            value={name}
            onChange={(e) => resetToFirstPage(setName)(e.target.value)}
            placeholder="이름 검색"
          />
        </div>
        <div>
          <label>상태</label>
          <select
            value={status}
            onChange={(e) => resetToFirstPage(setStatus)(e.target.value as MasterStatus | '')}
          >
            <option value="">전체</option>
            {Object.entries(MASTER_STATUS).map(([code, meta]) => (
              <option key={code} value={code}>
                {meta.label}
              </option>
            ))}
          </select>
        </div>
        <div style={{ marginLeft: 'auto', alignSelf: 'flex-end' }}>
          <button className="sm" onClick={exportCsv} disabled={exporting || totalElements === 0}>
            {exporting ? '내보내는 중…' : 'Excel 다운로드'}
          </button>
        </div>
      </div>

      <div className="panel">
        {isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : rows.length === 0 ? (
          <p className="muted">조건에 맞는 고객이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                {sortHeader('코드', 'code')}
                {sortHeader('고객명', 'name')}
                <th>사업자번호</th>
                {sortHeader('여신한도', 'creditLimit', 'num')}
                <th>결제조건</th>
                {sortHeader('상태', 'status')}
              </tr>
            </thead>
            <tbody>
              {rows.map((c) => (
                <tr key={c.id}>
                  <td>
                    <Link to={`/customers/${c.id}`} className="mono">
                      {c.code}
                    </Link>
                  </td>
                  <td>{c.name}</td>
                  <td className="mono muted">{c.businessNo || '-'}</td>
                  <td className="num mono">
                    {c.creditLimit === 0 ? (
                      <span className="muted">현금</span>
                    ) : (
                      formatMoney(c.creditLimit)
                    )}
                  </td>
                  <td>{c.paymentTerms}</td>
                  <td>
                    <StatusBadge map={MASTER_STATUS} status={c.status} />
                  </td>
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

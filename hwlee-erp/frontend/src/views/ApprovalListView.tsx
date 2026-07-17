import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { getInbox, getOutbox } from '../api/approvals'
import { APPROVAL_STATUS, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'

const PAGE_SIZE = 10

/** 결재함(내가 처리) / 상신함(내가 올림). */
type ApprovalBox = 'inbox' | 'outbox'

// 결재함(내가 처리할 것) / 상신함(내가 올린 것) 두 탭.
// 탭은 ?box=inbox|outbox 로 URL에 남겨 새로고침·뒤로가기에도 유지된다.
export default function ApprovalListView() {
  const [searchParams, setSearchParams] = useSearchParams()
  const box = searchParams.get('box') === 'outbox' ? 'outbox' : 'inbox'
  const [page, setPage] = useState(0)

  const params = { page, size: PAGE_SIZE, sort: 'id,desc' }
  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['approvals', box, params],
    queryFn: () => (box === 'inbox' ? getInbox(params) : getOutbox(params)),
    placeholderData: keepPreviousData,
  })

  const rows = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  function switchBox(next: ApprovalBox) {
    setPage(0)
    setSearchParams(next === 'inbox' ? {} : { box: next })
  }

  return (
    <div className="container">
      <div className="page-head">
        <h1>전자결재</h1>
      </div>

      <div className="toolbar">
        <div className="actions">
          <button
            className={box === 'inbox' ? 'primary' : ''}
            onClick={() => switchBox('inbox')}
          >
            결재함 (내가 처리)
          </button>
          <button
            className={box === 'outbox' ? 'primary' : ''}
            onClick={() => switchBox('outbox')}
          >
            상신함 (내가 올림)
          </button>
        </div>
      </div>

      <div className="panel">
        {isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : rows.length === 0 ? (
          <p className="muted">
            {box === 'inbox'
              ? '처리할 결재가 없습니다.'
              : '내가 올린 결재가 없습니다.'}
          </p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>결재번호</th>
                <th>문서</th>
                <th>제목</th>
                <th className="num">금액</th>
                <th>{box === 'inbox' ? '상신자' : '상신일'}</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((a) => (
                <tr key={a.id}>
                  <td>
                    <Link to={`/approvals/${a.id}`} className="mono">
                      {a.number}
                    </Link>
                  </td>
                  <td className="muted">{a.docTypeLabel}</td>
                  <td>
                    {a.title}
                    {/* 내 차례면 눈에 띄게 — 결재함에서 뭘 먼저 봐야 하는지 */}
                    {a.myTurn && (
                      <span className="mono" style={{ marginLeft: 8, color: 'var(--accent)' }}>
                        ● 내 차례
                      </span>
                    )}
                  </td>
                  <td className="num mono">{formatMoney(a.amount)}</td>
                  <td className="muted">
                    {box === 'inbox' ? a.requester : (a.requestedAt || '').slice(0, 10)}
                  </td>
                  <td>
                    <StatusBadge map={APPROVAL_STATUS} status={a.status} />
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

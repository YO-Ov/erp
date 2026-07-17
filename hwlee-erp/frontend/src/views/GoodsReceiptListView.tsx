import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchGoodsReceipts } from '../api/goodsReceipts'
import { GOODS_RECEIPT_STATUS } from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import type { GoodsReceiptStatus } from '../types/api'

const PAGE_SIZE = 10

export default function GoodsReceiptListView() {
  // '' = 전체(필터 없음). 서버 enum 과 빈 문자열만 허용해 오타를 막는다.
  const [status, setStatus] = useState<GoodsReceiptStatus | ''>('')
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
    queryKey: ['goods-receipts', params],
    queryFn: () => searchGoodsReceipts(params),
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
        <h1>입고</h1>
      </div>

      <div className="toolbar">
        <div>
          <label>상태</label>
          <select
            value={status}
            onChange={(e) => resetToFirstPage(setStatus)(e.target.value as GoodsReceiptStatus | '')}
          >
            <option value="">전체</option>
            {Object.entries(GOODS_RECEIPT_STATUS).map(([code, meta]) => (
              <option key={code} value={code}>
                {meta.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>입고일 From</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => resetToFirstPage(setDateFrom)(e.target.value)}
          />
        </div>
        <div>
          <label>입고일 To</label>
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
          <p className="muted">조건에 맞는 입고가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>입고번호</th>
                <th>공급처</th>
                <th>입고창고</th>
                <th>발주번호</th>
                <th>상태</th>
                <th>입고일</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((gr) => (
                <tr key={gr.id}>
                  <td>
                    <Link to={`/goods-receipts/${gr.id}`} className="mono">
                      {gr.number}
                    </Link>
                  </td>
                  <td>
                    {gr.vendorName}
                    <span className="muted mono" style={{ marginLeft: 6 }}>
                      {gr.vendorCode}
                    </span>
                  </td>
                  <td>{gr.warehouseName}</td>
                  <td className="mono">{gr.purchaseOrderNumber || '-'}</td>
                  <td>
                    <StatusBadge map={GOODS_RECEIPT_STATUS} status={gr.status} />
                  </td>
                  <td>{gr.receiptDate}</td>
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

import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { getSalesReport } from '../api/reports'
import { formatMoney } from '../domain/status'
import type { SalesReportUnit } from '../types/api'
import ReportTabs from '../components/ReportTabs'

// 올해 1월 1일 ~ 오늘.
function defaultRange(): { from: string; to: string } {
  const now = new Date()
  const year = now.getUTCFullYear()
  return { from: `${year}-01-01`, to: now.toISOString().slice(0, 10) }
}

export default function SalesReportView() {
  const init = defaultRange()
  const [from, setFrom] = useState(init.from)
  const [to, setTo] = useState(init.to)
  const [unit, setUnit] = useState<SalesReportUnit>('MONTH')

  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['sales-report', from, to, unit],
    queryFn: () => getSalesReport(from, to, unit),
    placeholderData: keepPreviousData,
  })

  return (
    <div className="container">
      <div className="page-head">
        <h1>리포트</h1>
      </div>
      <ReportTabs />

      <div className="toolbar">
        <div>
          <label>시작일</label>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div>
          <label>종료일</label>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        <div>
          <label>집계 단위</label>
          <select value={unit} onChange={(e) => setUnit(e.target.value as SalesReportUnit)}>
            <option value="DAY">일별</option>
            <option value="MONTH">월별</option>
          </select>
        </div>
      </div>

      <div className="panel">
        {isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : !data || data.rows.length === 0 ? (
          <p className="muted">해당 기간 매출이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>{unit === 'MONTH' ? '월' : '일자'}</th>
                <th className="num">건수</th>
                <th className="num">공급가</th>
                <th className="num">부가세</th>
                <th className="num">합계</th>
              </tr>
            </thead>
            <tbody>
              {data.rows.map((r) => (
                <tr key={r.period}>
                  <td>{r.period}</td>
                  <td className="num mono">{r.invoiceCount}</td>
                  <td className="num mono">{formatMoney(r.subtotal)}</td>
                  <td className="num mono">{formatMoney(r.taxAmount)}</td>
                  <td className="num mono">{formatMoney(r.totalAmount)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td>
                  <strong>합계</strong>
                </td>
                <td className="num mono">
                  <strong>{data.total.invoiceCount}</strong>
                </td>
                <td className="num mono">
                  <strong>{formatMoney(data.total.subtotal)}</strong>
                </td>
                <td className="num mono">
                  <strong>{formatMoney(data.total.taxAmount)}</strong>
                </td>
                <td className="num mono">
                  <strong>{formatMoney(data.total.totalAmount)}</strong>
                </td>
              </tr>
            </tfoot>
          </table>
        )}
        {isFetching && <p className="muted" style={{ fontSize: 12 }}>갱신중…</p>}
      </div>
    </div>
  )
}

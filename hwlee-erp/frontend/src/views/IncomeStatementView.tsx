import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { getIncomeStatement } from '../api/reports'
import { formatMoney } from '../domain/status'
import type { IncomeStatementLine } from '../types/api'
import ReportTabs from '../components/ReportTabs'

function defaultRange(): { from: string; to: string } {
  const now = new Date()
  return { from: `${now.getUTCFullYear()}-01-01`, to: now.toISOString().slice(0, 10) }
}

// 손익 요약의 한 줄. 이익 항목은 굵게 강조한다.
function SummaryRow({ label, value, bold }: { label: string; value: number; bold?: boolean }) {
  return (
    <tr style={bold ? { fontWeight: 700 } : undefined}>
      <td>{label}</td>
      <td className="num mono">{formatMoney(value)}</td>
    </tr>
  )
}

function LineTable({ title, lines }: { title: string; lines: IncomeStatementLine[] }) {
  return (
    <>
      <div className="section-title">{title}</div>
      <div className="panel">
        {lines.length === 0 ? (
          <p className="muted">해당 기간 계정 명세가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>계정</th>
                <th className="num">금액</th>
              </tr>
            </thead>
            <tbody>
              {lines.map((l) => (
                <tr key={l.accountCode}>
                  <td>
                    {l.accountName} <span className="muted mono">{l.accountCode}</span>
                  </td>
                  <td className="num mono">{formatMoney(l.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}

export default function IncomeStatementView() {
  const init = defaultRange()
  const [from, setFrom] = useState(init.from)
  const [to, setTo] = useState(init.to)

  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['income-statement', from, to],
    queryFn: () => getIncomeStatement(from, to),
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
        {isFetching && <div style={{ alignSelf: 'flex-end' }}><span className="muted">갱신중…</span></div>}
      </div>

      {isLoading ? (
        <div className="panel"><p className="muted">불러오는 중…</p></div>
      ) : isError ? (
        <div className="panel"><p className="error">{error.message}</p></div>
      ) : !data ? null : (
        <>
          <div className="panel">
            <table>
              <tbody>
                <SummaryRow label="매출" value={data.revenue} />
                <SummaryRow label="(−) 매출원가" value={data.costOfGoodsSold} />
                <SummaryRow label="매출총이익" value={data.grossProfit} bold />
                <SummaryRow label="(−) 판매관리비" value={data.sgaExpense} />
                <SummaryRow label="영업이익" value={data.operatingProfit} bold />
                <SummaryRow label="당기순이익" value={data.netIncome} bold />
              </tbody>
            </table>
            <p className="muted" style={{ marginTop: 10, fontSize: 12 }}>
              매출총이익 = 매출 − 매출원가, 영업이익 = 매출총이익 − 판관비 (POSTED 전표 기준)
            </p>
          </div>

          <LineTable title="수익 명세" lines={data.revenueLines} />
          <LineTable title="비용 명세" lines={data.expenseLines} />
        </>
      )}
    </div>
  )
}

import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getPayrollRun, payrollRunAction } from '../api/hr'
import {
  PAYROLL_STATUS,
  payrollActions,
  PAYROLL_ACTION_LABEL,
  formatMoney,
} from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import type { PayrollAction } from '../types/api'

export default function PayrollDetailView() {
  const { id = '' } = useParams()
  const queryClient = useQueryClient()

  const { data: run, isLoading, isError, error } = useQuery({
    queryKey: ['payroll-run', id],
    queryFn: () => getPayrollRun(id),
  })

  const mutation = useMutation({
    mutationFn: (action: PayrollAction) => payrollRunAction(id, action),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payroll-run', id] })
      queryClient.invalidateQueries({ queryKey: ['payroll-runs'] })
      // 확정·지급은 FI 전표를 만든다 — 전표 목록 캐시도 비운다.
      queryClient.invalidateQueries({ queryKey: ['journal-entries'] })
    },
  })

  function onAction(action: PayrollAction) {
    const msg =
      action === 'confirm'
        ? '급여를 확정하시겠습니까? 인건비 전표가 생성됩니다.'
        : '지급 처리하시겠습니까? 지급 전표가 생성됩니다.'
    if (!window.confirm(msg)) return
    mutation.mutate(action)
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  if (!run) return null

  const actions = payrollActions(run.status)

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/payroll-runs" className="muted">
            ← 급여대장 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{run.number}</span>{' '}
            <StatusBadge map={PAYROLL_STATUS} status={run.status} />
          </h1>
        </div>
        <div className="actions">
          {actions.map((a) => (
            <button
              key={a}
              className="primary"
              disabled={mutation.isPending}
              onClick={() => onAction(a)}
            >
              {PAYROLL_ACTION_LABEL[a]}
            </button>
          ))}
        </div>
      </div>

      {mutation.isError && <p className="error">{mutation.error.message}</p>}

      <div className="panel">
        <div className="info-grid">
          <div>
            <div className="k">대상 월</div>
            <div className="v">{run.period}</div>
          </div>
          <div>
            <div className="k">지급총액</div>
            <div className="v mono">{formatMoney(run.totalGross)}</div>
          </div>
          <div>
            <div className="k">공제총액</div>
            <div className="v mono">{formatMoney(run.totalDeduction)}</div>
          </div>
          <div>
            <div className="k">실지급액</div>
            <div className="v mono">{formatMoney(run.totalNet)}</div>
          </div>
          <div>
            <div className="k">확정일시</div>
            <div className="v">{run.confirmedAt || '-'}</div>
          </div>
          <div>
            <div className="k">지급일시</div>
            <div className="v">{run.paidAt || '-'}</div>
          </div>
        </div>
      </div>

      <div className="section-title">급여명세 ({run.payslips.length}명)</div>
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>사원</th>
              <th className="num">기본급</th>
              <th className="num">연장수당</th>
              <th className="num">지급액</th>
              <th className="num">소득세</th>
              <th className="num">보험(본인)</th>
              <th className="num">공제계</th>
              <th className="num">실수령</th>
            </tr>
          </thead>
          <tbody>
            {run.payslips.map((p) => (
              <tr key={p.id}>
                <td>
                  {p.employeeName} <span className="muted mono">{p.employeeCode}</span>
                </td>
                <td className="num mono">{formatMoney(p.basePay)}</td>
                <td className="num mono">{formatMoney(p.overtimePay)}</td>
                <td className="num mono">{formatMoney(p.grossPay)}</td>
                <td className="num mono">{formatMoney(p.incomeTax)}</td>
                <td className="num mono">{formatMoney(p.insuranceEmployee)}</td>
                <td className="num mono">{formatMoney(p.totalDeduction)}</td>
                <td className="num mono">
                  <strong>{formatMoney(p.netPay)}</strong>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        확정 = 비용 인식(인건비 전표), 지급 = 현금 유출(지급 전표) — 발생주의의 2단계.
      </p>
    </div>
  )
}

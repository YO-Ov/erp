import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getEmployee } from '../api/employees'
import { listContractsByEmployee, searchAttendances, createAttendance } from '../api/hr'
import { useAuth } from '../auth/AuthContext'
import {
  CONTRACT_STATUS,
  POSITION,
  formatMoney,
  formatMinutes,
} from '../domain/status'
import StatusBadge from '../components/StatusBadge'

// 이번 달 'YYYY-MM'.
function thisMonth(): string {
  return new Date().toISOString().slice(0, 7)
}

// 사원 상세. 기본정보는 넓은 역할이 보고, 급여계약·근태는 HR/ADMIN 만 로드한다(민감정보).
export default function EmployeeDetailView() {
  const { id = '' } = useParams()
  const employeeId = Number(id)
  const queryClient = useQueryClient()
  const { hasRole } = useAuth()
  const canHr = hasRole('HR', 'ADMIN')

  const [month, setMonth] = useState(thisMonth())

  const { data: emp, isLoading, isError, error } = useQuery({
    queryKey: ['employee', id],
    queryFn: () => getEmployee(id),
  })

  // HR/ADMIN 일 때만 계약·근태를 부른다(비-HR 은 백엔드도 403).
  const { data: contracts = [] } = useQuery({
    queryKey: ['contracts', id],
    queryFn: () => listContractsByEmployee(employeeId),
    enabled: canHr && !!emp,
  })

  // 선택한 달의 1일~말일. 근태 조회는 기간이 필수라 월 기준으로 만든다.
  const from = `${month}-01`
  const to = `${month}-31`
  const { data: attendances = [] } = useQuery({
    queryKey: ['attendances', id, month],
    queryFn: () => searchAttendances(employeeId, from, to),
    enabled: canHr && !!emp,
  })

  // 근태 등록 인라인 폼.
  const [workDate, setWorkDate] = useState(`${thisMonth()}-01`)
  const [clockIn, setClockIn] = useState('09:00')
  const [clockOut, setClockOut] = useState('18:00')

  const addMutation = useMutation({
    mutationFn: () =>
      createAttendance({ employeeId, workDate, clockIn, clockOut }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attendances', id] })
    },
  })

  function onAddAttendance(e: FormEvent) {
    e.preventDefault()
    addMutation.mutate()
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  if (!emp) return null

  const totalOvertime = attendances.reduce((s, a) => s + a.overtimeMinutes, 0)

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/employees" className="muted">
            ← 사원 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            {emp.name} <span className="muted mono">{emp.code}</span>
          </h1>
        </div>
      </div>

      <div className="panel">
        <div className="info-grid">
          <div>
            <div className="k">부서</div>
            <div className="v">{emp.departmentName || '-'}</div>
          </div>
          <div>
            <div className="k">이메일</div>
            <div className="v">{emp.email || '-'}</div>
          </div>
          <div>
            <div className="k">입사일</div>
            <div className="v">{emp.hireDate}</div>
          </div>
          <div>
            <div className="k">상태</div>
            <div className="v">{emp.status}</div>
          </div>
        </div>
      </div>

      {!canHr ? (
        <p className="muted" style={{ marginTop: 16 }}>
          급여계약·근태는 인사(HR) 권한이 있어야 조회할 수 있습니다.
        </p>
      ) : (
        <>
          <div className="section-title">급여계약 이력</div>
          <div className="panel">
            {contracts.length === 0 ? (
              <p className="muted">등록된 급여계약이 없습니다.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>직급</th>
                    <th className="num">기본급</th>
                    <th className="num">소정근로(월)</th>
                    <th className="num">시급</th>
                    <th>발효~만료</th>
                    <th>상태</th>
                  </tr>
                </thead>
                <tbody>
                  {contracts.map((c) => (
                    <tr key={c.id}>
                      <td>{POSITION[c.position]}</td>
                      <td className="num mono">{formatMoney(c.baseSalary)}</td>
                      <td className="num mono">{c.contractedHours}h</td>
                      <td className="num mono">{formatMoney(c.hourlyWage)}</td>
                      <td>
                        {c.effectiveFrom} ~ {c.effectiveTo || '현재'}
                      </td>
                      <td>
                        <StatusBadge map={CONTRACT_STATUS} status={c.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          <div className="section-title">근태</div>
          <div className="panel">
            <div className="toolbar" style={{ marginBottom: 12 }}>
              <div>
                <label>조회 월</label>
                <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} />
              </div>
              {attendances.length > 0 && (
                <div style={{ alignSelf: 'flex-end' }}>
                  <span className="muted">
                    {attendances.length}일 근무 · 연장 합계{' '}
                    <strong>{formatMinutes(totalOvertime)}</strong>
                  </span>
                </div>
              )}
            </div>

            {attendances.length === 0 ? (
              <p className="muted">해당 월 근태 기록이 없습니다.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>근무일</th>
                    <th>출근</th>
                    <th>퇴근</th>
                    <th className="num">근무시간</th>
                    <th className="num">연장</th>
                  </tr>
                </thead>
                <tbody>
                  {attendances.map((a) => (
                    <tr key={a.id}>
                      <td>{a.workDate}</td>
                      <td className="mono">{a.clockIn.slice(0, 5)}</td>
                      <td className="mono">{a.clockOut.slice(0, 5)}</td>
                      <td className="num mono">{formatMinutes(a.workedMinutes)}</td>
                      <td className="num mono">
                        {a.overtimeMinutes > 0 ? formatMinutes(a.overtimeMinutes) : '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}

            {/* 근태 등록 — 하루 한 건. 소정근로 8h 초과분이 자동으로 연장 계산된다. */}
            <form onSubmit={onAddAttendance} style={{ marginTop: 16 }}>
              <div className="section-title" style={{ fontSize: 13 }}>근태 등록</div>
              <div className="row" style={{ alignItems: 'flex-end' }}>
                <div className="field">
                  <label>근무일</label>
                  <input
                    type="date"
                    value={workDate}
                    onChange={(e) => setWorkDate(e.target.value)}
                    required
                  />
                </div>
                <div className="field">
                  <label>출근</label>
                  <input
                    type="time"
                    value={clockIn}
                    onChange={(e) => setClockIn(e.target.value)}
                    required
                  />
                </div>
                <div className="field">
                  <label>퇴근</label>
                  <input
                    type="time"
                    value={clockOut}
                    onChange={(e) => setClockOut(e.target.value)}
                    required
                  />
                </div>
                <div className="field">
                  <button className="primary" type="submit" disabled={addMutation.isPending}>
                    {addMutation.isPending ? '등록 중…' : '등록'}
                  </button>
                </div>
              </div>
              {addMutation.isError && (
                <p className="error" style={{ marginTop: 8 }}>
                  {addMutation.error.message}
                </p>
              )}
            </form>
          </div>
        </>
      )}
    </div>
  )
}

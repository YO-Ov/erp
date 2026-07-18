import client from './client'
import type {
  Attendance,
  AttendanceCreateRequest,
  EmploymentContract,
  EmploymentContractCreateRequest,
  Page,
  PageParams,
  PayrollAction,
  PayrollRun,
  PayrollRunCreateRequest,
} from '../types/api'

// 인사(HR) REST API — 근태·급여계약·급여대장. 전부 HR/ADMIN 전용(급여는 민감정보).

// ── 근태 (/attendances) — 조회는 사원+기간 필수, 응답은 배열 ──
export async function searchAttendances(
  employeeId: number,
  from: string,
  to: string,
): Promise<Attendance[]> {
  const { data } = await client.get<Attendance[]>('/attendances', {
    params: { employeeId, from, to },
  })
  return data
}

export async function createAttendance(body: AttendanceCreateRequest): Promise<Attendance> {
  const { data } = await client.post<Attendance>('/attendances', body)
  return data
}

// ── 급여계약 (/employment-contracts) — 사원별 이력, 응답은 배열 ──
export async function listContractsByEmployee(
  employeeId: number,
): Promise<EmploymentContract[]> {
  const { data } = await client.get<EmploymentContract[]>('/employment-contracts', {
    params: { employeeId },
  })
  return data
}

export async function createContract(
  body: EmploymentContractCreateRequest,
): Promise<EmploymentContract> {
  const { data } = await client.post<EmploymentContract>('/employment-contracts', body)
  return data
}

// ACTIVE 계약만 종료 가능. effectiveTo 는 쿼리 파라미터.
export async function terminateContract(
  id: string | number,
  effectiveTo: string,
): Promise<EmploymentContract> {
  const { data } = await client.post<EmploymentContract>(
    `/employment-contracts/${id}/terminate`,
    null,
    { params: { effectiveTo } },
  )
  return data
}

// ── 급여대장 (/payroll-runs) — 검색은 Page ──
export async function searchPayrollRuns(params: PageParams = {}): Promise<Page<PayrollRun>> {
  const { data } = await client.get<Page<PayrollRun>>('/payroll-runs', { params })
  return data
}

export async function getPayrollRun(id: string | number): Promise<PayrollRun> {
  const { data } = await client.get<PayrollRun>(`/payroll-runs/${id}`)
  return data
}

// 대상 월만 받아 유효계약 + 근태로 명세를 자동 계산해 DRAFT 로 만든다.
export async function createPayrollRun(body: PayrollRunCreateRequest): Promise<PayrollRun> {
  const { data } = await client.post<PayrollRun>('/payroll-runs', body)
  return data
}

// confirm(인건비 전표) | pay(지급 전표) — 발생주의 2단계.
export async function payrollRunAction(
  id: string | number,
  action: PayrollAction,
): Promise<PayrollRun> {
  const { data } = await client.post<PayrollRun>(`/payroll-runs/${id}/${action}`)
  return data
}

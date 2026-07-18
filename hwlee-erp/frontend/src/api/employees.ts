import client from './client'
import type { Employee } from '../types/api'

// 사원 마스터 REST API (/api/employees).
// ⚠️ 조회는 넓은 역할(SALES/PURCHASING/FINANCE/HR/ADMIN), 쓰기는 ADMIN 전용.
//    목록이 Page 가 아니라 배열로 온다(계정과목과 같은 예외).

export async function listEmployees(): Promise<Employee[]> {
  const { data } = await client.get<Employee[]>('/employees')
  return data
}

export async function getEmployee(id: string | number): Promise<Employee> {
  const { data } = await client.get<Employee>(`/employees/${id}`)
  return data
}

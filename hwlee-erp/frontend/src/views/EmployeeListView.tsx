import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { listEmployees } from '../api/employees'

// 사원 목록 — 읽기 전용(조회는 넓은 역할). 목록은 배열이라 검색·필터를 프론트에서 처리한다.
export default function EmployeeListView() {
  const [keyword, setKeyword] = useState('')
  const [dept, setDept] = useState('')

  const { data: employees = [], isLoading, isError, error } = useQuery({
    queryKey: ['employees'],
    queryFn: listEmployees,
  })

  // 부서 목록은 사원 데이터에서 뽑는다(마스터 API 없이).
  const departments = useMemo(() => {
    const set = new Map<string, string>()
    for (const e of employees) {
      if (e.departmentCode) set.set(e.departmentCode, e.departmentName || e.departmentCode)
    }
    return [...set.entries()].sort((a, b) => a[1].localeCompare(b[1], 'ko'))
  }, [employees])

  const rows = employees.filter((e) => {
    if (dept && e.departmentCode !== dept) return false
    if (keyword) {
      const k = keyword.toLowerCase()
      return (
        e.name.toLowerCase().includes(k) ||
        e.code.toLowerCase().includes(k) ||
        (e.email || '').toLowerCase().includes(k)
      )
    }
    return true
  })

  return (
    <div className="container">
      <div className="page-head">
        <h1>사원</h1>
      </div>

      <div className="toolbar">
        <div>
          <label>검색</label>
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="이름·사번·이메일"
          />
        </div>
        <div>
          <label>부서</label>
          <select value={dept} onChange={(e) => setDept(e.target.value)}>
            <option value="">전체</option>
            {departments.map(([code, name]) => (
              <option key={code} value={code}>
                {name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="panel">
        {isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : rows.length === 0 ? (
          <p className="muted">조건에 맞는 사원이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>사번</th>
                <th>이름</th>
                <th>부서</th>
                <th>이메일</th>
                <th>입사일</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((e) => (
                <tr key={e.id}>
                  <td>
                    <Link to={`/employees/${e.id}`} className="mono">
                      {e.code}
                    </Link>
                  </td>
                  <td>{e.name}</td>
                  <td>{e.departmentName || '-'}</td>
                  <td className="muted">{e.email || '-'}</td>
                  <td>{e.hireDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <p className="muted" style={{ marginTop: 12, fontSize: 12 }}>
          총 {rows.length}명{dept || keyword ? ` (전체 ${employees.length}명 중)` : ''}
        </p>
      </div>
    </div>
  )
}

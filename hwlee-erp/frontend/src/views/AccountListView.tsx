import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listAccounts } from '../api/accounts'
import { ACCOUNT_TYPE, NORMAL_SIDE } from '../domain/status'
import type { AccountType } from '../types/api'

// 계정과목(Chart of Accounts) 조회 — 읽기 전용.
// 전표의 '계정'이 뭔지 보여주는 참조 화면이라 목록 하나로 끝낸다.
export default function AccountListView() {
  // '' = 전체(필터 없음). 서버 enum 과 빈 문자열만 허용해 오타를 막는다.
  const [type, setType] = useState<AccountType | ''>('')

  const { data: accounts = [], isLoading, isError, error } = useQuery({
    queryKey: ['accounts'],
    queryFn: listAccounts,
  })

  const rows = type ? accounts.filter((a) => a.type === type) : accounts

  return (
    <div className="container">
      <div className="page-head">
        <h1>계정과목</h1>
      </div>

      <div className="toolbar">
        <div>
          <label>계정 유형</label>
          <select value={type} onChange={(e) => setType(e.target.value as AccountType | '')}>
            <option value="">전체</option>
            {Object.entries(ACCOUNT_TYPE).map(([code, label]) => (
              <option key={code} value={code}>
                {label}
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
        ) : (
          <table>
            <thead>
              <tr>
                <th>코드</th>
                <th>계정명</th>
                <th>유형</th>
                <th>정상잔액</th>
                <th>상위</th>
                <th>분개 가능</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((a) => (
                <tr key={a.id}>
                  <td className="mono">{a.code}</td>
                  {/* 상위 분류 계정은 굵게 — 계정과목 트리의 뼈대가 눈에 들어오게. */}
                  <td style={{ fontWeight: a.postable ? 400 : 700 }}>{a.name}</td>
                  <td>{ACCOUNT_TYPE[a.type] || a.type}</td>
                  <td className="muted">{NORMAL_SIDE[a.normalSide] || a.normalSide}</td>
                  <td className="mono muted">{a.parentCode || '-'}</td>
                  <td>
                    {a.postable ? (
                      '가능'
                    ) : (
                      <span className="muted">분류 계정</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <p className="muted" style={{ marginTop: 12, fontSize: 12 }}>
          총 {rows.length}개 · 전표는 '분개 가능'한 말단 계정에만 끊을 수 있습니다.
        </p>
      </div>
    </div>
  )
}

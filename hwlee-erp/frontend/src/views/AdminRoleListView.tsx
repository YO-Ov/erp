import { useQuery } from '@tanstack/react-query'
import { listAdminRoles } from '../api/admin'
import AdminTabs from '../components/AdminTabs'

// 역할·권한 조회 — 읽기 전용. 각 역할이 가진 권한 코드를 보여준다.
export default function AdminRoleListView() {
  const { data: roles = [], isLoading, isError, error } = useQuery({
    queryKey: ['admin-roles'],
    queryFn: listAdminRoles,
  })

  return (
    <div className="container">
      <div className="page-head">
        <h1>관리자</h1>
      </div>
      <AdminTabs />

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
                <th>역할명</th>
                <th className="num">권한 수</th>
                <th>권한</th>
              </tr>
            </thead>
            <tbody>
              {roles.map((r) => (
                <tr key={r.id}>
                  <td className="mono">{r.code}</td>
                  <td>{r.name}</td>
                  <td className="num mono">{r.permissions.length}</td>
                  <td>
                    {r.permissions.length === 0 ? (
                      <span className="muted">-</span>
                    ) : (
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                        {r.permissions.map((p) => (
                          <span key={p} className="badge mono" style={{ fontSize: 11 }}>
                            {p}
                          </span>
                        ))}
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <p className="muted" style={{ marginTop: 12, fontSize: 12 }}>
          역할은 시드로 고정입니다. 사용자별 역할 부여는 '사용자' 탭에서 조정합니다.
        </p>
      </div>
    </div>
  )
}

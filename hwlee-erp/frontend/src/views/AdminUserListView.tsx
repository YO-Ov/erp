import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listAdminUsers, listAdminRoles, updateUserRoles } from '../api/admin'
import AdminTabs from '../components/AdminTabs'
import type { AdminUser } from '../types/api'

// 사용자 목록 + 역할 편집. ADMIN 전용.
// 각 사용자의 역할을 체크박스로 통째 교체한다(부서 시드로 만들어진 역할을 수동 조정).
export default function AdminUserListView() {
  const queryClient = useQueryClient()

  const { data: users = [], isLoading, isError, error } = useQuery({
    queryKey: ['admin-users'],
    queryFn: listAdminUsers,
  })
  const { data: roles = [] } = useQuery({
    queryKey: ['admin-roles'],
    queryFn: listAdminRoles,
  })

  // 편집 중인 사용자 id + 선택된 역할 id 집합.
  const [editingId, setEditingId] = useState<number | null>(null)
  const [selected, setSelected] = useState<Set<number>>(new Set())

  function startEdit(u: AdminUser) {
    setEditingId(u.id)
    setSelected(new Set(u.roles.map((r) => r.id)))
  }
  function cancelEdit() {
    setEditingId(null)
    setSelected(new Set())
  }
  function toggle(roleId: number) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(roleId)) next.delete(roleId)
      else next.add(roleId)
      return next
    })
  }

  const mutation = useMutation({
    mutationFn: (userId: number) => updateUserRoles(userId, [...selected]),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
      cancelEdit()
    },
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
                <th>계정</th>
                <th>이름</th>
                <th>상태</th>
                <th>역할</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => {
                const editing = editingId === u.id
                return (
                  <tr key={u.id}>
                    <td className="mono">{u.username}</td>
                    <td>{u.employeeName || '-'}</td>
                    <td>
                      {u.enabled ? (
                        <span className="muted">활성</span>
                      ) : (
                        <span className="error">비활성</span>
                      )}
                      {u.accountLocked && <span className="error"> · 잠김</span>}
                    </td>
                    <td>
                      {editing ? (
                        // 편집 모드: 전체 역할 체크박스.
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
                          {roles.map((r) => (
                            <label key={r.id} style={{ fontSize: 13, whiteSpace: 'nowrap' }}>
                              <input
                                type="checkbox"
                                checked={selected.has(r.id)}
                                onChange={() => toggle(r.id)}
                                style={{ marginRight: 4 }}
                              />
                              {r.name}
                              <span className="muted mono" style={{ fontSize: 11 }}>
                                {' '}
                                {r.code}
                              </span>
                            </label>
                          ))}
                        </div>
                      ) : u.roles.length === 0 ? (
                        <span className="muted">역할 없음</span>
                      ) : (
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                          {u.roles.map((r) => (
                            <span key={r.id} className="badge" title={r.code}>
                              {r.name}
                            </span>
                          ))}
                        </div>
                      )}
                    </td>
                    <td>
                      {editing ? (
                        <div className="actions">
                          <button
                            className="sm primary"
                            disabled={mutation.isPending}
                            onClick={() => mutation.mutate(u.id)}
                          >
                            저장
                          </button>
                          <button className="sm" onClick={cancelEdit}>
                            취소
                          </button>
                        </div>
                      ) : (
                        <button className="sm" onClick={() => startEdit(u)}>
                          역할 편집
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
        {mutation.isError && <p className="error">{mutation.error.message}</p>}
        <p className="muted" style={{ marginTop: 12, fontSize: 12 }}>
          총 {users.length}명 · 역할을 바꾸면 그 사용자의 권한이 즉시 반영됩니다.
        </p>
      </div>
    </div>
  )
}

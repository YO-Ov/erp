import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  addCustomerContact,
  deleteCustomerContact,
  getCustomerContacts,
  updateCustomerContact,
} from '../api/customers'
import type { CustomerContact, CustomerContactRequest } from '../types/api'

// 고객 담당자(연락처) 목록 + 인라인 추가/수정/삭제.
// 한 고객에 여러 담당자(구매·경리·현장 등)를 두고, 대표 담당자는 1명(서버가 보장).
// 조회는 누구나, 편집은 SALES/ADMIN(canEdit 로 버튼 노출 제어 — 최종 강제는 백엔드).

type EditKey = 'new' | number

const EMPTY_FORM = { name: '', position: '', phone: '', email: '', primary: false }

export default function CustomerContactsPanel({
  customerId,
  canEdit,
}: {
  customerId: number
  canEdit: boolean
}) {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<EditKey | null>(null)
  const [form, setForm] = useState(EMPTY_FORM)

  const { data: contacts = [], isLoading, isError, error } = useQuery({
    queryKey: ['customer-contacts', customerId],
    queryFn: () => getCustomerContacts(customerId),
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['customer-contacts', customerId] })
  }

  const saveMutation = useMutation({
    mutationFn: () => {
      const body: CustomerContactRequest = {
        name: form.name.trim(),
        position: form.position.trim() || null,
        phone: form.phone.trim() || null,
        email: form.email.trim() || null,
        primary: form.primary,
      }
      return editing === 'new'
        ? addCustomerContact(customerId, body)
        : updateCustomerContact(customerId, editing as number, body)
    },
    onSuccess: () => {
      invalidate()
      closeForm()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (contactId: number) => deleteCustomerContact(customerId, contactId),
    onSuccess: invalidate,
  })

  function openNew() {
    setForm(EMPTY_FORM)
    setEditing('new')
  }

  function openEdit(c: CustomerContact) {
    setForm({
      name: c.name,
      position: c.position ?? '',
      phone: c.phone ?? '',
      email: c.email ?? '',
      primary: c.primary,
    })
    setEditing(c.id)
  }

  function closeForm() {
    setEditing(null)
    setForm(EMPTY_FORM)
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    saveMutation.mutate()
  }

  function onDelete(c: CustomerContact) {
    if (!window.confirm(`담당자 "${c.name}" 을(를) 삭제하시겠습니까?`)) return
    deleteMutation.mutate(c.id)
  }

  return (
    <>
      <div
        className="section-title"
        style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}
      >
        <span>담당자</span>
        {canEdit && editing === null && (
          <button className="sm" onClick={openNew}>
            + 담당자 추가
          </button>
        )}
      </div>

      <div className="panel">
        {isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : contacts.length === 0 ? (
          <p className="muted">등록된 담당자가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>이름</th>
                <th>부서·직책</th>
                <th>전화</th>
                <th>이메일</th>
                {canEdit && <th style={{ width: 120 }}>관리</th>}
              </tr>
            </thead>
            <tbody>
              {contacts.map((c) => (
                <tr key={c.id}>
                  <td>
                    {c.name}
                    {c.primary && (
                      <span className="badge tone-active" style={{ marginLeft: 6 }}>
                        대표
                      </span>
                    )}
                  </td>
                  <td>{c.position || '-'}</td>
                  <td className="mono">{c.phone || '-'}</td>
                  <td className="mono muted">{c.email || '-'}</td>
                  {canEdit && (
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button className="sm" onClick={() => openEdit(c)}>
                          수정
                        </button>
                        <button
                          className="sm danger"
                          onClick={() => onDelete(c)}
                          disabled={deleteMutation.isPending}
                        >
                          삭제
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {deleteMutation.isError && (
          <p className="error" style={{ marginTop: 10 }}>
            {deleteMutation.error.message}
          </p>
        )}

        {canEdit && editing !== null && (
          <form onSubmit={onSubmit} style={{ marginTop: contacts.length ? 16 : 0 }}>
            <div style={{ marginBottom: 8, fontWeight: 600 }}>
              {editing === 'new' ? '담당자 추가' : '담당자 수정'}
            </div>
            <div className="row">
              <div className="field" style={{ flex: 1 }}>
                <label>이름 *</label>
                <input
                  type="text"
                  value={form.name}
                  maxLength={100}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  required
                />
              </div>
              <div className="field" style={{ flex: 1 }}>
                <label>부서·직책</label>
                <input
                  type="text"
                  value={form.position}
                  maxLength={100}
                  placeholder="예: 구매팀 과장"
                  onChange={(e) => setForm({ ...form, position: e.target.value })}
                />
              </div>
            </div>
            <div className="row">
              <div className="field" style={{ flex: 1 }}>
                <label>전화</label>
                <input
                  type="text"
                  value={form.phone}
                  maxLength={30}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                />
              </div>
              <div className="field" style={{ flex: 1 }}>
                <label>이메일</label>
                <input
                  type="email"
                  value={form.email}
                  maxLength={200}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                />
              </div>
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
              <input
                type="checkbox"
                checked={form.primary}
                onChange={(e) => setForm({ ...form, primary: e.target.checked })}
              />
              대표 담당자로 지정 (기존 대표는 자동 해제)
            </label>

            <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
              <button
                className="primary"
                type="submit"
                disabled={!form.name.trim() || saveMutation.isPending}
              >
                {saveMutation.isPending ? '저장 중…' : '저장'}
              </button>
              <button type="button" onClick={closeForm}>
                취소
              </button>
            </div>

            {saveMutation.isError && (
              <p className="error" style={{ marginTop: 12 }}>
                {saveMutation.error.message}
              </p>
            )}
          </form>
        )}
      </div>
    </>
  )
}

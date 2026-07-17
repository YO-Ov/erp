import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getApproval, approvalAction } from '../api/approvals'
import {
  APPROVAL_STATUS,
  APPROVAL_STEP_STATUS,
  APPROVAL_STEP_TYPE,
  approvalActions,
  approvalDocLink,
  approvalReason,
  APPROVAL_ACTION_LABEL,
  formatMoney,
} from '../domain/status'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'
import type { ApprovalActionCode } from '../domain/status'

// 반려·반송은 사유가 실무상 필수다(왜 돌려보냈는지 상신자가 알아야 함).
const NEEDS_COMMENT = ['reject', 'return']

export default function ApprovalDetailView() {
  // 라우트에 :id 가 있어 항상 존재하지만 타입상 undefined 가능 — 빈 문자열로 좁힌다.
  const { id = '' } = useParams()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const [comment, setComment] = useState('')

  const { data: apv, isLoading, isError, error } = useQuery({
    queryKey: ['approval', id],
    queryFn: () => getApproval(id),
  })

  const mutation = useMutation({
    mutationFn: (action: ApprovalActionCode) => approvalAction(id, action, comment),
    onSuccess: () => {
      setComment('')
      queryClient.invalidateQueries({ queryKey: ['approval', id] })
      queryClient.invalidateQueries({ queryKey: ['approvals'] })
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      // 승인되면 원본 문서가 자동으로 진행된다(전표 전기·발주 확정 등) — 문서 캐시도 비운다.
      queryClient.invalidateQueries({ queryKey: ['journal-entries'] })
      queryClient.invalidateQueries({ queryKey: ['payments'] })
      queryClient.invalidateQueries({ queryKey: ['purchase-orders'] })
      queryClient.invalidateQueries({ queryKey: ['quotations'] })
    },
  })

  function onAction(action: ApprovalActionCode) {
    if (NEEDS_COMMENT.includes(action) && !comment.trim()) {
      window.alert(`${APPROVAL_ACTION_LABEL[action]} 사유를 입력해 주세요.`)
      return
    }
    // 되돌리기 어려운 액션만 한 번 더 확인한다(승인·재상신은 바로 진행).
    const CONFIRM_MSG: Partial<Record<ApprovalActionCode, string>> = {
      withdraw: '이 상신을 회수하시겠습니까?',
      reject: '반려하면 이 문서는 종결됩니다. 계속하시겠습니까?',
      return: '상신자에게 반송하시겠습니까? 상신자가 고쳐서 재상신할 수 있습니다.',
    }
    const confirmMsg = CONFIRM_MSG[action]
    if (confirmMsg && !window.confirm(confirmMsg)) return
    mutation.mutate(action)
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  // 로딩·에러가 아니면 데이터가 있지만 TS는 모른다 — 아래에서 그냥 쓰기 위해 좁힌다.
  if (!apv) return null

  // ProtectedRoute 를 지나왔으므로 user 는 있다.
  const actions = approvalActions(apv, user?.username ?? '')
  const docLink = approvalDocLink(apv.docType, apv.refId)
  const reason = approvalReason(apv)
  const canComment = actions.some((a) => ['approve', 'reject', 'return'].includes(a))

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/approvals" className="muted">
            ← 전자결재
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{apv.number}</span>{' '}
            <StatusBadge map={APPROVAL_STATUS} status={apv.status} />
          </h1>
        </div>
        <div className="actions">
          {actions.map((a) => (
            <button
              key={a}
              className={a === 'approve' || a === 'resubmit' ? 'primary' : a === 'reject' ? 'danger' : ''}
              disabled={mutation.isPending}
              onClick={() => onAction(a)}
            >
              {APPROVAL_ACTION_LABEL[a]}
            </button>
          ))}
        </div>
      </div>

      {mutation.isError && <p className="error">{mutation.error.message}</p>}

      <div className="panel">
        <div className="info-grid">
          <div>
            <div className="k">문서 종류</div>
            <div className="v">{apv.docTypeLabel}</div>
          </div>
          <div>
            <div className="k">원본 문서</div>
            <div className="v">
              {docLink ? (
                <Link to={docLink} className="mono">
                  {apv.refNo}
                </Link>
              ) : (
                // 여신 상향처럼 React 화면이 아직 없는 문서 종류.
                <span className="mono muted">{apv.refNo} (화면 없음)</span>
              )}
            </div>
          </div>
          <div>
            <div className="k">상신자</div>
            <div className="v">{apv.requester}</div>
          </div>
          <div>
            <div className="k">상신일시</div>
            <div className="v">{apv.requestedAt}</div>
          </div>
          <div>
            <div className="k">금액</div>
            <div className="v mono">{formatMoney(apv.amount)}</div>
          </div>
          <div>
            <div className="k">처리일시</div>
            <div className="v">{apv.decidedAt || '-'}</div>
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            <div className="k">제목</div>
            <div className="v">{apv.title}</div>
          </div>
          {reason && (
            <div style={{ gridColumn: '1 / -1' }}>
              <div className="k">반려/반송 사유</div>
              <div className="v error">{reason}</div>
            </div>
          )}
        </div>
      </div>

      <div className="section-title">결재선</div>
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th className="num" style={{ width: 60 }}>단계</th>
              <th>유형</th>
              <th>결재자</th>
              <th>부서</th>
              <th>상태</th>
              <th>처리일시</th>
              <th>의견</th>
            </tr>
          </thead>
          <tbody>
            {(apv.steps || []).map((s) => {
              // 지금 진행 중인 단계를 강조 — 결재가 어디까지 왔는지 한눈에.
              const isCurrent = apv.status === 'PENDING' && s.stepNo === apv.currentStep
              return (
                <tr key={s.stepNo} style={isCurrent ? { fontWeight: 600 } : undefined}>
                  <td className="num mono">
                    {s.stepNo}
                    {isCurrent && <span style={{ color: 'var(--accent)' }}> ●</span>}
                  </td>
                  <td>{APPROVAL_STEP_TYPE[s.type] || s.type}</td>
                  <td>
                    {s.approverName}
                    <span className="muted mono" style={{ marginLeft: 6, fontSize: 12 }}>
                      {s.approver}
                    </span>
                  </td>
                  <td className="muted">{s.deptName}</td>
                  <td>
                    {/* 참조 단계는 처리 대상이 아니라 계속 PENDING(=미열람)으로 남는다. */}
                    {s.type === 'REFERENCE' && s.status === 'PENDING' ? (
                      <span className="muted">열람 대기</span>
                    ) : (
                      <StatusBadge map={APPROVAL_STEP_STATUS} status={s.status} />
                    )}
                  </td>
                  <td className="muted">{s.decidedAt || '-'}</td>
                  <td>{s.comment || '-'}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {canComment && (
        <div className="panel" style={{ marginTop: 14 }}>
          <div className="field">
            <label>의견 {actions.includes('reject') && '(반려·반송 시 필수)'}</label>
            <input
              type="text"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="예) 금액 근거 자료를 첨부해 주세요"
              style={{ width: '100%' }}
            />
          </div>
        </div>
      )}

      {apv.status === 'APPROVED' && (
        <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
          승인 완료 — 원본 문서가 자동으로 진행되었습니다(전표·지급은 전기, 발주는 확정).
        </p>
      )}
    </div>
  )
}

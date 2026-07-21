import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { deleteCustomer, getCustomer } from '../api/customers'
import { getPendingCreditRequest } from '../api/credit'
import { MASTER_STATUS, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'
import { useAuth } from '../auth/AuthContext'

// 고객 상세 — 기본정보 + 여신한도(표시만). 여신한도 변경은 재무 권한이라
// 영업은 '여신 상향 요청'을 올려 재무 승인을 받는다(권한 분리).
export default function CustomerDetailView() {
  const { id = '' } = useParams()
  const customerId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { hasRole } = useAuth()
  const canDelete = hasRole('ADMIN')

  const { data: c, isLoading, isError, error } = useQuery({
    queryKey: ['customer', id],
    queryFn: () => getCustomer(id),
  })

  // 검토 중인 여신 요청이 있으면 "상향 요청" 대신 "검토 중"으로 안내(중복 신청 방지).
  const { data: pending } = useQuery({
    queryKey: ['customer-credit-pending', id],
    queryFn: () => getPendingCreditRequest(customerId),
    enabled: !!c,
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteCustomer(customerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      navigate('/customers')
    },
  })

  function onDelete() {
    if (
      !window.confirm('이 고객을 삭제하시겠습니까? (과거 거래는 보존되며 새 거래에서만 숨겨집니다)')
    )
      return
    deleteMutation.mutate()
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  if (!c) return null

  const isCash = c.creditLimit === 0

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/customers" className="muted">
            ← 고객 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            {c.name} <span className="muted mono">{c.code}</span>
          </h1>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Link to={`/customers/${c.id}/edit`}>
            <button className="primary">수정</button>
          </Link>
          {canDelete && (
            <button className="danger" onClick={onDelete} disabled={deleteMutation.isPending}>
              {deleteMutation.isPending ? '삭제 중…' : '삭제'}
            </button>
          )}
        </div>
      </div>

      <div className="panel">
        <div className="info-grid">
          <div>
            <div className="k">코드</div>
            <div className="v mono">{c.code}</div>
          </div>
          <div>
            <div className="k">사업자번호</div>
            <div className="v mono">{c.businessNo || '-'}</div>
          </div>
          <div>
            <div className="k">주소</div>
            <div className="v">{c.address || '-'}</div>
          </div>
          <div>
            <div className="k">결제조건</div>
            <div className="v">{c.paymentTerms}</div>
          </div>
          <div>
            <div className="k">상태</div>
            <div className="v">
              <StatusBadge map={MASTER_STATUS} status={c.status} />
            </div>
          </div>
        </div>
      </div>

      <div className="section-title">여신한도</div>
      <div className="panel">
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, flexWrap: 'wrap' }}>
          <strong style={{ fontSize: 22 }}>{formatMoney(c.creditLimit)}</strong>
          {isCash && <span className="badge tone-muted">현금거래 (한도 없음)</span>}
          {pending ? (
            <Link to={`/credit-requests/${pending.id}`} className="badge tone-warn">
              여신 검토 중 · {pending.number}
            </Link>
          ) : (
            <Link to={`/credit-requests/new?customerId=${c.id}`}>
              <button className="sm">여신 상향 요청</button>
            </Link>
          )}
        </div>
        <p className="muted" style={{ marginTop: 10, fontSize: 12 }}>
          여신한도는 <strong>재무(여신) 권한</strong>입니다. 영업은 변경할 수 없으며, 상향이
          필요하면 <strong>여신 상향 요청</strong>을 올려 재무 승인을 받습니다.
        </p>
      </div>

      {deleteMutation.isError && (
        <p className="error" style={{ marginTop: 12 }}>
          {deleteMutation.error.message}
        </p>
      )}
    </div>
  )
}

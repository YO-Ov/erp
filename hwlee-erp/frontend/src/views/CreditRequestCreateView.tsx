import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { createCreditRequest } from '../api/credit'
import { getCreditStatus } from '../api/salesOrders'
import { listCustomers } from '../api/masters'
import { formatMoney } from '../domain/status'

// 여신 상향 요청 작성 — 고객·요청한도·사유. 생성 시 서버가 전자결재를 상신한다.
export default function CreditRequestCreateView() {
  const navigate = useNavigate()

  // 고객 상세의 '여신 상향 요청' 버튼에서 넘어오면 해당 고객을 미리 선택한다.
  const [searchParams] = useSearchParams()
  const [customerId, setCustomerId] = useState(searchParams.get('customerId') || '')
  const [requestedLimit, setRequestedLimit] = useState('')
  const [reason, setReason] = useState('')

  const { data: customers = [] } = useQuery({
    queryKey: ['customers'],
    queryFn: () => listCustomers(),
  })

  // 고객을 고르면 현재 여신 현황(한도·사용)을 보여준다 — 요청 한도 판단 근거.
  const { data: credit } = useQuery({
    queryKey: ['credit-status', customerId],
    queryFn: () => getCreditStatus(Number(customerId)),
    enabled: !!customerId,
  })

  const mutation = useMutation({
    mutationFn: () =>
      createCreditRequest({
        customerId: Number(customerId),
        requestedLimit: Number(requestedLimit),
        reason: reason.trim() || null,
      }),
    onSuccess: (saved) => navigate(`/credit-requests/${saved.id}`),
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    mutation.mutate()
  }

  const canSubmit = customerId && Number(requestedLimit) > 0

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/credit-requests" className="muted">
            ← 여신 상향 요청 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>여신 상향 요청</h1>
        </div>
      </div>

      <form onSubmit={onSubmit}>
        <div className="panel">
          <div className="row">
            <div className="field" style={{ flex: 2 }}>
              <label>고객 *</label>
              <select
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                style={{ width: '100%' }}
                required
              >
                <option value="">고객 선택</option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.code})
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>요청 한도 *</label>
              <input
                type="number"
                min="1"
                step="1"
                value={requestedLimit}
                onChange={(e) => setRequestedLimit(e.target.value)}
                style={{ width: '100%', textAlign: 'right' }}
                required
              />
            </div>
          </div>

          {credit && (
            <div className="info-grid" style={{ marginTop: 8 }}>
              <div>
                <div className="k">현재 한도</div>
                <div className="v mono">{formatMoney(credit.creditLimit)}</div>
              </div>
              <div>
                <div className="k">사용중(수주+채권)</div>
                <div className="v mono">{formatMoney(credit.used)}</div>
              </div>
              <div>
                <div className="k">잔여</div>
                <div className="v mono">{formatMoney(credit.remaining)}</div>
              </div>
            </div>
          )}

          <div className="field" style={{ marginTop: 8 }}>
            <label>사유</label>
            <input
              type="text"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="예) 대량 발주 예정으로 한도 상향 필요"
              style={{ width: '100%' }}
            />
          </div>

          <p className="muted" style={{ fontSize: 12, marginTop: 8 }}>
            요청하면 전자결재로 상신됩니다. 재무팀장이 결재함에서 승인하면 고객 한도에 반영됩니다.
          </p>
        </div>

        {mutation.isError && (
          <p className="error" style={{ marginTop: 12 }}>
            {mutation.error.message}
          </p>
        )}

        <div className="actions" style={{ marginTop: 16 }}>
          <button className="primary" type="submit" disabled={!canSubmit || mutation.isPending}>
            {mutation.isPending ? '상신 중…' : '요청 상신'}
          </button>
          <Link to="/credit-requests">
            <button type="button">취소</button>
          </Link>
        </div>
      </form>
    </div>
  )
}

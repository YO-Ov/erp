import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { createPayment, createPaymentDraft } from '../api/payments'
import { listCustomers, listVendors } from '../api/masters'
import { formatMoney } from '../domain/status'
import type { Payment, PaymentCreateRequest, PaymentType } from '../types/api'

/** 저장 방식 — 즉시 전기(POSTED)냐, 결재용 초안(DRAFT)이냐. */
type SaveMode = 'post' | 'draft'

function today() {
  return new Date().toISOString().slice(0, 10)
}

// 입금/출금 등록.
//  - 입금(RECEIPT)      → 고객 필수. 결재 없이 바로 전기한다.
//  - 출금(DISBURSEMENT) → 공급처 필수. 회사 돈이 나가므로 초안→결재 상신 경로가 있다.
export default function PaymentCreateView() {
  const navigate = useNavigate()

  const [type, setType] = useState<PaymentType>('RECEIPT')
  const [partyId, setPartyId] = useState('')
  const [amount, setAmount] = useState('')
  const [paymentDate, setPaymentDate] = useState(today())
  const [description, setDescription] = useState('')

  const isReceipt = type === 'RECEIPT'

  const { data: customers = [] } = useQuery({
    queryKey: ['customers'],
    queryFn: () => listCustomers(),
    enabled: isReceipt,
  })
  const { data: vendors = [] } = useQuery({
    queryKey: ['vendors'],
    queryFn: () => listVendors(),
    enabled: !isReceipt,
  })
  const parties = isReceipt ? customers : vendors

  const mutation = useMutation<Payment, Error, { body: PaymentCreateRequest; mode: SaveMode }>({
    mutationFn: ({ body, mode }) =>
      mode === 'post' ? createPayment(body) : createPaymentDraft(body),
    onSuccess: (saved) => navigate(`/payments/${saved.id}`),
  })

  // 구분을 바꾸면 거래처는 성격이 달라지므로 선택을 비운다.
  function onTypeChange(next: PaymentType) {
    setType(next)
    setPartyId('')
  }

  function buildBody(): PaymentCreateRequest {
    // 백엔드는 반대편 party 가 채워져 있으면 400 으로 거부한다 — 해당 필드만 싣는다.
    // PaymentCreateRequest 가 유니온이라, 두 필드를 같이 넣으면 컴파일에서 걸린다.
    const common = {
      amount: Number(amount),
      paymentDate,
      description: description.trim() || null,
    }
    return type === 'RECEIPT'
      ? { type: 'RECEIPT', customerId: Number(partyId), ...common }
      : { type: 'DISBURSEMENT', vendorId: Number(partyId), ...common }
  }

  function onSubmit(e: FormEvent, mode: SaveMode) {
    e.preventDefault()
    mutation.mutate({ body: buildBody(), mode })
  }

  const canSubmit = partyId && Number(amount) > 0 && paymentDate

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/payments" className="muted">
            ← 입금/출금 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>입출금 등록</h1>
        </div>
      </div>

      <form onSubmit={(e) => onSubmit(e, 'post')}>
        <div className="panel">
          <div className="row">
            <div className="field">
              <label>구분 *</label>
              <select
                value={type}
                onChange={(e) => onTypeChange(e.target.value as PaymentType)}
                style={{ width: '100%' }}
              >
                <option value="RECEIPT">입금 (고객 → 우리)</option>
                <option value="DISBURSEMENT">출금 (우리 → 공급처)</option>
              </select>
            </div>
            <div className="field" style={{ flex: 2 }}>
              <label>{isReceipt ? '고객 *' : '공급처 *'}</label>
              <select
                value={partyId}
                onChange={(e) => setPartyId(e.target.value)}
                style={{ width: '100%' }}
                required
              >
                <option value="">{isReceipt ? '고객 선택' : '공급처 선택'}</option>
                {parties.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.code})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="row">
            <div className="field">
              <label>금액 *</label>
              <input
                type="number"
                min="1"
                step="1"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                style={{ width: '100%', textAlign: 'right' }}
                required
              />
              {Number(amount) > 0 && (
                <span className="muted mono" style={{ fontSize: 12 }}>
                  {formatMoney(Number(amount))} 원
                </span>
              )}
            </div>
            <div className="field">
              <label>거래일 *</label>
              <input
                type="date"
                value={paymentDate}
                onChange={(e) => setPaymentDate(e.target.value)}
                style={{ width: '100%' }}
                required
              />
            </div>
            <div className="field" style={{ flex: 2 }}>
              <label>적요</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="예) 3월 매출대금 회수"
                style={{ width: '100%' }}
              />
            </div>
          </div>

          <p className="muted" style={{ fontSize: 12, marginTop: 8 }}>
            {isReceipt
              ? '전기하면 차)현금 / 대)매출채권 분개가 자동 생성됩니다.'
              : '전기하면 차)매입채무 / 대)현금 분개가 자동 생성됩니다.'}
          </p>
        </div>

        {mutation.isError && (
          <p className="error" style={{ marginTop: 12 }}>
            {mutation.error.message}
          </p>
        )}

        <div className="actions" style={{ marginTop: 16 }}>
          <button
            className="primary"
            type="submit"
            disabled={!canSubmit || mutation.isPending}
          >
            {mutation.isPending ? '저장 중…' : '등록(즉시 전기)'}
          </button>
          {/* 결재 상신은 출금만 — 입금 초안은 상신할 수 없다(백엔드 규칙). */}
          {!isReceipt && (
            <button
              type="button"
              disabled={!canSubmit || mutation.isPending}
              onClick={(e) => onSubmit(e, 'draft')}
            >
              초안 저장(결재용)
            </button>
          )}
          <Link to="/payments">
            <button type="button">취소</button>
          </Link>
        </div>
      </form>
    </div>
  )
}

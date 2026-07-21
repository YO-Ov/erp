import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { createCustomer, getCustomer, updateCustomer } from '../api/customers'
import type { PaymentTerms } from '../types/api'

const PAYMENT_TERMS: PaymentTerms[] = ['NET30', 'NET60', 'COD', 'PREPAID']

// 고객 등록/수정 겸용. 수정 모드에선 사업자번호(외부 식별자)를 바꿀 수 없다.
// 여신한도는 이 화면에서 다루지 않는다 — 재무 여신 승인으로만 부여/상향(권한 분리).
export default function CustomerFormView() {
  const { id } = useParams()
  const isEdit = id != null
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [businessNo, setBusinessNo] = useState('')
  const [address, setAddress] = useState('')
  const [paymentTerms, setPaymentTerms] = useState<PaymentTerms>('NET30')

  // 수정 모드: 기존 값을 불러와 폼을 채운다.
  const { data: existing } = useQuery({
    queryKey: ['customer', id],
    queryFn: () => getCustomer(id!),
    enabled: isEdit,
  })

  useEffect(() => {
    if (existing) {
      setName(existing.name)
      setBusinessNo(existing.businessNo || '')
      setAddress(existing.address || '')
      setPaymentTerms(existing.paymentTerms)
    }
  }, [existing])

  const mutation = useMutation({
    mutationFn: () => {
      const trimmedAddress = address.trim() || null
      if (isEdit) {
        return updateCustomer(id!, { name: name.trim(), address: trimmedAddress, paymentTerms })
      }
      return createCustomer({
        name: name.trim(),
        businessNo: businessNo.trim(),
        address: trimmedAddress,
        paymentTerms,
      })
    },
    onSuccess: (saved) => navigate(`/customers/${saved.id}`),
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    mutation.mutate()
  }

  const canSubmit = name.trim() && (isEdit || businessNo.trim())

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to={isEdit ? `/customers/${id}` : '/customers'} className="muted">
            ← {isEdit ? '고객 상세' : '고객 목록'}
          </Link>
          <h1 style={{ marginTop: 6 }}>
            {isEdit ? `고객 수정${existing ? ` · ${existing.code}` : ''}` : '신규 고객'}
          </h1>
        </div>
      </div>

      <form onSubmit={onSubmit}>
        <div className="panel">
          <div className="row">
            <div className="field" style={{ flex: 1 }}>
              <label>고객명 *</label>
              <input
                type="text"
                value={name}
                maxLength={200}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label>사업자번호 *</label>
              <input
                type="text"
                value={businessNo}
                maxLength={20}
                placeholder="000-00-00000"
                onChange={(e) => setBusinessNo(e.target.value)}
                readOnly={isEdit}
                required={!isEdit}
              />
              <p className="muted" style={{ fontSize: 12, marginTop: 4 }}>
                등록 후에는 변경할 수 없습니다(외부 식별자).
              </p>
            </div>
          </div>

          <div className="row">
            <div className="field" style={{ flex: 2 }}>
              <label>주소</label>
              <input
                type="text"
                value={address}
                maxLength={500}
                onChange={(e) => setAddress(e.target.value)}
              />
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label>결제조건 *</label>
              <select
                value={paymentTerms}
                onChange={(e) => setPaymentTerms(e.target.value as PaymentTerms)}
              >
                {PAYMENT_TERMS.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {!isEdit && (
            <p className="muted" style={{ fontSize: 12, marginTop: 12 }}>
              신규 고객은 <strong>여신한도 0(현금거래)</strong>으로 시작합니다. 외상 한도가
              필요하면 등록 후 <strong>여신 상향 요청</strong>을 올려 재무 승인을 받으세요.
            </p>
          )}
        </div>

        <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
          <button className="primary" type="submit" disabled={!canSubmit || mutation.isPending}>
            {mutation.isPending ? '저장 중…' : '저장'}
          </button>
          <Link to={isEdit ? `/customers/${id}` : '/customers'}>
            <button type="button">취소</button>
          </Link>
        </div>

        {mutation.isError && (
          <p className="error" style={{ marginTop: 12 }}>
            {mutation.error.message}
          </p>
        )}
      </form>
    </div>
  )
}

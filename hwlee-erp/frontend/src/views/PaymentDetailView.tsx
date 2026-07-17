import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getPayment, submitPaymentApproval } from '../api/payments'
import { searchJournalEntries } from '../api/journalEntries'
import {
  PAYMENT_STATUS,
  PAYMENT_TYPE,
  paymentActions,
  ACTION_LABEL,
  formatMoney,
} from '../domain/status'
import StatusBadge from '../components/StatusBadge'

export default function PaymentDetailView() {
  // 라우트에 :id 가 있어 항상 존재하지만 타입상 undefined 가능 — 빈 문자열로 좁힌다.
  const { id = '' } = useParams()
  const queryClient = useQueryClient()

  const { data: p, isLoading, isError, error } = useQuery({
    queryKey: ['payment', id],
    queryFn: () => getPayment(id),
  })

  // 전기된 입출금은 분개 전표를 만든다(sourceType=PAY, sourceId=이 지급 id).
  // 그 전표를 역으로 찾아 붙여주면 "돈의 이동 → 회계 반영"이 한 화면에서 이어진다.
  const { data: journals } = useQuery({
    queryKey: ['journal-entries', 'by-payment', id],
    queryFn: () => searchJournalEntries({ sourceType: 'PAY', sourceId: id, size: 5 }),
    enabled: !!p && p.status === 'POSTED',
  })
  const linkedEntries = journals?.content || []

  const mutation = useMutation({
    mutationFn: () => submitPaymentApproval(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payment', id] })
      queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
  })

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  // 로딩·에러가 아니면 데이터가 있지만 TS는 모른다 — 아래에서 그냥 쓰기 위해 좁힌다.
  if (!p) return null

  const actions = paymentActions(p.status, p.type)
  const isReceipt = p.type === 'RECEIPT'

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/payments" className="muted">
            ← 입금/출금 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{p.number}</span>{' '}
            <StatusBadge map={PAYMENT_TYPE} status={p.type} />{' '}
            <StatusBadge map={PAYMENT_STATUS} status={p.status} />
          </h1>
        </div>
        <div className="actions">
          {actions.map((a) => (
            <button
              key={a}
              className="primary"
              disabled={mutation.isPending}
              onClick={() => mutation.mutate()}
            >
              {ACTION_LABEL[a]}
            </button>
          ))}
        </div>
      </div>

      {mutation.isError && <p className="error">{mutation.error.message}</p>}

      <div className="panel">
        <div className="info-grid">
          <div>
            <div className="k">구분</div>
            <div className="v">
              {PAYMENT_TYPE[p.type]?.label || p.type}
              <span className="muted" style={{ marginLeft: 6, fontSize: 12 }}>
                {isReceipt ? '고객이 외상값을 갚음' : '거래처에 대금 지급'}
              </span>
            </div>
          </div>
          <div>
            <div className="k">{isReceipt ? '고객' : '공급처'}</div>
            <div className="v mono">
              {isReceipt ? p.customerCode || '-' : p.vendorCode || '-'}
            </div>
          </div>
          <div>
            <div className="k">거래일</div>
            <div className="v">{p.paymentDate}</div>
          </div>
          <div>
            <div className="k">금액</div>
            <div className="v mono">{formatMoney(p.amount)}</div>
          </div>
          <div>
            <div className="k">전기일시</div>
            <div className="v">{p.postedAt || '-'}</div>
          </div>
          {p.description && (
            <div style={{ gridColumn: '1 / -1' }}>
              <div className="k">적요</div>
              <div className="v">{p.description}</div>
            </div>
          )}
        </div>
      </div>

      {p.status === 'POSTED' && (
        <>
          <div className="section-title">연결 전표</div>
          <div className="panel">
            {linkedEntries.length === 0 ? (
              <p className="muted">연결된 전표가 없습니다.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>전표번호</th>
                    <th>전표일</th>
                    <th>적요</th>
                    <th className="num">금액</th>
                  </tr>
                </thead>
                <tbody>
                  {linkedEntries.map((je) => (
                    <tr key={je.id}>
                      <td>
                        <Link to={`/journal-entries/${je.id}`} className="mono">
                          {je.number}
                        </Link>
                      </td>
                      <td>{je.entryDate}</td>
                      <td>{je.description}</td>
                      <td className="num mono">{formatMoney(je.totalDebit)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        작성 {p.createdBy} · {p.createdAt} / 수정 {p.updatedBy} · {p.updatedAt}
      </p>
    </div>
  )
}

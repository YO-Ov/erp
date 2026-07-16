import { Link, useNavigate, useParams } from 'react-router-dom'
// (수정 버튼은 DRAFT 상태에서만 노출)
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getQuotation, quotationAction } from '../api/quotations'
import {
  QUOTATION_STATUS,
  quotationActions,
  ACTION_LABEL,
  formatMoney,
} from '../domain/status'
import StatusBadge from '../components/StatusBadge'

export default function QuotationDetailView() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: q, isLoading, isError, error } = useQuery({
    queryKey: ['quotation', id],
    queryFn: () => getQuotation(id),
  })

  // 상태 전이 액션. 성공하면 이 견적과 목록 캐시를 무효화해 다시 불러온다.
  const mutation = useMutation({
    mutationFn: (action) => quotationAction(id, action),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quotation', id] })
      queryClient.invalidateQueries({ queryKey: ['quotations'] })
    },
  })

  function onAction(action) {
    // '수주 생성'은 상태 전이가 아니라 수주 작성 화면으로 이동.
    if (action === 'convert') {
      navigate(`/sales-orders/new?quotationId=${id}`)
      return
    }
    if (action === 'cancel' && !window.confirm('이 견적을 취소하시겠습니까?')) return
    mutation.mutate(action)
  }

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>

  const actions = quotationActions(q.status)

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/quotations" className="muted">
            ← 견적 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{q.number}</span>{' '}
            <StatusBadge map={QUOTATION_STATUS} status={q.status} />
          </h1>
        </div>
        <div className="actions">
          {q.status === 'DRAFT' && (
            <Link to={`/quotations/${id}/edit`}>
              <button>수정</button>
            </Link>
          )}
          {actions.map((a) => (
              <button
                key={a}
                className={a === 'cancel' ? 'danger' : 'primary'}
                disabled={mutation.isPending}
                onClick={() => onAction(a)}
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
            <div className="k">고객</div>
            <div className="v">
              {q.customerName} <span className="muted mono">{q.customerCode}</span>
            </div>
          </div>
          <div>
            <div className="k">발행일</div>
            <div className="v">{q.issuedDate}</div>
          </div>
          <div>
            <div className="k">유효기한</div>
            <div className="v">{q.validUntil || '-'}</div>
          </div>
          <div>
            <div className="k">합계금액</div>
            <div className="v mono">{formatMoney(q.totalAmount)}</div>
          </div>
        </div>
      </div>

      <div className="section-title">견적 라인</div>
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>품목</th>
              <th className="num">수량</th>
              <th className="num">단가</th>
              <th className="num">금액</th>
            </tr>
          </thead>
          <tbody>
            {(q.lines || []).map((l) => (
              <tr key={l.id}>
                <td>{l.lineNo}</td>
                <td>
                  {l.itemName} <span className="muted mono">{l.itemCode}</span>
                </td>
                <td className="num mono">{formatMoney(l.quantity)}</td>
                <td className="num mono">{formatMoney(l.unitPrice)}</td>
                <td className="num mono">{formatMoney(l.lineTotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        작성 {q.createdBy} · {q.createdAt} / 수정 {q.updatedBy} · {q.updatedAt}
      </p>
    </div>
  )
}

import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getCreditRequest } from '../api/credit'
import { getApprovalForDoc } from '../api/approvals'
import { CREDIT_REQUEST_STATUS, formatMoney } from '../domain/status'
import StatusBadge from '../components/StatusBadge'

export default function CreditRequestDetailView() {
  const { id = '' } = useParams()

  const { data: req, isLoading, isError, error } = useQuery({
    queryKey: ['credit-request', id],
    queryFn: () => getCreditRequest(id),
  })

  // 승인/거부는 전자결재로 이관됐다 — 이 요청의 결재 문서를 역조회해 링크를 붙인다.
  const { data: approval } = useQuery({
    queryKey: ['approval-for', 'CREDIT_LIMIT', id],
    queryFn: () => getApprovalForDoc('CREDIT_LIMIT', Number(id)),
    enabled: !!req,
  })

  if (isLoading) return <div className="container"><p className="muted">불러오는 중…</p></div>
  if (isError) return <div className="container"><p className="error">{error.message}</p></div>
  if (!req) return null

  const delta = req.requestedLimit - req.currentLimit

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <Link to="/credit-requests" className="muted">
            ← 여신 상향 요청 목록
          </Link>
          <h1 style={{ marginTop: 6 }}>
            <span className="mono">{req.number}</span>{' '}
            <StatusBadge map={CREDIT_REQUEST_STATUS} status={req.status} />
          </h1>
        </div>
      </div>

      <div className="panel">
        <div className="info-grid">
          <div>
            <div className="k">고객</div>
            <div className="v">{req.customerName}</div>
          </div>
          <div>
            <div className="k">현재 한도</div>
            <div className="v mono">{formatMoney(req.currentLimit)}</div>
          </div>
          <div>
            <div className="k">요청 한도</div>
            <div className="v mono">
              {formatMoney(req.requestedLimit)}
              {delta !== 0 && (
                <span className="muted" style={{ marginLeft: 6, fontSize: 12 }}>
                  ({delta > 0 ? '+' : ''}
                  {formatMoney(delta)})
                </span>
              )}
            </div>
          </div>
          <div>
            <div className="k">요청자</div>
            <div className="v">{req.requestedBy}</div>
          </div>
          <div>
            <div className="k">결정자 / 결정일시</div>
            <div className="v">
              {req.decidedBy ? `${req.decidedBy} · ${req.decidedAt}` : '-'}
            </div>
          </div>
          {req.reason && (
            <div style={{ gridColumn: '1 / -1' }}>
              <div className="k">사유</div>
              <div className="v">{req.reason}</div>
            </div>
          )}
          {req.decisionNote && (
            <div style={{ gridColumn: '1 / -1' }}>
              <div className="k">결정 의견</div>
              <div className="v">{req.decisionNote}</div>
            </div>
          )}
        </div>
      </div>

      {/* 승인/거부는 여기서 하지 않는다 — 재무팀장이 결재함에서 처리한다. */}
      <div className="panel" style={{ marginTop: 14 }}>
        {req.status === 'PENDING' ? (
          approval ? (
            <p className="muted">
              전자결재로 상신되어 검토 중입니다.{' '}
              <Link to={`/approvals/${approval.id}`} className="mono">
                {approval.number}
              </Link>{' '}
              에서 재무팀장이 승인/거부합니다.
            </p>
          ) : (
            <p className="muted">전자결재로 상신되어 검토 중입니다(결재함에서 처리).</p>
          )
        ) : (
          <p className="muted">
            처리 완료된 요청입니다. 승인 시 고객 여신 한도에 반영됩니다.
          </p>
        )}
      </div>

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        요청 {req.requestedBy} · {req.createdAt}
      </p>
    </div>
  )
}

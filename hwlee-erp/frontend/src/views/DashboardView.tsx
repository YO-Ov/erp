import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getInbox, getOutbox } from '../api/approvals'
import { listNotifications, markNotificationRead, markAllNotificationsRead } from '../api/notifications'
import { getSalesDashboard } from '../api/dashboard'
import {
  SALES_ORDER_STATUS,
  NOTIFICATION_TYPE,
  formatMoney,
  statusMeta,
} from '../domain/status'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'
import type { SalesOrderStatus, SdDashboard } from '../types/api'

function Card({
  label,
  value,
  sub,
  to,
}: {
  label: string
  value: ReactNode
  sub?: string
  /** 주면 카드 전체가 그 화면으로 가는 링크가 된다. */
  to?: string
}) {
  const body = (
    <div className="panel" style={{ height: '100%' }}>
      <div className="k">{label}</div>
      <div style={{ fontSize: 26, fontWeight: 700, marginTop: 6 }}>{value}</div>
      {sub && <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>{sub}</div>}
    </div>
  )
  return to ? <Link to={to}>{body}</Link> : body
}

/** 수주 파이프라인 집계를 상태 타입을 유지한 채 순회한다(Object.entries 는 키를 string 으로 넓힌다). */
function pipelineEntries(pipeline: SdDashboard['pipeline']): [SalesOrderStatus, number][] {
  return Object.entries(pipeline || {}) as [SalesOrderStatus, number][]
}

function CardGrid({ children }: { children: ReactNode }) {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(min(200px, 100%), 1fr))',
        gap: 12,
      }}
    >
      {children}
    </div>
  )
}

// 역할과 무관하게 모두가 쓰는 홈 화면.
//  - 공통: 결재/알림 요약 카드 4개 + 알림
//  - 영업(SALES)이면 SD KPI 섹션이 더 붙는다.
export default function DashboardView() {
  const { user, hasRole } = useAuth()
  const queryClient = useQueryClient()
  const isSales = hasRole('SALES', 'ADMIN')

  const { data: inbox } = useQuery({
    queryKey: ['approvals', 'inbox', { page: 0, size: 5 }],
    queryFn: () => getInbox({ page: 0, size: 5, sort: 'id,desc' }),
  })
  const { data: outbox } = useQuery({
    queryKey: ['approvals', 'outbox', { page: 0, size: 5 }],
    queryFn: () => getOutbox({ page: 0, size: 5, sort: 'id,desc' }),
  })
  const { data: notis } = useQuery({
    queryKey: ['notifications', { page: 0, size: 5 }],
    queryFn: () => listNotifications({ page: 0, size: 5 }),
  })
  const { data: sd } = useQuery({
    queryKey: ['sd-dashboard'],
    queryFn: getSalesDashboard,
    enabled: isSales,
  })

  // id 를 주면 그 알림만, null 이면 전체 읽음 처리.
  const readMutation = useMutation<void, Error, number | null>({
    mutationFn: (id) => (id ? markNotificationRead(id) : markAllNotificationsRead()),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const inboxRows = inbox?.content || []
  const outboxRows = outbox?.content || []
  const notiRows = notis?.content || []

  // 결재함 전체 건수는 서버 페이징의 totalElements 가 정확하다(현재 페이지 길이가 아니라).
  const myTurnCount = inboxRows.filter((a) => a.myTurn).length
  const pendingSent = outboxRows.filter((a) => a.status === 'PENDING').length
  const rejectedSent = outboxRows.filter((a) => a.status === 'REJECTED').length
  const unread = notiRows.filter((n) => !n.read).length

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <h1>대시보드</h1>
          <p className="muted" style={{ marginTop: 4 }}>
            {user?.username} · {user?.roles.join(', ')}
          </p>
        </div>
      </div>

      <CardGrid>
        <Card
          label="결재 대기"
          value={inbox?.totalElements ?? '—'}
          sub={myTurnCount > 0 ? `지금 내 차례 ${myTurnCount}건` : '내 차례 없음'}
          to="/approvals"
        />
        <Card
          label="진행중 상신"
          value={pendingSent}
          sub="내가 올려 결재중"
          to="/approvals?box=outbox"
        />
        <Card
          label="반려됨"
          value={rejectedSent}
          sub="최근 상신 기준"
          to="/approvals?box=outbox"
        />
        <Card label="미확인 알림" value={unread} sub="최근 5건 기준" />
      </CardGrid>

      <div className="section-title">알림</div>
      <div className="panel">
        {notiRows.length === 0 ? (
          <p className="muted">알림이 없습니다.</p>
        ) : (
          <>
            <table>
              <thead>
                <tr>
                  <th>유형</th>
                  <th>내용</th>
                  <th>일시</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {notiRows.map((n) => (
                  <tr key={n.id} style={n.read ? { opacity: 0.55 } : { fontWeight: 600 }}>
                    <td className="muted">{NOTIFICATION_TYPE[n.type] || n.type}</td>
                    <td>
                      {n.title}
                      <div className="muted" style={{ fontWeight: 400, fontSize: 12 }}>
                        {n.message}
                      </div>
                    </td>
                    <td className="muted">{(n.createdAt || '').replace('T', ' ').slice(0, 16)}</td>
                    <td>
                      {!n.read && (
                        <button
                          className="sm"
                          disabled={readMutation.isPending}
                          onClick={() => readMutation.mutate(n.id)}
                        >
                          읽음
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {unread > 0 && (
              <div style={{ marginTop: 12 }}>
                <button
                  className="sm"
                  disabled={readMutation.isPending}
                  onClick={() => readMutation.mutate(null)}
                >
                  전체 읽음 처리
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {isSales && sd && (
        <>
          <div className="section-title">영업 현황</div>
          <CardGrid>
            <Card
              label="이번 달 수주"
              value={formatMoney(sd.thisMonthOrderAmount)}
              sub={`${sd.thisMonthOrderCount}건`}
              to="/sales-orders"
            />
            <Card
              label="출하 대기"
              value={sd.awaitingShipmentCount}
              sub={formatMoney(sd.awaitingShipmentAmount)}
              to="/deliveries/new"
            />
            <Card
              label="미청구"
              value={sd.uninvoicedCount}
              sub={formatMoney(sd.uninvoicedAmount)}
              to="/invoices"
            />
            <Card
              label="견적 발송 대기"
              value={sd.quotationToSendCount}
              sub="승인됨 · 미발송"
              to="/quotations"
            />
          </CardGrid>

          <div className="section-title">수주 진행 현황</div>
          <div className="panel">
            <table>
              <thead>
                <tr>
                  <th>상태</th>
                  <th className="num">건수</th>
                </tr>
              </thead>
              <tbody>
                {/* Object.entries 는 키를 string 으로 넓혀버려서 상태 타입을 되찾아준다. */}
                {pipelineEntries(sd.pipeline).map(([status, count]) => (
                  <tr key={status}>
                    <td>{statusMeta(SALES_ORDER_STATUS, status).label}</td>
                    <td className="num mono">{count}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="section-title">최근 수주</div>
          <div className="panel">
            {(sd.recentOrders || []).length === 0 ? (
              <p className="muted">최근 수주가 없습니다.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>수주번호</th>
                    <th>고객</th>
                    <th>수주일</th>
                    <th>상태</th>
                    <th className="num">금액</th>
                  </tr>
                </thead>
                <tbody>
                  {sd.recentOrders.map((o) => (
                    <tr key={o.number}>
                      <td className="mono">{o.number}</td>
                      <td>{o.customerName}</td>
                      <td>{o.orderDate}</td>
                      <td>
                        <StatusBadge map={SALES_ORDER_STATUS} status={o.status} />
                      </td>
                      <td className="num mono">{formatMoney(o.totalAmount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </div>
  )
}

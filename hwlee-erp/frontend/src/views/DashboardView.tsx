import { useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getInbox, getOutbox } from '../api/approvals'
import { listNotifications, markNotificationRead, markAllNotificationsRead } from '../api/notifications'
import { getSalesDashboard, getOrderStatus } from '../api/dashboard'
import {
  SALES_ORDER_STATUS,
  NOTIFICATION_TYPE,
  formatMoney,
  statusMeta,
} from '../domain/status'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'
import type { SalesOrderStatus } from '../types/api'

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

// '수주 진행 현황' 기간 프리셋. 커스텀 년월 범위는 (분석 성격이라) 리포트 화면 몫으로 남긴다.
type Period = 'today' | 'week' | 'month' | 'all'
const PERIOD_LABELS: Record<Period, string> = {
  today: '오늘',
  week: '이번주',
  month: '이번달',
  all: '전체',
}

const pad = (n: number) => String(n).padStart(2, '0')
const fmtDate = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

/** 프리셋 → 수주일 범위(YYYY-MM-DD). '전체'는 빈 객체(파라미터 없이 전체 집계). */
function periodRange(p: Period): { dateFrom?: string; dateTo?: string } {
  if (p === 'all') return {}
  const now = new Date()
  if (p === 'today') return { dateFrom: fmtDate(now), dateTo: fmtDate(now) }
  if (p === 'week') {
    const monOffset = (now.getDay() + 6) % 7 // 월=0 이 되도록 보정
    const mon = new Date(now)
    mon.setDate(now.getDate() - monOffset)
    const sun = new Date(mon)
    sun.setDate(mon.getDate() + 6)
    return { dateFrom: fmtDate(mon), dateTo: fmtDate(sun) }
  }
  // month
  const first = new Date(now.getFullYear(), now.getMonth(), 1)
  const last = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  return { dateFrom: fmtDate(first), dateTo: fmtDate(last) }
}

/**
 * 수주 진행 현황 — 상태별 건수 가로 막대 차트.
 * 단일 계열(건수)이라 색은 식별이 아니라 강조용(모든 막대 accent 단색), 라벨·수치는 텍스트 토큰.
 * 라이브러리 없이 CSS 막대로 그려 번들을 늘리지 않는다.
 */
function OrderStatusChart({
  data,
}: {
  data: { status: SalesOrderStatus; label: string; count: number }[]
}) {
  const total = data.reduce((s, d) => s + d.count, 0)
  const max = Math.max(1, ...data.map((d) => d.count))
  if (total === 0) {
    return <p className="muted">해당 기간의 수주가 없습니다.</p>
  }
  return (
    <div style={{ display: 'grid', gap: 10 }}>
      {data.map((d) => (
        <div
          key={d.status}
          style={{
            display: 'grid',
            gridTemplateColumns: '96px 1fr 44px',
            alignItems: 'center',
            gap: 12,
          }}
          title={`${d.label}: ${d.count}건`}
        >
          <span className="muted" style={{ fontSize: 13 }}>
            {d.label}
          </span>
          <div
            style={{
              background: 'var(--bg-elevated)',
              border: '1px solid var(--border)',
              borderRadius: 5,
              height: 16,
            }}
          >
            <div
              style={{
                width: `${(d.count / max) * 100}%`,
                minWidth: d.count > 0 ? 4 : 0,
                height: '100%',
                background: 'var(--accent)',
                borderRadius: 4,
              }}
            />
          </div>
          <span className="mono" style={{ fontSize: 13, textAlign: 'right' }}>
            {d.count}
          </span>
        </div>
      ))}
    </div>
  )
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

  // 수주 진행 현황 차트 — 기간 프리셋으로 다시 집계(전체가 기본).
  const [period, setPeriod] = useState<Period>('all')
  const { data: orderStatus } = useQuery({
    queryKey: ['sd-order-status', period],
    queryFn: () => getOrderStatus(periodRange(period)),
    enabled: isSales,
  })
  const statusData = (Object.keys(SALES_ORDER_STATUS) as SalesOrderStatus[]).map((s) => ({
    status: s,
    label: statusMeta(SALES_ORDER_STATUS, s).label,
    count: orderStatus?.[s] ?? 0,
  }))

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
            <div
              style={{
                display: 'flex',
                justifyContent: 'flex-end',
                marginBottom: 14,
              }}
            >
              <select
                className="sm"
                value={period}
                onChange={(e) => setPeriod(e.target.value as Period)}
                aria-label="기간 선택"
              >
                {(Object.keys(PERIOD_LABELS) as Period[]).map((p) => (
                  <option key={p} value={p}>
                    {PERIOD_LABELS[p]}
                  </option>
                ))}
              </select>
            </div>
            <OrderStatusChart data={statusData} />
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

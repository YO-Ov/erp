import { Fragment, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getInbox, getOutbox } from '../api/approvals'
import { listNotifications, markNotificationRead, markAllNotificationsRead } from '../api/notifications'
import {
  getSalesDashboard,
  getOrderStatus,
  getPurchasingDashboard,
  getProductionDashboard,
  getFinanceDashboard,
  getHrDashboard,
  type OrderStatusCounts,
} from '../api/dashboard'
import {
  SALES_ORDER_STATUS,
  PURCHASE_ORDER_STATUS,
  PRODUCTION_ORDER_STATUS,
  PAYROLL_STATUS,
  NOTIFICATION_TYPE,
  formatMoney,
  statusMeta,
  type StatusMap,
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

// 진행 단계(흐름 순서)와 종료 단계 분리 — 종료/취소는 카드가 아니라 아래 요약으로 뺀다.
const PIPELINE_STAGES: SalesOrderStatus[] = [
  'DRAFT',
  'CONFIRMED',
  'SHIPPING',
  'SHIPPED',
  'INVOICING',
  'INVOICED',
]

/**
 * 수주 진행 현황 — 진행 단계 카드를 흐름(→)으로 배치한 파이프라인 스텝퍼.
 * 종료·취소는 막대에서 빼고 아래 muted 요약으로. 건수>0 단계는 accent 로 강조해 "어디에 일이 몰렸는지" 보이게.
 * 좁은 화면에선 한 줄 유지한 채 가로 스크롤(표처럼 접지 않음).
 */
function OrderStatusPipeline({ counts }: { counts: OrderStatusCounts | undefined }) {
  const c = (s: SalesOrderStatus) => counts?.[s] ?? 0
  const activeTotal = PIPELINE_STAGES.reduce((sum, s) => sum + c(s), 0)
  return (
    <>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          overflowX: 'auto',
          paddingBottom: 4,
        }}
      >
        {PIPELINE_STAGES.map((s, i) => (
          <Fragment key={s}>
            <div
              style={{
                flex: '1 1 0',
                minWidth: 88,
                textAlign: 'center',
                padding: '12px 10px',
                background: 'var(--bg-elevated)',
                border: '1px solid var(--border)',
                borderRadius: 10,
              }}
            >
              <div className="muted" style={{ fontSize: 12 }}>
                {statusMeta(SALES_ORDER_STATUS, s).label}
              </div>
              <div
                style={{
                  fontSize: 22,
                  fontWeight: 700,
                  marginTop: 4,
                  color: c(s) > 0 ? 'var(--accent)' : 'var(--text-muted)',
                }}
              >
                {c(s)}
              </div>
            </div>
            {i < PIPELINE_STAGES.length - 1 && (
              <span className="muted" style={{ flex: '0 0 auto', fontSize: 16 }}>
                →
              </span>
            )}
          </Fragment>
        ))}
      </div>
      <div className="muted" style={{ marginTop: 12, fontSize: 13 }}>
        진행 중 {activeTotal}건 · 종료 {c('CLOSED')} · 취소 {c('CANCELLED')}
      </div>
    </>
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

/**
 * 상태별 건수 가로 막대 — 구매·생산 파이프라인, 급여 상태, 부서별 인원 등에 공통으로 쓴다.
 * 단일 계열(건수)이라 색은 강조용(모든 막대 accent). 라이브러리 없이 CSS 막대로 그린다.
 */
function StatusBarChart({
  data,
  emptyText = '데이터가 없습니다.',
}: {
  data: { key: string; label: string; count: number }[]
  emptyText?: string
}) {
  const total = data.reduce((s, d) => s + d.count, 0)
  const max = Math.max(1, ...data.map((d) => d.count))
  if (total === 0) return <p className="muted">{emptyText}</p>
  return (
    <div style={{ display: 'grid', gap: 10 }}>
      {data.map((d) => (
        <div
          key={d.key}
          style={{
            display: 'grid',
            gridTemplateColumns: '110px 1fr 44px',
            alignItems: 'center',
            gap: 12,
          }}
          title={`${d.label}: ${d.count}`}
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

/** 상태맵 정의 순서대로 pipeline(상태→건수)을 막대 데이터로 변환. */
function pipelineToBars<K extends string>(pipeline: Record<K, number>, map: StatusMap<K>) {
  return (Object.keys(map) as K[]).map((k) => ({
    key: k,
    label: statusMeta(map, k).label,
    count: pipeline[k] ?? 0,
  }))
}

// 역할과 무관하게 모두가 쓰는 홈 화면.
//  - 공통: 결재/알림 요약 카드 4개 + 알림
//  - 영업(SALES)이면 SD KPI 섹션이 더 붙는다.
export default function DashboardView() {
  const { user, hasRole } = useAuth()
  const queryClient = useQueryClient()
  const isSales = hasRole('SALES', 'ADMIN')
  const isPurchasing = hasRole('PURCHASING', 'ADMIN')
  const isProduction = hasRole('PRODUCTION', 'ADMIN')
  const isFinance = hasRole('FINANCE', 'ADMIN')
  const isHr = hasRole('HR', 'ADMIN')

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

  // 수주 진행 현황 — 기간 프리셋으로 다시 집계(이번달이 기본).
  const [period, setPeriod] = useState<Period>('month')
  const { data: orderStatus } = useQuery({
    queryKey: ['sd-order-status', period],
    queryFn: () => getOrderStatus(periodRange(period)),
    enabled: isSales,
  })
  const { data: mm } = useQuery({
    queryKey: ['mm-dashboard'],
    queryFn: getPurchasingDashboard,
    enabled: isPurchasing,
  })
  const { data: pp } = useQuery({
    queryKey: ['pp-dashboard'],
    queryFn: getProductionDashboard,
    enabled: isProduction,
  })
  const { data: fi } = useQuery({
    queryKey: ['fi-dashboard'],
    queryFn: getFinanceDashboard,
    enabled: isFinance,
  })
  const { data: hr } = useQuery({
    queryKey: ['hr-dashboard'],
    queryFn: getHrDashboard,
    enabled: isHr,
  })

  // id 를 주면 그 알림만, null 이면 전체 읽음 처리.
  const readMutation = useMutation<void, Error, number | null>({
    mutationFn: (id) => (id ? markNotificationRead(id) : markAllNotificationsRead()),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const outboxRows = outbox?.content || []
  const notiRows = notis?.content || []

  // 결재함 전체 건수는 서버 페이징의 totalElements 가 정확하다(현재 페이지 길이가 아니라).
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
        <Card label="결재 대기" value={inbox?.totalElements ?? '—'} to="/approvals" />
        <Card label="진행중 상신" value={pendingSent} to="/approvals?box=outbox" />
        <Card label="반려됨" value={rejectedSent} to="/approvals?box=outbox" />
        <Card label="미확인 알림" value={unread} />
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
            <OrderStatusPipeline counts={orderStatus} />
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

      {isPurchasing && mm && (
        <>
          <div className="section-title">구매 현황</div>
          <CardGrid>
            <Card
              label="이번 달 발주"
              value={formatMoney(mm.thisMonthOrderAmount)}
              sub={`${mm.thisMonthOrderCount}건`}
              to="/purchase-orders"
            />
            <Card
              label="입고 대기"
              value={mm.awaitingReceiptCount}
              sub={formatMoney(mm.awaitingReceiptAmount)}
              to="/goods-receipts"
            />
          </CardGrid>

          <div className="section-title">발주 상태별 현황</div>
          <div className="panel">
            <StatusBarChart
              data={pipelineToBars(mm.pipeline, PURCHASE_ORDER_STATUS)}
              emptyText="발주가 없습니다."
            />
          </div>

          <div className="section-title">최근 발주</div>
          <div className="panel">
            {(mm.recentOrders || []).length === 0 ? (
              <p className="muted">최근 발주가 없습니다.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>발주번호</th>
                    <th>공급처</th>
                    <th>발주일</th>
                    <th>상태</th>
                    <th className="num">금액</th>
                  </tr>
                </thead>
                <tbody>
                  {mm.recentOrders.map((o) => (
                    <tr key={o.number}>
                      <td className="mono">{o.number}</td>
                      <td>{o.vendorName}</td>
                      <td>{o.orderDate}</td>
                      <td>
                        <StatusBadge map={PURCHASE_ORDER_STATUS} status={o.status} />
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

      {isProduction && pp && (
        <>
          <div className="section-title">생산 현황</div>
          <CardGrid>
            <Card
              label="진행중"
              value={pp.inProgressCount}
              sub="착수됨(RELEASED)"
              to="/production-orders"
            />
            <Card
              label="완료 대기"
              value={pp.awaitingCompletionCount}
              sub="착수 예정(PLANNED)"
              to="/production-orders"
            />
            <Card
              label="이번 달 생산지시"
              value={pp.thisMonthOrderCount}
              sub="이번 달 등록"
              to="/production-orders"
            />
          </CardGrid>

          <div className="section-title">생산 상태별 현황</div>
          <div className="panel">
            <StatusBarChart
              data={pipelineToBars(pp.pipeline, PRODUCTION_ORDER_STATUS)}
              emptyText="생산지시가 없습니다."
            />
          </div>

          <div className="section-title">최근 생산지시</div>
          <div className="panel">
            {(pp.recentOrders || []).length === 0 ? (
              <p className="muted">최근 생산지시가 없습니다.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>지시번호</th>
                    <th>품목</th>
                    <th>일자</th>
                    <th>상태</th>
                    <th className="num">수량</th>
                  </tr>
                </thead>
                <tbody>
                  {pp.recentOrders.map((o) => (
                    <tr key={o.number}>
                      <td className="mono">{o.number}</td>
                      <td>{o.productName}</td>
                      <td>{o.orderDate}</td>
                      <td>
                        <StatusBadge map={PRODUCTION_ORDER_STATUS} status={o.status} />
                      </td>
                      <td className="num mono">{o.quantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      {isFinance && fi && (
        <>
          <div className="section-title">재무 현황</div>
          <CardGrid>
            <Card
              label="이번 달 매출"
              value={formatMoney(fi.thisMonthSalesAmount)}
              sub="발행 인보이스"
              to="/invoices"
            />
            <Card
              label="이번 달 입금"
              value={formatMoney(fi.thisMonthReceiptAmount)}
              sub="수금(POSTED)"
              to="/payments"
            />
            <Card
              label="미수금"
              value={formatMoney(fi.accountsReceivable)}
              sub="발행 인보이스 − 입금"
              to="/payments"
            />
            <Card
              label="승인 대기 전표"
              value={fi.pendingJournalCount}
              sub="작성중(DRAFT)"
              to="/journal-entries"
            />
            <Card
              label="여신 요청 대기"
              value={fi.pendingCreditRequestCount}
              sub="검토중(PENDING)"
              to="/credit-requests"
            />
          </CardGrid>
        </>
      )}

      {isHr && hr && (
        <>
          <div className="section-title">인사 현황</div>
          <CardGrid>
            <Card
              label="재직 사원"
              value={hr.activeEmployeeCount}
              sub={`전체 ${hr.totalEmployeeCount}명`}
              to="/employees"
            />
            <Card label="부서" value={hr.departmentCount} sub="부서 수" />
            <Card
              label="이번 달 급여대장"
              value={
                hr.thisMonthPayrollStatus
                  ? statusMeta(PAYROLL_STATUS, hr.thisMonthPayrollStatus).label
                  : '미생성'
              }
              sub={
                hr.thisMonthPayrollStatus
                  ? `${hr.thisMonthPeriod} · ${formatMoney(hr.thisMonthPayrollNet)}`
                  : hr.thisMonthPeriod
              }
              to="/payroll-runs"
            />
          </CardGrid>

          <div className="section-title">급여대장 상태별</div>
          <div className="panel">
            <StatusBarChart
              data={pipelineToBars(hr.payrollStatusPipeline, PAYROLL_STATUS)}
              emptyText="급여대장이 없습니다."
            />
          </div>

          <div className="section-title">부서별 인원</div>
          <div className="panel">
            <StatusBarChart
              data={Object.entries(hr.departmentHeadcount).map(([name, count]) => ({
                key: name,
                label: name,
                count,
              }))}
              emptyText="부서 정보가 없습니다."
            />
          </div>

          <div className="section-title">최근 입사자</div>
          <div className="panel">
            {(hr.recentHires || []).length === 0 ? (
              <p className="muted">최근 입사자가 없습니다.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>사번</th>
                    <th>이름</th>
                    <th>부서</th>
                    <th>입사일</th>
                  </tr>
                </thead>
                <tbody>
                  {hr.recentHires.map((e) => (
                    <tr key={e.code}>
                      <td className="mono">{e.code}</td>
                      <td>{e.name}</td>
                      <td>{e.departmentName}</td>
                      <td>{e.hireDate}</td>
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

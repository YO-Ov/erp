// 각 도메인 enum의 한글 라벨 + 배지 색 tone + 상태별 가능한 액션.
// 백엔드 enum 과 1:1로 맞춘다.
// (프론트는 '어떤 버튼을 보여줄지'만 이 규칙으로 판단하고,
//  실제 상태 전이 강제는 백엔드가 한다 — 프론트는 화면 편의일 뿐)
//
// ⚠️ 타입이 잡아주지 못하는 영역: 상태 전이 규칙·차대 균형 같은 도메인 규칙은
//    런타임 검증(백엔드)의 몫이다. 여기 규칙이 서버와 어긋나면 컴파일은 통과하고
//    화면에서 버튼만 잘못 보인다 — 서버 에러 메시지를 항상 표시해야 하는 이유.

import type {
  AccountType,
  GoodsReceiptAction,
  ProductionOrderAction,
  PurchaseOrderAction,
  QuotationAction,
  SalesOrderAction,
  ApprovalDocType,
  ApprovalStatus,
  ApprovalStepStatus,
  ApprovalStepType,
  Approval,
  DeliveryStatus,
  GoodsReceiptStatus,
  InvoiceStatus,
  JournalEntryStatus,
  JournalSource,
  MovementReason,
  NormalSide,
  NotificationType,
  CreditRequestStatus,
  ContractStatus,
  PayrollAction,
  PayrollStatus,
  Position,
  PaymentStatus,
  PaymentType,
  ProductionOrderStatus,
  PurchaseOrderStatus,
  QuotationStatus,
  SalesOrderStatus,
} from '../types/api'

/** 배지 색 계열. */
export type Tone = 'neutral' | 'active' | 'warn' | 'done' | 'muted' | 'danger'

export interface StatusMeta {
  label: string
  tone: Tone
}

/**
 * 상태 코드 → 표시 메타. StatusBadge 가 받는 map 의 형태.
 * Record<K, StatusMeta> 로 두면 enum 값을 하나라도 빠뜨렸을 때 컴파일에서 걸린다.
 */
export type StatusMap<K extends string> = Record<K, StatusMeta>

// ── 견적 ─────────────────────────────────────────────
// DRAFT → APPROVED → SENT → ACCEPTED → CONVERTED
//                              ↘ EXPIRED / ↘ CANCELLED
export const QUOTATION_STATUS: StatusMap<QuotationStatus> = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  APPROVED: { label: '승인됨', tone: 'active' },
  SENT: { label: '발송됨', tone: 'done' },
  ACCEPTED: { label: '수락됨', tone: 'active' },
  CONVERTED: { label: '수주전환', tone: 'done' },
  EXPIRED: { label: '만료', tone: 'muted' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

/**
 * 견적 상세에서 노출할 버튼.
 * ⚠️ convert 는 API 액션이 아니라 수주 생성 화면으로 '이동'이다 — 화면이 따로 처리한다.
 */
export type QuotationButton = QuotationAction | 'convert'

// 견적 상태별로 노출할 액션 버튼.
export function quotationActions(status: QuotationStatus): QuotationButton[] {
  switch (status) {
    case 'DRAFT':
      return ['submit-approval']
    case 'APPROVED':
      return ['send', 'cancel']
    case 'SENT':
      return ['accept', 'cancel']
    case 'ACCEPTED':
      return ['convert', 'cancel'] // convert = 수주 생성 화면으로 이동
    default:
      return []
  }
}

// ── 수주 ─────────────────────────────────────────────
// DRAFT → CONFIRMED → SHIPPING ↔ SHIPPED → INVOICING ↔ INVOICED → CLOSED
//       ↘ CANCELLED (DRAFT/CONFIRMED 에서만)
export const SALES_ORDER_STATUS: StatusMap<SalesOrderStatus> = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  CONFIRMED: { label: '확정', tone: 'active' },
  SHIPPING: { label: '출하중', tone: 'warn' },
  SHIPPED: { label: '출하완료', tone: 'done' },
  INVOICING: { label: '청구중', tone: 'warn' },
  INVOICED: { label: '청구완료', tone: 'done' },
  CLOSED: { label: '종료', tone: 'muted' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function salesOrderActions(status: SalesOrderStatus): SalesOrderAction[] {
  switch (status) {
    case 'DRAFT':
      return ['confirm', 'cancel']
    case 'CONFIRMED':
      return ['cancel']
    case 'INVOICED':
      return ['close']
    default:
      return []
  }
}

// ── 출하 ─────────────────────────────────────────────
// DRAFT → SHIPPED / ↘ CANCELLED
export const DELIVERY_STATUS: StatusMap<DeliveryStatus> = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  SHIPPED: { label: '출하완료', tone: 'done' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function deliveryActions(status: DeliveryStatus): 'cancel'[] {
  // 취소 가능한 상태에서만 취소 버튼.
  return status === 'CANCELLED' ? [] : ['cancel']
}

// ── 청구 ─────────────────────────────────────────────
// DRAFT → ISSUED / ↘ CANCELLED
export const INVOICE_STATUS: StatusMap<InvoiceStatus> = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  ISSUED: { label: '발행됨', tone: 'done' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function invoiceActions(status: InvoiceStatus): 'cancel'[] {
  return status === 'CANCELLED' ? [] : ['cancel']
}

// ── 발주(구매) ───────────────────────────────────────
// DRAFT → CONFIRMED → RECEIVED → CLOSED / ↘ CANCELLED
// confirm 엔드포인트는 없다 — DRAFT는 결재 상신(submit-approval)으로만 진행.
export const PURCHASE_ORDER_STATUS: StatusMap<PurchaseOrderStatus> = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  CONFIRMED: { label: '확정', tone: 'active' },
  RECEIVED: { label: '입고완료', tone: 'done' },
  CLOSED: { label: '종료', tone: 'muted' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function purchaseOrderActions(status: PurchaseOrderStatus): PurchaseOrderAction[] {
  switch (status) {
    case 'DRAFT':
      return ['submit-approval', 'cancel']
    case 'CONFIRMED':
      return ['close', 'cancel']
    case 'RECEIVED':
      return ['close']
    default:
      return []
  }
}

// ── 생산 작업지시(PP) ─────────────────────────────────
// PLANNED → RELEASED → COMPLETED / ↘ CANCELLED
export const PRODUCTION_ORDER_STATUS: StatusMap<ProductionOrderStatus> = {
  PLANNED: { label: '계획', tone: 'neutral' },
  RELEASED: { label: '착수', tone: 'active' },
  COMPLETED: { label: '완료', tone: 'done' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function productionOrderActions(status: ProductionOrderStatus): ProductionOrderAction[] {
  switch (status) {
    case 'PLANNED':
      return ['release', 'cancel']
    case 'RELEASED':
      return ['complete', 'cancel'] // dispatch(MES 전송)는 상세에서 별도 조건으로 노출
    default:
      return []
  }
}

// ── 입고(구매) ───────────────────────────────────────
// DRAFT → POSTED(전기: 재고 반영·발주 RECEIVED 전이) / ↘ CANCELLED
export const GOODS_RECEIPT_STATUS: StatusMap<GoodsReceiptStatus> = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  POSTED: { label: '전기완료', tone: 'done' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function goodsReceiptActions(status: GoodsReceiptStatus): GoodsReceiptAction[] {
  return status === 'DRAFT' ? ['post', 'cancel'] : []
}

// 발주 상태별로 입고를 만들 수 있는지(확정 이후 ~ 입고완료까지).
export function canReceive(purchaseOrderStatus: PurchaseOrderStatus): boolean {
  return ['CONFIRMED', 'RECEIVED'].includes(purchaseOrderStatus)
}

// 수주 상태별로 후속 문서(출하·청구)를 만들 수 있는지.
// 확정(CONFIRMED) 이후 ~ 종료/취소 전까지 허용.
export function canCreateDownstream(salesOrderStatus: SalesOrderStatus): boolean {
  return ['CONFIRMED', 'SHIPPING', 'SHIPPED', 'INVOICING', 'INVOICED'].includes(salesOrderStatus)
}

// ── 전표(FI) ─────────────────────────────────────────
// DRAFT → POSTED(전기: 원장 반영) / POSTED → CANCELLED
// ⚠️ SD/MM 과 반대다 — 취소는 POSTED 만 가능하고 DRAFT 는 취소 대상이 아니다.
//    (회계는 확정된 전표를 '취소된 사실'로 남기고, 초안은 결재로만 진행)
export const JOURNAL_ENTRY_STATUS: StatusMap<JournalEntryStatus> = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  POSTED: { label: '전기완료', tone: 'done' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export type JournalEntryButton = 'submit-approval' | 'cancel'

export function journalEntryActions(
  status: JournalEntryStatus,
  sourceType: JournalSource,
): JournalEntryButton[] {
  switch (status) {
    case 'DRAFT':
      // 상신은 수동(MANUAL) 전표만 — 자동 분개 전표는 결재 대상이 아니다.
      return sourceType === 'MANUAL' ? ['submit-approval'] : []
    case 'POSTED':
      return ['cancel']
    default:
      return []
  }
}

// 전표 출처 — 어떤 업무가 이 전표를 만들었는지. MANUAL 만 사람이 직접 입력한 것.
export const JOURNAL_SOURCE: Record<JournalSource, string> = {
  INV: '매출(청구)',
  GI: '매출원가(출하)',
  GR: '매입(입고)',
  PAY: '입금/출금',
  PAYROLL: '급여',
  PROD: '생산완료',
  MANUAL: '수동입력',
}

// ── 입금/출금(FI) ────────────────────────────────────
// DRAFT → POSTED. 취소 엔드포인트는 없다.
export const PAYMENT_STATUS: StatusMap<PaymentStatus> = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  POSTED: { label: '전기완료', tone: 'done' },
}

export function paymentActions(status: PaymentStatus, type: PaymentType): 'submit-approval'[] {
  // 상신은 출금(DISBURSEMENT) 초안만 — 입금은 결재 없이 바로 전기한다.
  return status === 'DRAFT' && type === 'DISBURSEMENT' ? ['submit-approval'] : []
}

export const PAYMENT_TYPE: StatusMap<PaymentType> = {
  RECEIPT: { label: '입금', tone: 'active' },
  DISBURSEMENT: { label: '출금', tone: 'warn' },
}

// ── 계정과목(FI) ─────────────────────────────────────
export const ACCOUNT_TYPE: Record<AccountType, string> = {
  ASSET: '자산',
  LIABILITY: '부채',
  EQUITY: '자본',
  REVENUE: '수익',
  EXPENSE: '비용',
}

export const NORMAL_SIDE: Record<NormalSide, string> = {
  DEBIT: '차변',
  CREDIT: '대변',
}

// ── 전자결재 ─────────────────────────────────────────
// DRAFT →(상신) PENDING →(전 단계 처리) APPROVED →(원본 문서 자동 진행)
//                   ├─(반려) REJECTED (종결)
//                   ├─(반송) DRAFT (상신자가 고쳐서 재상신)
//                   └─(회수) WITHDRAWN (종결)
export const APPROVAL_STATUS: StatusMap<ApprovalStatus> = {
  DRAFT: { label: '작성중/반송됨', tone: 'neutral' },
  PENDING: { label: '결재중', tone: 'warn' },
  APPROVED: { label: '승인', tone: 'done' },
  REJECTED: { label: '반려', tone: 'danger' },
  WITHDRAWN: { label: '회수', tone: 'muted' },
}

// 결재선 각 단계의 처리 상태.
export const APPROVAL_STEP_STATUS: StatusMap<ApprovalStepStatus> = {
  PENDING: { label: '미처리', tone: 'neutral' },
  APPROVED: { label: '처리됨', tone: 'done' },
  REJECTED: { label: '반려', tone: 'danger' },
  SKIPPED: { label: '건너뜀', tone: 'muted' },
}

// 결재 단계 유형 — 한국 실무 전자결재의 세 갈래.
export const APPROVAL_STEP_TYPE: Record<ApprovalStepType, string> = {
  APPROVAL: '결재',
  AGREEMENT: '합의',
  REFERENCE: '참조',
}

// 결재 대상 문서 → 원본 문서 React 경로.
// ⚠️ 백엔드 ApprovalResponse.docLink 는 옛 Thymeleaf 경로(/fi/payments/1)라 쓸 수 없다.
//    React 라우트와 다르므로 여기서 직접 매핑한다.
const APPROVAL_DOC_ROUTE: Record<ApprovalDocType, string | null> = {
  QUOTATION: '/quotations/',
  SALES_ORDER: '/sales-orders/',
  PURCHASE_ORDER: '/purchase-orders/',
  PAYMENT: '/payments/',
  JOURNAL: '/journal-entries/',
  CREDIT_LIMIT: '/credit-requests/',
}

// 원본 문서로 가는 링크. 화면이 아직 없는 문서(여신 상향 등)는 null.
export function approvalDocLink(docType: ApprovalDocType, refId: number): string | null {
  const prefix = APPROVAL_DOC_ROUTE[docType]
  return prefix ? `${prefix}${refId}` : null
}

export type ApprovalActionCode = 'approve' | 'reject' | 'return' | 'withdraw' | 'resubmit'

// 결재 문서에서 내가 지금 할 수 있는 것.
//  - myTurn 은 서버가 결재선을 보고 판단해 내려준다(내 차례인지).
//  - 회수는 상신자 본인 + PENDING + 아직 아무도 처리 안 했을 때만(서버가 최종 검증).
//  - 재상신은 상신자 본인 + 반송돼서 DRAFT 로 돌아온 문서.
export function approvalActions(
  approval: Approval | undefined,
  currentUsername: string,
): ApprovalActionCode[] {
  if (!approval) return []
  const isRequester = approval.requester === currentUsername
  const acts: ApprovalActionCode[] = []

  if (approval.status === 'PENDING' && approval.myTurn) {
    acts.push('approve', 'reject')
    // 반송은 결재(APPROVAL) 단계만 — 합의·참조 단계는 반려만 가능하다.
    const myStep = (approval.steps || []).find(
      (s) => s.approver === currentUsername && s.status === 'PENDING',
    )
    if (myStep?.type === 'APPROVAL') acts.push('return')
  }
  if (approval.status === 'PENDING' && isRequester) acts.push('withdraw')
  if (approval.status === 'DRAFT' && isRequester) acts.push('resubmit')

  return acts
}

export const APPROVAL_ACTION_LABEL: Record<ApprovalActionCode, string> = {
  approve: '승인',
  reject: '반려',
  return: '반송',
  withdraw: '회수',
  resubmit: '재상신',
}

// 반려/반송 사유를 한 군데서 꺼낸다.
// ⚠️ 백엔드는 둘을 다른 곳에 담는다 — 반송은 returnReason 에, 반려는 그 단계의 comment 에만.
//    returnReason 만 보면 반려된 문서의 사유가 빈칸으로 보인다.
export function approvalReason(approval: Approval | undefined): string | null {
  if (!approval) return null
  if (approval.returnReason) return approval.returnReason
  const rejected = (approval.steps || []).find((s) => s.status === 'REJECTED')
  return rejected?.comment || null
}

// 알림 유형 라벨.
export const NOTIFICATION_TYPE: Record<NotificationType, string> = {
  PRODUCTION_CANCELLED: '생산지시 취소',
  CREDIT_REQUEST_SUBMITTED: '여신 상향 요청',
  CREDIT_REQUEST_APPROVED: '여신 상향 승인',
  CREDIT_REQUEST_REJECTED: '여신 상향 반려',
  APPROVAL_REQUESTED: '결재 요청',
  APPROVAL_APPROVED: '결재 승인',
  APPROVAL_REJECTED: '결재 반려',
  APPROVAL_RETURNED: '결재 반송',
  APPROVAL_REFERENCED: '결재 참조',
}

// 액션 버튼 표시명.
export const ACTION_LABEL: Record<string, string> = {
  'submit-approval': '결재 상신',
  send: '발송',
  accept: '수락',
  convert: '수주 생성',
  cancel: '취소',
  confirm: '확정',
  close: '마감',
  post: '전기',
  release: '착수',
  complete: '완료',
  dispatch: 'MES 전송',
}

// 금액/날짜 표시 헬퍼.
export function formatMoney(v: number | null | undefined): string {
  if (v == null) return '-'
  return Number(v).toLocaleString('ko-KR')
}

export function statusMeta<K extends string>(map: StatusMap<K>, status: K): StatusMeta {
  return map[status] || { label: status || '-', tone: 'neutral' }
}

// ── 재고 이동사유 (MM) ───────────────────────────────
// 부호 방향이 정해져 있다(백엔드가 강제). 화면은 라벨 + 입/출고 방향만 판단한다.
export const MOVEMENT_REASON: Record<MovementReason, string> = {
  GOODS_RECEIPT: '매입 입고',
  GOODS_ISSUE: '출고',
  ADJUSTMENT_PLUS: '조정(+)',
  ADJUSTMENT_MINUS: '조정(−)',
  SCRAP: '폐기',
  PRODUCTION_OUT: '생산 투입',
  PRODUCTION_IN: '생산 산출',
}

// 입고(+)면 done(파랑), 출고(−)면 warn(주황) 계열로 배지 색을 나눈다.
// 반환은 이 둘로만 좁혀서, 쓰는 쪽이 2색 스타일 맵만 준비하면 되게 한다.
export function movementTone(reason: MovementReason): 'done' | 'warn' {
  const inbound: MovementReason[] = ['GOODS_RECEIPT', 'ADJUSTMENT_PLUS', 'PRODUCTION_IN']
  return inbound.includes(reason) ? 'done' : 'warn'
}

// ── 인사(HR) ─────────────────────────────────────────

// 직급 — 조회/리포트용 메타(급여 계산엔 안 쓰임).
export const POSITION: Record<Position, string> = {
  STAFF: '사원',
  SENIOR: '선임',
  MANAGER: '책임/과장',
  DIRECTOR: '임원/이사',
}

// 급여계약 상태.
export const CONTRACT_STATUS: StatusMap<ContractStatus> = {
  ACTIVE: { label: '유효', tone: 'active' },
  INACTIVE: { label: '종료', tone: 'muted' },
}

// 급여대장 상태.
// DRAFT →confirm(인건비 전표) → CONFIRMED →pay(지급 전표) → PAID. 발생주의 2단계.
export const PAYROLL_STATUS: StatusMap<PayrollStatus> = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  CONFIRMED: { label: '확정', tone: 'active' },
  PAID: { label: '지급완료', tone: 'done' },
}

export function payrollActions(status: PayrollStatus): PayrollAction[] {
  switch (status) {
    case 'DRAFT':
      return ['confirm']
    case 'CONFIRMED':
      return ['pay']
    default:
      return []
  }
}

export const PAYROLL_ACTION_LABEL: Record<PayrollAction, string> = {
  confirm: '확정(인건비 전표)',
  pay: '지급(지급 전표)',
}

// 근무시간(분) → "8시간 30분" 표기. 근태·연장근로 표시용.
export function formatMinutes(min: number | null | undefined): string {
  if (min == null) return '-'
  const h = Math.floor(min / 60)
  const m = min % 60
  if (h === 0) return `${m}분`
  return m === 0 ? `${h}시간` : `${h}시간 ${m}분`
}

// ── 여신 상향 요청(FI) ───────────────────────────────
// PENDING →(전자결재 승인) APPROVED / →(거부) REJECTED. 실제 결정은 결재함에서.
export const CREDIT_REQUEST_STATUS: StatusMap<CreditRequestStatus> = {
  PENDING: { label: '검토중', tone: 'warn' },
  APPROVED: { label: '승인', tone: 'done' },
  REJECTED: { label: '거부', tone: 'danger' },
}

// 견적/수주 상태 enum의 한글 라벨 + 배지 색 tone + 상태별 가능한 액션.
// 백엔드 QuotationStatus / SalesOrderStatus 와 1:1로 맞춘다.
// (프론트는 '어떤 버튼을 보여줄지'만 이 규칙으로 판단하고,
//  실제 상태 전이 강제는 백엔드가 한다 — 프론트는 화면 편의일 뿐)

// ── 견적 ─────────────────────────────────────────────
// DRAFT → APPROVED → SENT → ACCEPTED → CONVERTED
//                              ↘ EXPIRED / ↘ CANCELLED
export const QUOTATION_STATUS = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  APPROVED: { label: '승인됨', tone: 'active' },
  SENT: { label: '발송됨', tone: 'done' },
  ACCEPTED: { label: '수락됨', tone: 'active' },
  CONVERTED: { label: '수주전환', tone: 'done' },
  EXPIRED: { label: '만료', tone: 'muted' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

// 견적 상태별로 노출할 액션 버튼.
export function quotationActions(status) {
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
export const SALES_ORDER_STATUS = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  CONFIRMED: { label: '확정', tone: 'active' },
  SHIPPING: { label: '출하중', tone: 'warn' },
  SHIPPED: { label: '출하완료', tone: 'done' },
  INVOICING: { label: '청구중', tone: 'warn' },
  INVOICED: { label: '청구완료', tone: 'done' },
  CLOSED: { label: '종료', tone: 'muted' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function salesOrderActions(status) {
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
export const DELIVERY_STATUS = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  SHIPPED: { label: '출하완료', tone: 'done' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function deliveryActions(status) {
  // 취소 가능한 상태에서만 취소 버튼.
  return status === 'CANCELLED' ? [] : ['cancel']
}

// ── 청구 ─────────────────────────────────────────────
// DRAFT → ISSUED / ↘ CANCELLED
export const INVOICE_STATUS = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  ISSUED: { label: '발행됨', tone: 'done' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function invoiceActions(status) {
  return status === 'CANCELLED' ? [] : ['cancel']
}

// ── 발주(구매) ───────────────────────────────────────
// DRAFT → CONFIRMED → RECEIVED → CLOSED / ↘ CANCELLED
// confirm 엔드포인트는 없다 — DRAFT는 결재 상신(submit-approval)으로만 진행.
export const PURCHASE_ORDER_STATUS = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  CONFIRMED: { label: '확정', tone: 'active' },
  RECEIVED: { label: '입고완료', tone: 'done' },
  CLOSED: { label: '종료', tone: 'muted' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function purchaseOrderActions(status) {
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
export const PRODUCTION_ORDER_STATUS = {
  PLANNED: { label: '계획', tone: 'neutral' },
  RELEASED: { label: '착수', tone: 'active' },
  COMPLETED: { label: '완료', tone: 'done' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function productionOrderActions(status) {
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
export const GOODS_RECEIPT_STATUS = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  POSTED: { label: '전기완료', tone: 'done' },
  CANCELLED: { label: '취소', tone: 'danger' },
}

export function goodsReceiptActions(status) {
  return status === 'DRAFT' ? ['post', 'cancel'] : []
}

// 발주 상태별로 입고를 만들 수 있는지(확정 이후 ~ 입고완료까지).
export function canReceive(purchaseOrderStatus) {
  return ['CONFIRMED', 'RECEIVED'].includes(purchaseOrderStatus)
}

// 수주 상태별로 후속 문서(출하·청구)를 만들 수 있는지.
// 확정(CONFIRMED) 이후 ~ 종료/취소 전까지 허용.
export function canCreateDownstream(salesOrderStatus) {
  return ['CONFIRMED', 'SHIPPING', 'SHIPPED', 'INVOICING', 'INVOICED'].includes(
    salesOrderStatus,
  )
}

// 액션 버튼 표시명.
export const ACTION_LABEL = {
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
export function formatMoney(v) {
  if (v == null) return '-'
  return Number(v).toLocaleString('ko-KR')
}

export function statusMeta(map, status) {
  return map[status] || { label: status || '-', tone: 'neutral' }
}

// 백엔드(hwlee-erp) REST 응답·요청의 타입 정의.
// 각 타입은 Java 쪽 record 와 1:1 로 대응한다 — 서버 DTO 가 바뀌면 여기부터 고친다.
// 근거: http://localhost:8080/api-docs (springdoc OpenAPI 3.1, 경로 124 / 스키마 136)
//
// ⚠️ 스펙의 required 는 믿지 않는다 — springdoc 은 Java record 필드를 전부 optional 로 내보낸다.
//    필드명·타입만 스펙에서 가져오고, null 여부는 도메인 코드(엔티티·서비스) 기준으로 적었다.
//
// 직렬화 규칙:
//   BigDecimal            → number
//   LocalDate             → string ('2026-07-17')
//   LocalDateTime         → string ('2026-07-17T09:30:00')
//   Java enum             → 같은 이름의 문자열 리터럴 유니온

// ── 공통 ─────────────────────────────────────────────

/** Spring Data 의 Page<T>. ⚠️ /api/accounts 만 예외로 Page 가 아니라 배열을 그대로 준다. */
export interface Page<T> {
  content: T[]
  totalPages: number
  totalElements: number
  size: number
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

/** 생성/수정 이력 — 대부분의 Response 가 공유한다(BaseEntity). */
export interface Audited {
  createdAt: string
  createdBy: string
  updatedAt: string
  updatedBy: string
}

/** 마스터 공통 상태. */
export type MasterStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED'

/** 지급 조건. */
export type PaymentTerms = 'NET30' | 'NET60' | 'COD' | 'PREPAID'

/** 목록 조회 공통 쿼리 파라미터(Spring Pageable + 검색 조건). */
export interface PageParams {
  page?: number
  size?: number
  sort?: string
}

// ── 인증 (security/auth) ─────────────────────────────

/** LoginRequest — ⚠️ 필드명이 email 이 아니라 username(값은 이메일). */
export interface LoginRequest {
  username: string
  password: string
}

/** LoginResponse — refresh 토큰은 없다(만료 시 재로그인). */
export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  roles: string[]
}

/** 백엔드 Role enum 과 대응. 화면 권한 가드에 쓴다. */
export type Role =
  | 'SALES'
  | 'PURCHASING'
  | 'PRODUCTION'
  | 'FINANCE'
  | 'HR'
  | 'DIRECTOR'
  | 'ADMIN'

// ── 마스터 (master) ──────────────────────────────────

export interface Customer extends Audited {
  id: number
  code: string
  name: string
  businessNo: string | null
  address: string | null
  creditLimit: number
  paymentTerms: PaymentTerms
  status: MasterStatus
}

// 고객 등록 — 신규는 여신한도 0(현금거래)으로 시작하고, 한도 부여/상향은 재무의 여신 승인으로만.
export interface CustomerCreateRequest {
  name: string
  businessNo: string
  address: string | null
  paymentTerms: PaymentTerms
}

// 고객 수정 — businessNo(외부 식별자)와 creditLimit(여신=재무 권한)은 바꿀 수 없다.
export interface CustomerUpdateRequest {
  name: string
  address: string | null
  paymentTerms: PaymentTerms
}

export type ItemType = 'FINISHED' | 'COMPONENT'
export type ItemUnit = 'EA' | 'BOX' | 'KG'

export interface Item extends Audited {
  id: number
  code: string
  name: string
  category: string | null
  itemType: ItemType
  unit: ItemUnit
  standardCost: number
  standardPrice: number
  status: MasterStatus
}

export interface Warehouse extends Audited {
  id: number
  code: string
  name: string
  address: string | null
  factoryCode: string | null
  factoryName: string | null
  status: MasterStatus
}

export interface Vendor extends Audited {
  id: number
  code: string
  name: string
  businessNo: string | null
  address: string | null
  paymentTerms: PaymentTerms
  status: MasterStatus
}

/** VendorItemResponse — 구매정보레코드. 발주 품목은 이 목록으로 제한된다. */
export interface VendorItem extends Audited {
  id: number
  vendorId: number
  vendorCode: string
  vendorName: string
  itemId: number
  itemCode: string
  itemName: string
  supplyPrice: number
  leadTimeDays: number | null
  status: MasterStatus
}

// ── 영업: 견적 (sd/quotation) ────────────────────────

/** QuotationStatus — DRAFT → APPROVED → SENT → ACCEPTED → CONVERTED / EXPIRED / CANCELLED */
export type QuotationStatus =
  | 'DRAFT'
  | 'APPROVED'
  | 'SENT'
  | 'ACCEPTED'
  | 'CONVERTED'
  | 'EXPIRED'
  | 'CANCELLED'

export interface QuotationLine {
  id: number
  lineNo: number
  itemId: number
  itemCode: string
  itemName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface Quotation extends Audited {
  id: number
  number: string
  customerId: number
  customerCode: string
  customerName: string
  status: QuotationStatus
  issuedDate: string
  validUntil: string | null
  totalAmount: number
  lines: QuotationLine[]
}

export interface QuotationLineRequest {
  itemId: number
  quantity: number
  unitPrice: number
}

export interface QuotationCreateRequest {
  customerId: number
  issuedDate: string
  validUntil: string | null
  lines: QuotationLineRequest[]
}

/** ⚠️ 수정 DTO 엔 customerId 가 없다 — 고객은 변경 불가. */
export interface QuotationUpdateRequest {
  issuedDate: string
  validUntil: string | null
  lines: QuotationLineRequest[]
}

/** 견적 상태 전이 액션(엔드포인트 경로 조각). */
export type QuotationAction = 'submit-approval' | 'send' | 'accept' | 'cancel'

// ── 영업: 수주 (sd/salesorder) ───────────────────────

export type SalesOrderStatus =
  | 'DRAFT'
  | 'CONFIRMED'
  | 'SHIPPING'
  | 'SHIPPED'
  | 'INVOICING'
  | 'INVOICED'
  | 'CLOSED'
  | 'CANCELLED'

export interface SalesOrderLine {
  id: number
  lineNo: number
  itemId: number
  itemCode: string
  itemName: string
  orderQty: number
  shippedQty: number
  invoicedQty: number
  unitPrice: number
  lineTotal: number
}

export interface SalesOrder extends Audited {
  id: number
  number: string
  customerId: number
  customerCode: string
  customerName: string
  salespersonId: number | null
  salespersonName: string | null
  quotationId: number | null
  quotationNumber: string | null
  status: SalesOrderStatus
  orderDate: string
  confirmedAt: string | null
  totalAmount: number
  lines: SalesOrderLine[]
}

export interface SalesOrderLineRequest {
  itemId: number
  orderQty: number
  unitPrice: number
}

export interface SalesOrderCreateRequest {
  customerId: number
  /** @NotNull 이 아니라 생략 가능 — 안 보내면 서버가 null 로 받는다. */
  salespersonId?: number | null
  /** 견적→수주 전환일 때만 채운다. */
  quotationId?: number | null
  orderDate: string
  lines: SalesOrderLineRequest[]
}

/** ⚠️ 수정 DTO 엔 customerId·quotationId 가 없다 — 둘 다 변경 불가. */
export interface SalesOrderUpdateRequest {
  salespersonId?: number | null
  orderDate: string
  lines: SalesOrderLineRequest[]
}

export type SalesOrderAction = 'confirm' | 'cancel' | 'close'

/** CreditStatusResponse — 수주 등록 시 여신 현황 표시용. */
export interface CreditStatus {
  customerId: number
  creditLimit: number
  used: number
  orderBacklog: number
  receivable: number
  remaining: number
}

// ── 영업: 출하 (sd/delivery) ─────────────────────────

export type DeliveryStatus = 'DRAFT' | 'SHIPPED' | 'CANCELLED'

export interface DeliveryLine {
  id: number
  lineNo: number
  salesOrderLineId: number
  itemId: number
  itemCode: string
  itemName: string
  quantity: number
}

/** ⚠️ warehouseName 이 없다 — 창고 이름은 프론트가 마스터로 id→name 매핑한다. */
export interface Delivery extends Audited {
  id: number
  number: string
  salesOrderId: number
  salesOrderNumber: string
  warehouseId: number
  status: DeliveryStatus
  shippedDate: string
  lines: DeliveryLine[]
}

export interface DeliveryLineRequest {
  salesOrderLineId: number
  quantity: number
}

export interface DeliveryCreateRequest {
  salesOrderId: number
  warehouseId: number
  shippedDate: string
  lines: DeliveryLineRequest[]
}

// ── 영업: 청구 (sd/invoice) ──────────────────────────

export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'CANCELLED'

export interface InvoiceLine {
  id: number
  lineNo: number
  salesOrderLineId: number
  itemId: number
  itemCode: string
  itemName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

/** 세액은 서버가 계산한다(subtotal + taxAmount = totalAmount). */
export interface Invoice extends Audited {
  id: number
  number: string
  salesOrderId: number
  salesOrderNumber: string
  status: InvoiceStatus
  invoiceDate: string
  subtotal: number
  taxAmount: number
  totalAmount: number
  lines: InvoiceLine[]
}

export interface InvoiceLineRequest {
  salesOrderLineId: number
  quantity: number
}

export interface InvoiceCreateRequest {
  salesOrderId: number
  invoiceDate: string
  lines: InvoiceLineRequest[]
}

// ── 구매: 발주 (mm/purchaseorder) ────────────────────

/** ⚠️ confirm 엔드포인트가 없다 — DRAFT 는 결재 상신으로만 CONFIRMED 가 된다. */
export type PurchaseOrderStatus = 'DRAFT' | 'CONFIRMED' | 'RECEIVED' | 'CLOSED' | 'CANCELLED'

export interface PurchaseOrderLine {
  id: number
  lineNo: number
  itemId: number
  itemCode: string
  itemName: string
  quantity: number
  unitPrice: number
  lineTotal: number
  receivedQuantity: number
  openQuantity: number
}

export interface PurchaseOrder extends Audited {
  id: number
  number: string
  vendorId: number
  vendorCode: string
  vendorName: string
  warehouseId: number
  warehouseCode: string
  warehouseName: string
  status: PurchaseOrderStatus
  orderDate: string
  expectedDate: string | null
  remark: string | null
  totalAmount: number
  lines: PurchaseOrderLine[]
}

export interface PurchaseOrderLineRequest {
  itemId: number
  quantity: number
  unitPrice: number
}

export interface PurchaseOrderCreateRequest {
  vendorId: number
  warehouseId: number
  orderDate: string
  expectedDate: string | null
  remark: string | null
  lines: PurchaseOrderLineRequest[]
}

export type PurchaseOrderUpdateRequest = PurchaseOrderCreateRequest

export type PurchaseOrderAction = 'submit-approval' | 'close' | 'cancel'

// ── 구매: 입고 (mm/receipt) ──────────────────────────

export type GoodsReceiptStatus = 'DRAFT' | 'POSTED' | 'CANCELLED'

/** ⚠️ 단가 필드명이 unitPrice 가 아니라 unitCost(발주 라인과 다르다). */
export interface GoodsReceiptLine {
  id: number
  lineNo: number
  itemId: number
  itemCode: string
  itemName: string
  quantity: number
  unitCost: number
  lineTotal: number
}

export interface GoodsReceipt extends Audited {
  id: number
  number: string
  vendorId: number
  vendorCode: string
  vendorName: string
  warehouseId: number
  warehouseCode: string
  warehouseName: string
  purchaseOrderId: number | null
  purchaseOrderNumber: string | null
  status: GoodsReceiptStatus
  receiptDate: string
  postedAt: string | null
  lines: GoodsReceiptLine[]
}

export interface GoodsReceiptLineRequest {
  itemId: number
  quantity: number
  unitCost: number
}

export interface GoodsReceiptCreateRequest {
  vendorId: number
  warehouseId: number
  receiptDate: string
  purchaseOrderId: number | null
  lines: GoodsReceiptLineRequest[]
}

export type GoodsReceiptAction = 'post' | 'cancel'

// ── 생산: 작업지시 (pp/productionorder) ──────────────

export type ProductionOrderStatus = 'PLANNED' | 'RELEASED' | 'COMPLETED' | 'CANCELLED'

export interface ProductionOrderLine {
  id: number
  lineNo: number
  componentItemId: number
  componentCode: string
  componentName: string
  requiredQty: number
  issuedUnitCost: number | null
}

export interface ProductionOrder extends Audited {
  id: number
  number: string
  productItemId: number
  productCode: string
  productName: string
  warehouseId: number
  warehouseName: string
  quantity: number
  status: ProductionOrderStatus
  orderDate: string
  dueDate: string | null
  completedAt: string | null
  /** MES 로 전송되면 채워진다(ERP→MES 연동). */
  mesWorkOrderNo: string | null
  mesDispatchedAt: string | null
  lines: ProductionOrderLine[]
}

/** 소요자재 라인은 서버가 BOM 을 전개해 자동 생성한다(BOM 없으면 409). */
export interface ProductionOrderCreateRequest {
  productItemId: number
  warehouseId: number
  quantity: number
  orderDate: string
  dueDate: string | null
}

/** MaterialAvailabilityResponse.Line */
export interface MaterialAvailabilityLine {
  componentItemId: number
  componentCode: string
  componentName: string
  requiredQty: number
  onHandQty: number
  sufficient: boolean
}

export interface MaterialAvailability {
  producible: boolean
  lines: MaterialAvailabilityLine[]
}

export type ProductionOrderAction = 'release' | 'complete' | 'cancel' | 'dispatch'

// ── 재무: 계정과목 (fi/account) ──────────────────────

export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE'
export type NormalSide = 'DEBIT' | 'CREDIT'

/** postable=false 는 분류(헤더) 계정 — 여기엔 분개할 수 없다(400). */
export interface Account extends Audited {
  id: number
  code: string
  name: string
  type: AccountType
  normalSide: NormalSide
  parentCode: string | null
  postable: boolean
  status: MasterStatus
}

// ── 재무: 전표 (fi/journal) ──────────────────────────

/** ⚠️ 취소는 POSTED 만 가능하다 — DRAFT 는 취소 대상이 아니다(409). SD/MM 과 반대. */
export type JournalEntryStatus = 'DRAFT' | 'POSTED' | 'CANCELLED'

/** 전표 출처 — MANUAL 만 사람이 직접 입력한 것, 나머지는 자동 분개. */
export type JournalSource = 'INV' | 'GI' | 'GR' | 'PAY' | 'PAYROLL' | 'PROD' | 'MANUAL'

export interface JournalLine {
  id: number
  lineNo: number
  accountId: number
  accountCode: string
  accountName: string
  debit: number
  credit: number
}

export interface JournalEntry extends Audited {
  id: number
  number: string
  entryDate: string
  description: string
  status: JournalEntryStatus
  sourceType: JournalSource
  /** MANUAL 이면 null, 그 외엔 출처 문서 id(다형성 참조 — FK 아님). */
  sourceId: number | null
  postedAt: string | null
  totalDebit: number
  totalCredit: number
  lines: JournalLine[]
}

/** ⚠️ 계정 지정이 accountId 가 아니라 accountCode. 차변·대변 중 한 쪽만 > 0 이어야 한다. */
export interface JournalLineRequest {
  accountCode: string
  debit: number
  credit: number
}

export interface JournalEntryCreateRequest {
  entryDate: string
  description: string
  lines: JournalLineRequest[]
}

// ── 재무: 입금/출금 (fi/payment) ─────────────────────

export type PaymentStatus = 'DRAFT' | 'POSTED'

/** RECEIPT=입금(차 현금/대 매출채권), DISBURSEMENT=출금(차 매입채무/대 현금). */
export type PaymentType = 'RECEIPT' | 'DISBURSEMENT'

/** ⚠️ 거래처 '이름'이 없다 — code 만 온다. */
export interface Payment extends Audited {
  id: number
  number: string
  type: PaymentType
  customerId: number | null
  customerCode: string | null
  vendorId: number | null
  vendorCode: string | null
  amount: number
  paymentDate: string
  status: PaymentStatus
  postedAt: string | null
  description: string | null
}

/**
 * ⚠️ type 과 party 의 정합성을 서버가 강제한다(400):
 *   RECEIPT      → customerId 필수, vendorId 를 지정하면 거부
 *   DISBURSEMENT → vendorId 필수, customerId 를 지정하면 거부
 * 그래서 두 필드를 동시에 갖지 않도록 유니온으로 좁힌다 — 잘못된 조합이 컴파일에서 걸린다.
 */
export type PaymentCreateRequest =
  | {
      type: 'RECEIPT'
      customerId: number
      vendorId?: never
      amount: number
      paymentDate: string
      description: string | null
    }
  | {
      type: 'DISBURSEMENT'
      vendorId: number
      customerId?: never
      amount: number
      paymentDate: string
      description: string | null
    }

// ── 전자결재 (approval) ──────────────────────────────

export type ApprovalStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'
export type ApprovalStepStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SKIPPED'
export type ApprovalStepType = 'APPROVAL' | 'AGREEMENT' | 'REFERENCE'

export type ApprovalDocType =
  | 'QUOTATION'
  | 'SALES_ORDER'
  | 'PURCHASE_ORDER'
  | 'PAYMENT'
  | 'JOURNAL'
  | 'CREDIT_LIMIT'

export interface ApprovalStep {
  stepNo: number
  type: ApprovalStepType
  typeLabel: string
  approver: string
  approverName: string
  deptName: string | null
  status: ApprovalStepStatus
  decidedAt: string | null
  /** 반려 사유는 returnReason 이 아니라 이 필드에만 담긴다. */
  comment: string | null
}

export interface Approval {
  id: number
  number: string
  docType: ApprovalDocType
  docTypeLabel: string
  refId: number
  refNo: string
  /** ⚠️ 옛 Thymeleaf 경로(/fi/payments/1)라 React 라우트와 다르다 — 쓰지 말 것. */
  docLink: string
  title: string
  amount: number | null
  status: ApprovalStatus
  currentStep: number
  requester: string
  requestedAt: string
  decidedAt: string | null
  /** 반송 사유만 담긴다(반려는 steps[].comment). */
  returnReason: string | null
  /** 서버가 결재선을 보고 판단 — 지금 이 사람이 처리할 차례인지. */
  myTurn: boolean
  steps: ApprovalStep[]
}

export interface ApprovalActionRequest {
  comment: string | null
}

export type ApprovalAction = 'approve' | 'reject' | 'return' | 'withdraw' | 'resubmit'

// ── 알림 (notification) ──────────────────────────────

export type NotificationType =
  | 'PRODUCTION_CANCELLED'
  | 'CREDIT_REQUEST_SUBMITTED'
  | 'CREDIT_REQUEST_APPROVED'
  | 'CREDIT_REQUEST_REJECTED'
  | 'APPROVAL_REQUESTED'
  | 'APPROVAL_APPROVED'
  | 'APPROVAL_REJECTED'
  | 'APPROVAL_RETURNED'
  | 'APPROVAL_REFERENCED'

export interface Notification {
  id: number
  type: NotificationType
  title: string
  message: string
  linkUrl: string | null
  read: boolean
  createdAt: string
}

// ── 대시보드 (sd/dashboard) ──────────────────────────

export interface RecentOrder {
  number: string
  customerName: string
  totalAmount: number
  status: SalesOrderStatus
  orderDate: string
}

/** 목록 API 클라 합산은 페이징 때문에 부정확해서 서버가 집계해 내려준다. */
export interface SdDashboard {
  thisMonthOrderCount: number
  thisMonthOrderAmount: number
  awaitingShipmentCount: number
  awaitingShipmentAmount: number
  uninvoicedCount: number
  uninvoicedAmount: number
  quotationToSendCount: number
  /** 수주 상태 → 건수. 서버가 모든 상태를 0 포함해 채워 내려준다. */
  pipeline: Record<SalesOrderStatus, number>
  recentOrders: RecentOrder[]
}

// ── 구매(MM) 대시보드 ────────────────────────────────
export interface MmRecentOrder {
  number: string
  vendorName: string
  totalAmount: number
  status: PurchaseOrderStatus
  orderDate: string
}

export interface MmDashboard {
  thisMonthOrderCount: number
  thisMonthOrderAmount: number
  /** 입고 대기(CONFIRMED). */
  awaitingReceiptCount: number
  awaitingReceiptAmount: number
  pipeline: Record<PurchaseOrderStatus, number>
  recentOrders: MmRecentOrder[]
}

// ── 생산(PP) 대시보드 ────────────────────────────────
export interface PpRecentOrder {
  number: string
  productName: string
  quantity: number
  status: ProductionOrderStatus
  orderDate: string
}

export interface PpDashboard {
  /** 진행중(RELEASED). */
  inProgressCount: number
  /** 완료 대기(PLANNED). */
  awaitingCompletionCount: number
  thisMonthOrderCount: number
  pipeline: Record<ProductionOrderStatus, number>
  recentOrders: PpRecentOrder[]
}

// ── 재무(FI) 대시보드 ────────────────────────────────
export interface FiDashboard {
  thisMonthSalesAmount: number
  thisMonthReceiptAmount: number
  /** 미수금 = 발행 인보이스 합 − 입금 합(0 하한). */
  accountsReceivable: number
  pendingJournalCount: number
  pendingCreditRequestCount: number
  accountsReceivableDefinition: string
}

// ── 인사(HR) 대시보드 ────────────────────────────────
export interface HrRecentHire {
  code: string
  name: string
  departmentName: string
  hireDate: string
}

export interface HrDashboard {
  activeEmployeeCount: number
  totalEmployeeCount: number
  departmentCount: number
  /** 'YYYY-MM'. */
  thisMonthPeriod: string
  /** 이번 달 급여대장 상태. 미생성이면 null. */
  thisMonthPayrollStatus: PayrollStatus | null
  thisMonthPayrollNet: number
  /** 전체 급여대장 상태별 건수. */
  payrollStatusPipeline: Record<PayrollStatus, number>
  /** 부서명 → 인원(많은 순). */
  departmentHeadcount: Record<string, number>
  recentHires: HrRecentHire[]
}

// ── 재고: 현재고 (mm/stock) ──────────────────────────

/** StockResponse — 조회 전용. 변경은 입고/출고/조정만 수행한다. */
export interface Stock {
  id: number
  itemId: number
  itemCode: string
  itemName: string
  warehouseId: number
  warehouseCode: string
  warehouseName: string
  qtyOnHand: number
  /** 가중평균 단가(입고 시 갱신). */
  averageCost: number
  /** 낙관적 락 버전. */
  version: number
  updatedAt: string
}

// ── 재고: 이동이력 (mm/stock-movement) ───────────────

/**
 * StockMovement 의 발생 사유 — 부호 방향이 정해져 있다(도메인이 강제).
 *  입고(+): GOODS_RECEIPT, ADJUSTMENT_PLUS, PRODUCTION_IN
 *  출고(−): GOODS_ISSUE, ADJUSTMENT_MINUS, SCRAP, PRODUCTION_OUT
 */
export type MovementReason =
  | 'GOODS_RECEIPT'
  | 'GOODS_ISSUE'
  | 'ADJUSTMENT_PLUS'
  | 'ADJUSTMENT_MINUS'
  | 'SCRAP'
  | 'PRODUCTION_OUT'
  | 'PRODUCTION_IN'

/** StockMovementResponse — 이동 원장 한 행. 입고/출고 트랜잭션이 자동으로 남긴다. */
export interface StockMovement {
  id: number
  itemId: number
  itemCode: string
  itemName: string
  warehouseId: number
  warehouseCode: string
  /** 부호가 방향을 나타낸다(+입고 / −출고). */
  qtyDelta: number
  unitCost: number
  reason: MovementReason
  /** 출처 문서 종류(GR·GI·PROD 등) + id — 다형성 참조(FK 아님). */
  refType: string | null
  refId: number | null
  movedAt: string
}

// ── 인사: 사원 (master/employee) ─────────────────────

/** EmployeeResponse — 조회는 넓은 역할, 쓰기는 ADMIN 전용. 목록은 배열(Page 아님). */
export interface Employee extends Audited {
  id: number
  code: string
  name: string
  email: string | null
  departmentCode: string | null
  departmentName: string | null
  hireDate: string
  status: MasterStatus
}

// ── 인사: 급여계약 (hr/contract) ─────────────────────

/** 직급 4단계 — 조회/리포트용 메타. 급여 계산엔 기본급이 진실. */
export type Position = 'STAFF' | 'SENIOR' | 'MANAGER' | 'DIRECTOR'

/** ACTIVE → terminate() → INACTIVE. */
export type ContractStatus = 'ACTIVE' | 'INACTIVE'

/** EmploymentContractResponse — 발효일 기준 급여 조건 이력. HR/ADMIN 전용. */
export interface EmploymentContract extends Audited {
  id: number
  employeeId: number
  employeeCode: string
  employeeName: string
  position: Position
  baseSalary: number
  contractedHours: number
  /** 시급 = baseSalary / contractedHours (서버 계산). */
  hourlyWage: number
  effectiveFrom: string
  /** null 이면 현재 유효(만료 안 됨). */
  effectiveTo: string | null
  status: ContractStatus
}

export interface EmploymentContractCreateRequest {
  employeeId: number
  position: Position
  baseSalary: number
  contractedHours: number
  effectiveFrom: string
}

// ── 인사: 근태 (hr/attendance) ───────────────────────

/**
 * AttendanceResponse — 하루 한 건. 소정근로 8h(480분) 초과분이 연장(overtime).
 * 조회는 employeeId + from~to 필수, 응답은 배열.
 */
export interface Attendance extends Audited {
  id: number
  employeeId: number
  employeeName: string
  workDate: string
  /** 'HH:mm:ss'. */
  clockIn: string
  clockOut: string
  workedMinutes: number
  overtimeMinutes: number
}

export interface AttendanceCreateRequest {
  employeeId: number
  workDate: string
  clockIn: string
  clockOut: string
}

// ── 인사: 급여대장 (hr/payroll) ──────────────────────

/**
 * PayrollStatus — DRAFT →confirm(인건비 전표) → CONFIRMED →pay(지급 전표) → PAID.
 * 확정=비용 인식, 지급=현금 유출 — 발생주의의 2단계.
 */
export type PayrollStatus = 'DRAFT' | 'CONFIRMED' | 'PAID'

/** PayslipResponse — 급여대장 한 사람 몫의 명세. */
export interface Payslip {
  id: number
  employeeId: number
  employeeCode: string
  employeeName: string
  basePay: number
  overtimePay: number
  grossPay: number
  incomeTax: number
  insuranceEmployee: number
  /** 회사 부담분(공제 아님, 참고). */
  insuranceCompany: number
  totalDeduction: number
  netPay: number
}

export interface PayrollRun extends Audited {
  id: number
  number: string
  /** 대상 월 'YYYY-MM'. */
  period: string
  runDate: string
  status: PayrollStatus
  totalGross: number
  totalDeduction: number
  totalNet: number
  confirmedAt: string | null
  paidAt: string | null
  payslips: Payslip[]
}

/** 대상 월(YYYY-MM)만 받는다 — 명세는 유효계약 + 근태로 자동 계산. */
export interface PayrollRunCreateRequest {
  period: string
}

export type PayrollAction = 'confirm' | 'pay'

// ── 리포트 (report) — 매출·재고·손익. 조회 전용, FINANCE/DIRECTOR/ADMIN ──

/** 매출 리포트 집계 단위. */
export type SalesReportUnit = 'DAY' | 'MONTH'

/** 매출 리포트 한 행 — period 는 일별 'yyyy-MM-dd', 월별 'yyyy-MM', 합계행은 '합계'. */
export interface SalesReportRow {
  period: string
  invoiceCount: number
  subtotal: number
  taxAmount: number
  totalAmount: number
}

export interface SalesReport {
  unit: SalesReportUnit
  from: string
  to: string
  rows: SalesReportRow[]
  /** 전체 합계행(period='합계'). */
  total: SalesReportRow
}

/** 재고 현황 리포트 한 행 — (품목, 창고)별 평가액. */
export interface InventoryReportRow {
  itemCode: string
  itemName: string
  warehouseName: string
  qtyOnHand: number
  averageCost: number
  /** 평가액 = 수량 × 평균단가(서버 계산). */
  valuationAmount: number
}

export interface InventoryReport {
  rows: InventoryReportRow[]
  totalValuation: number
}

/** 손익계산서 명세 한 줄 — 계정 단위 정상방향 순액. */
export interface IncomeStatementLine {
  accountCode: string
  accountName: string
  amount: number
}

/**
 * 손익계산서(미니).
 *   매출총이익 = 매출 − 매출원가
 *   영업이익   = 매출총이익 − 판관비
 *   당기순이익 = 수익총계 − 비용총계 (영업외 없는 미니 버전이라 영업이익과 동일)
 */
export interface IncomeStatement {
  from: string
  to: string
  revenue: number
  costOfGoodsSold: number
  grossProfit: number
  sgaExpense: number
  operatingProfit: number
  netIncome: number
  revenueLines: IncomeStatementLine[]
  expenseLines: IncomeStatementLine[]
}

// ── AI 어시스턴트 챗봇 (assistant) ───────────────────
// POST /api/assistant/chat → ERP 가 로컬 LLM 에이전트로 프록시. 로그인 전 부서 공용.

/** 1차 요청은 {message}, plan 확인 재전송은 {intent, confirm:true}. */
export interface AssistantRequest {
  message?: string
  /** plan 응답의 intent 를 그대로 되돌려 보낸다. 에이전트가 해석하므로 프론트는 통과만. */
  intent?: unknown
  confirm?: boolean
}

/**
 * 에이전트 응답. type 에 따라 렌더가 갈린다:
 *   message — 일반 답변(lines)
 *   result  — 조회/실행 결과(lines)
 *   error   — 오류·연결 실패(lines). 에이전트 미기동 시 백엔드가 503 으로 이 모양을 만든다.
 *   plan    — 쓰기 작업 미리보기(summary) + [실행]/[취소]. 실행 시 intent 를 confirm 재전송.
 */
export interface AssistantResponse {
  type: 'message' | 'result' | 'error' | 'plan'
  lines?: string[]
  summary?: string[]
  intent?: unknown
}

// ── 여신 상향 요청 (fi/credit) ───────────────────────
// 조회 SALES/FINANCE/ADMIN, 생성 SALES/ADMIN. 승인/거부는 전자결재(CREDIT_LIMIT)로 이관.

export type CreditRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** CreditLimitRequestResponse — 고객 여신 한도 상향 요청. */
export interface CreditLimitRequest {
  id: number
  number: string
  customerId: number
  customerName: string
  /** 현재 한도. */
  currentLimit: number
  /** 요청 한도. */
  requestedLimit: number
  reason: string | null
  status: CreditRequestStatus
  requestedBy: string
  createdAt: string
  decidedBy: string | null
  decidedAt: string | null
  decisionNote: string | null
}

export interface CreditLimitRequestCreateRequest {
  customerId: number
  requestedLimit: number
  reason: string | null
}

// ── BOM (pp/bom) ─────────────────────────────────────
// 조회 PRODUCTION/ADMIN. 완제품별 부품 소요량.

/** BomResponse — 완제품 1개당 부품 소요량 한 줄. */
export interface Bom {
  id: number
  productItemId: number
  productCode: string
  productName: string
  componentItemId: number
  componentCode: string
  componentName: string
  quantity: number
}

// ── 관리자: 사용자·역할 (security/admin) ─────────────
// ADMIN 전용. 사용자 목록·역할 목록·사용자 역할 교체.

/** 사용자에게 부여된 역할 요약(역할 편집 체크박스와 맞춤). */
export interface RoleRef {
  id: number
  code: string
  name: string
}

/** AdminUserResponse — ⚠️ passwordHash 는 서버가 응답에 담지 않는다. */
export interface AdminUser {
  id: number
  username: string
  employeeName: string | null
  enabled: boolean
  accountLocked: boolean
  roles: RoleRef[]
}

/** AdminRoleResponse — 역할 + 가진 권한 코드 목록. */
export interface AdminRole {
  id: number
  code: string
  name: string
  permissions: string[]
}

/** 사용자 역할 교체 — 선택된 역할 id 집합으로 통째 교체(비면 모든 역할 회수). */
export interface UpdateRolesRequest {
  roleIds: number[]
}

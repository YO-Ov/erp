// 백엔드(hwlee-mes) REST 응답·요청의 타입 정의.
// 각 타입은 Java 쪽 record 와 1:1 로 대응한다 — 서버 DTO 가 바뀌면 여기부터 고친다.
//
// 직렬화 규칙:
//   BigDecimal            → number
//   LocalDate             → string ('2026-07-17')
//   LocalDateTime         → string ('2026-07-17T09:30:00')
//   Java enum             → 같은 이름의 문자열 리터럴 유니온

// ── enum (com.hwlee.mes.*) ──

/** WorkOrderStatus — RECEIVED ─start▶ IN_PROGRESS ⇄ PAUSED ─complete▶ COMPLETED */
export type WorkOrderStatus = 'RECEIVED' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'CANCELLED'

/** EquipmentStatus — 가동률 분자=RUNNING, 분모=RUNNING+DOWN(IDLE·MAINTENANCE 는 계획정지라 제외) */
export type EquipmentStatus = 'RUNNING' | 'IDLE' | 'DOWN' | 'MAINTENANCE'

/** QualityResult */
export type QualityResult = 'PASS' | 'FAIL'

// ── 작업지시 (workorder) ──

/** WorkOrderResponse.LineResponse */
export interface WorkOrderLine {
  lineNo: number
  componentCode: string
  componentName: string
  requiredQty: number
  unit: string | null
}

/** WorkOrderResponse */
export interface WorkOrder {
  id: number
  workOrderNo: string
  erpOrderNo: string | null
  productCode: string
  productName: string
  quantity: number
  plannedDate: string | null
  status: WorkOrderStatus
  receivedAt: string | null
  producedQty: number
  defectQty: number
  assignedEquipmentCode: string | null
  assignedOperatorCode: string | null
  startedAt: string | null
  finishedAt: string | null
  lines: WorkOrderLine[]
}

/** WorkOrderReceiveRequest.ComponentLineRequest */
export interface ComponentLineRequest {
  componentCode: string
  componentName: string
  requiredQty: number
  unit: string | null
}

/** WorkOrderReceiveRequest — erpOrderNo 가 멱등 키. ⚠️ 요청 필드는 `components`(응답은 `lines`) */
export interface WorkOrderReceiveRequest {
  erpOrderNo: string
  productCode: string
  productName: string
  quantity: number
  plannedDate: string | null
  components: ComponentLineRequest[]
}

// ── 현장 실행 (performance) ──

/** StartRequest — 설비·작업자 배정 */
export interface StartRequest {
  equipmentId: number
  operatorId: number
}

/** ReportRequest — 부분 실적. 불량 발생 시 defectReasonId 선택 */
export interface ReportRequest {
  goodQty: number
  defectQty: number
  defectReasonId: number | null
}

/** ProductionResultResponse.ConsumptionResponse */
export interface Consumption {
  componentCode: string
  componentName: string
  consumedQty: number
}

/** ProductionResultResponse */
export interface ProductionResult {
  id: number
  seq: number
  goodQty: number
  defectQty: number
  reportedAt: string
  defectReasonCode: string | null
  defectReasonName: string | null
  consumptions: Consumption[]
}

// ── 품질 (quality) ──

/** InspectRequest — defectQty > 0 이면 defectReasonId 필수 */
export interface InspectRequest {
  inspectedQty: number
  passedQty: number
  defectQty: number
  defectReasonId: number | null
  note: string | null
}

/** QualityInspectionResponse */
export interface QualityInspection {
  id: number
  workOrderNo: string
  inspectedQty: number
  passedQty: number
  defectQty: number
  defectReasonCode: string | null
  defectReasonName: string | null
  result: QualityResult
  inspectedAt: string
  note: string | null
}

// ── 마스터 (master) ──

/** EquipmentController.EquipmentResponse */
export interface Equipment {
  id: number
  code: string
  name: string
  lineName: string | null
  status: EquipmentStatus
}

/** EquipmentStatusService.UtilizationResponse */
export interface Utilization {
  equipmentCode: string
  currentStatus: EquipmentStatus
  loadingSeconds: number
  operatingSeconds: number
  utilizationPercent: number
}

/** OperatorController.OperatorResponse */
export interface Operator {
  id: number
  code: string
  name: string
}

/** QualityController.DefectReasonResponse */
export interface DefectReason {
  id: number
  code: string
  name: string
}

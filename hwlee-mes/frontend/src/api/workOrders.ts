import client from './client'
import type {
  DefectReason,
  Equipment,
  EquipmentStatus,
  InspectRequest,
  Operator,
  ProductionResult,
  QualityInspection,
  ReportRequest,
  StartRequest,
  Utilization,
  WorkOrder,
  WorkOrderReceiveRequest,
} from '../types/api'

// 작업지시 · 실적 · 품질 · 마스터(설비/작업자/불량사유) REST 호출 모음.
// 실제 엔드포인트는 hwlee-mes 의 @RestController 들과 1:1로 대응한다.

/** 라우트 파라미터는 문자열로 들어오므로 id 는 둘 다 받는다. */
type Id = number | string

export const workOrderApi = {
  // ── 작업지시 (WorkOrderController: /api/work-orders) ──
  list: (): Promise<WorkOrder[]> => client.get<WorkOrder[]>('/work-orders').then((r) => r.data),
  findById: (id: Id): Promise<WorkOrder> =>
    client.get<WorkOrder>(`/work-orders/${id}`).then((r) => r.data),
  receive: (payload: WorkOrderReceiveRequest): Promise<WorkOrder> =>
    client.post<WorkOrder>('/work-orders', payload).then((r) => r.data),

  // ── 현장 실행 (PerformanceController: /api/work-orders/{id}/...) ──
  start: (id: Id, payload: StartRequest): Promise<WorkOrder> =>
    client.post<WorkOrder>(`/work-orders/${id}/start`, payload).then((r) => r.data),
  pause: (id: Id): Promise<WorkOrder> =>
    client.post<WorkOrder>(`/work-orders/${id}/pause`).then((r) => r.data),
  resume: (id: Id): Promise<WorkOrder> =>
    client.post<WorkOrder>(`/work-orders/${id}/resume`).then((r) => r.data),
  complete: (id: Id): Promise<WorkOrder> =>
    client.post<WorkOrder>(`/work-orders/${id}/complete`).then((r) => r.data),
  // ⚠️ 실적 보고만 작업지시가 아니라 '등록된 실적'을 돌려준다 — 갱신된 작업지시가 필요하면 findById 로 다시 읽을 것.
  reportResult: (id: Id, payload: ReportRequest): Promise<ProductionResult> =>
    client.post<ProductionResult>(`/work-orders/${id}/results`, payload).then((r) => r.data),
  results: (id: Id): Promise<ProductionResult[]> =>
    client.get<ProductionResult[]>(`/work-orders/${id}/results`).then((r) => r.data),

  // ── 품질 (QualityController) ──
  inspections: (id: Id): Promise<QualityInspection[]> =>
    client.get<QualityInspection[]>(`/work-orders/${id}/inspections`).then((r) => r.data),
  inspect: (id: Id, payload: InspectRequest): Promise<QualityInspection> =>
    client.post<QualityInspection>(`/work-orders/${id}/inspections`, payload).then((r) => r.data),
}

export const equipmentApi = {
  list: (): Promise<Equipment[]> => client.get<Equipment[]>('/equipments').then((r) => r.data),
  changeStatus: (id: Id, status: EquipmentStatus): Promise<EquipmentStatus> =>
    client.post<EquipmentStatus>(`/equipments/${id}/status`, { status }).then((r) => r.data),
  utilization: (id: Id): Promise<Utilization> =>
    client.get<Utilization>(`/equipments/${id}/utilization`).then((r) => r.data),
}

export const masterApi = {
  equipments: (): Promise<Equipment[]> => client.get<Equipment[]>('/equipments').then((r) => r.data),
  operators: (): Promise<Operator[]> => client.get<Operator[]>('/operators').then((r) => r.data),
  defectReasons: (): Promise<DefectReason[]> =>
    client.get<DefectReason[]>('/defect-reasons').then((r) => r.data),
}

import client from './client'

// 작업지시 · 실적 · 품질 · 마스터(설비/작업자/불량사유) REST 호출 모음.
// 실제 엔드포인트는 hwlee-mes 의 @RestController 들과 1:1로 대응한다.

export const workOrderApi = {
  // ── 작업지시 (WorkOrderController: /api/work-orders) ──
  list: () => client.get('/work-orders').then((r) => r.data),
  findById: (id) => client.get(`/work-orders/${id}`).then((r) => r.data),
  receive: (payload) => client.post('/work-orders', payload).then((r) => r.data),

  // ── 현장 실행 (PerformanceController: /api/work-orders/{id}/...) ──
  start: (id, payload) => client.post(`/work-orders/${id}/start`, payload).then((r) => r.data),
  pause: (id) => client.post(`/work-orders/${id}/pause`).then((r) => r.data),
  resume: (id) => client.post(`/work-orders/${id}/resume`).then((r) => r.data),
  complete: (id) => client.post(`/work-orders/${id}/complete`).then((r) => r.data),
  reportResult: (id, payload) =>
    client.post(`/work-orders/${id}/results`, payload).then((r) => r.data),
  results: (id) => client.get(`/work-orders/${id}/results`).then((r) => r.data),

  // ── 품질 (QualityController) ──
  inspections: (id) => client.get(`/work-orders/${id}/inspections`).then((r) => r.data),
  inspect: (id, payload) =>
    client.post(`/work-orders/${id}/inspections`, payload).then((r) => r.data),
}

export const equipmentApi = {
  list: () => client.get('/equipments').then((r) => r.data),
  changeStatus: (id, status) =>
    client.post(`/equipments/${id}/status`, { status }).then((r) => r.data),
  utilization: (id) => client.get(`/equipments/${id}/utilization`).then((r) => r.data),
}

export const masterApi = {
  equipments: () => client.get('/equipments').then((r) => r.data),
  operators: () => client.get('/operators').then((r) => r.data),
  defectReasons: () => client.get('/defect-reasons').then((r) => r.data),
}

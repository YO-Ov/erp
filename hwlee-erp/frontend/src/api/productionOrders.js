import client from './client'

// 생산 작업지시 REST API (/api/production-orders) — ProductionController 와 1:1.
// 권한: PRODUCTION/ADMIN.

export async function searchProductionOrders(params = {}) {
  const { data } = await client.get('/production-orders', { params })
  return data
}

export async function getProductionOrder(id) {
  const { data } = await client.get(`/production-orders/${id}`)
  return data
}

export async function createProductionOrder(body) {
  // body: { productItemId, warehouseId, quantity, orderDate, dueDate }
  // 완제품 BOM 을 전개해 소요 자재 라인이 자동 생성된다(BOM 없으면 409).
  const { data } = await client.post('/production-orders', body)
  return data
}

export async function productionOrderAction(id, action) {
  // action: release(착수) | complete(완료) | cancel | dispatch(MES 전송)
  const { data } = await client.post(`/production-orders/${id}/${action}`)
  return data
}

// 자재 가용성(BOM 소요량 vs 현재고) — 생산 가능 여부 미리보기.
export async function getMaterialAvailability(id) {
  const { data } = await client.get(`/production-orders/${id}/material-availability`)
  return data // { producible, lines: [{ componentItemId, componentName, requiredQty, onHandQty, sufficient }] }
}

import client from './client'

// 발주 REST API (/api/purchase-orders) — 백엔드 PurchaseOrderController 와 1:1.
// 조회는 여러 부서, 쓰기(생성/수정/상신/종료/취소)는 PURCHASING/ADMIN 만 허용된다.

export async function searchPurchaseOrders(params = {}) {
  const { data } = await client.get('/purchase-orders', { params })
  return data
}

export async function getPurchaseOrder(id) {
  const { data } = await client.get(`/purchase-orders/${id}`)
  return data
}

export async function createPurchaseOrder(body) {
  // body: { vendorId, warehouseId, orderDate, expectedDate, remark, lines: [{ itemId, quantity, unitPrice }] }
  const { data } = await client.post('/purchase-orders', body)
  return data
}

export async function updatePurchaseOrder(id, body) {
  const { data } = await client.put(`/purchase-orders/${id}`, body)
  return data
}

export async function purchaseOrderAction(id, action) {
  // action: submit-approval | close | cancel
  const { data } = await client.post(`/purchase-orders/${id}/${action}`)
  return data
}

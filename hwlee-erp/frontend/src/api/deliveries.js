import client from './client'

// 출하 REST API (/api/deliveries) — 백엔드 DeliveryController 와 1:1.

export async function searchDeliveries(params = {}) {
  const { data } = await client.get('/deliveries', { params })
  return data
}

export async function getDelivery(id) {
  const { data } = await client.get(`/deliveries/${id}`)
  return data
}

export async function createDelivery(body) {
  // body: { salesOrderId, warehouseId, shippedDate, lines: [{ salesOrderLineId, quantity }] }
  const { data } = await client.post('/deliveries', body)
  return data
}

export async function deliveryAction(id, action) {
  // action: cancel
  const { data } = await client.post(`/deliveries/${id}/${action}`)
  return data
}

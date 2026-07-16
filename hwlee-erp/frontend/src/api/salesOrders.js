import client from './client'

// 수주 REST API (/api/sales-orders) — 백엔드 SalesOrderController 와 1:1.

// 목록 검색 (페이징). params: { customerId, salespersonId, status, dateFrom, dateTo, page, size, sort }
export async function searchSalesOrders(params = {}) {
  const { data } = await client.get('/sales-orders', { params })
  return data
}

export async function getSalesOrder(id) {
  const { data } = await client.get(`/sales-orders/${id}`)
  return data
}

export async function createSalesOrder(body) {
  // body: { customerId, salespersonId, quotationId, orderDate, lines: [{ itemId, orderQty, unitPrice }] }
  const { data } = await client.post('/sales-orders', body)
  return data
}

export async function updateSalesOrder(id, body) {
  const { data } = await client.put(`/sales-orders/${id}`, body)
  return data
}

export async function salesOrderAction(id, action) {
  // action: confirm | cancel | close
  const { data } = await client.post(`/sales-orders/${id}/${action}`)
  return data
}

// 고객 여신 현황 (수주 생성 시 참고).
export async function getCreditStatus(customerId) {
  const { data } = await client.get('/sales-orders/credit-status', {
    params: { customerId },
  })
  return data // { customerId, creditLimit, used, orderBacklog, receivable, remaining }
}

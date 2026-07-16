import client from './client'

// 견적 REST API (/api/quotations) — 백엔드 QuotationController 와 1:1.

// 목록 검색 (페이징). params: { customerId, status, dateFrom, dateTo, page, size, sort }
export async function searchQuotations(params = {}) {
  const { data } = await client.get('/quotations', { params })
  return data // Spring Page: { content, totalElements, totalPages, number, size, ... }
}

export async function getQuotation(id) {
  const { data } = await client.get(`/quotations/${id}`)
  return data
}

export async function createQuotation(body) {
  // body: { customerId, issuedDate, validUntil, lines: [{ itemId, quantity, unitPrice }] }
  const { data } = await client.post('/quotations', body)
  return data
}

export async function updateQuotation(id, body) {
  const { data } = await client.put(`/quotations/${id}`, body)
  return data
}

// 상태 전이 액션들. 모두 POST /api/quotations/{id}/{action}
export async function quotationAction(id, action) {
  const { data } = await client.post(`/quotations/${id}/${action}`)
  return data
}

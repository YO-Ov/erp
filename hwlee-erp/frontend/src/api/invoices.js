import client from './client'

// 청구 REST API (/api/invoices) — 백엔드 InvoiceController 와 1:1.

export async function searchInvoices(params = {}) {
  const { data } = await client.get('/invoices', { params })
  return data
}

export async function getInvoice(id) {
  const { data } = await client.get(`/invoices/${id}`)
  return data
}

export async function createInvoice(body) {
  // body: { salesOrderId, invoiceDate, lines: [{ salesOrderLineId, quantity }] }
  const { data } = await client.post('/invoices', body)
  return data
}

export async function invoiceAction(id, action) {
  // action: cancel
  const { data } = await client.post(`/invoices/${id}/${action}`)
  return data
}

import client from './client'
import type {
  Invoice,
  InvoiceCreateRequest,
  InvoiceStatus,
  Page,
  PageParams,
} from '../types/api'

// 청구 REST API (/api/invoices) — 백엔드 InvoiceController 와 1:1.

export interface InvoiceSearchParams extends PageParams {
  salesOrderId?: number
  status?: InvoiceStatus
  dateFrom?: string
  dateTo?: string
}

export async function searchInvoices(params: InvoiceSearchParams = {}): Promise<Page<Invoice>> {
  const { data } = await client.get<Page<Invoice>>('/invoices', { params })
  return data
}

export async function getInvoice(id: string | number): Promise<Invoice> {
  const { data } = await client.get<Invoice>(`/invoices/${id}`)
  return data
}

export async function createInvoice(body: InvoiceCreateRequest): Promise<Invoice> {
  const { data } = await client.post<Invoice>('/invoices', body)
  return data
}

export async function invoiceAction(id: string | number, action: 'cancel'): Promise<Invoice> {
  const { data } = await client.post<Invoice>(`/invoices/${id}/${action}`)
  return data
}

import client from './client'
import type {
  Customer,
  CustomerContact,
  CustomerContactRequest,
  CustomerCreateRequest,
  CustomerUpdateRequest,
  MasterStatus,
  Page,
  PageParams,
} from '../types/api'

// 고객 마스터 REST API (/api/customers) — 백엔드 CustomerController 와 1:1.
// 조회는 넓은 역할, 등록·수정은 SALES/ADMIN, 삭제는 ADMIN 전용(백엔드가 강제).

export interface CustomerSearchParams extends PageParams {
  name?: string
  businessNo?: string
  status?: MasterStatus
}

export async function searchCustomers(
  params: CustomerSearchParams = {},
): Promise<Page<Customer>> {
  const { data } = await client.get<Page<Customer>>('/customers', { params })
  return data
}

export async function getCustomer(id: string | number): Promise<Customer> {
  const { data } = await client.get<Customer>(`/customers/${id}`)
  return data
}

export async function createCustomer(body: CustomerCreateRequest): Promise<Customer> {
  const { data } = await client.post<Customer>('/customers', body)
  return data
}

export async function updateCustomer(
  id: string | number,
  body: CustomerUpdateRequest,
): Promise<Customer> {
  const { data } = await client.put<Customer>(`/customers/${id}`, body)
  return data
}

export async function deleteCustomer(id: string | number): Promise<void> {
  await client.delete(`/customers/${id}`)
}

// ── 담당자(연락처) 서브리소스 (/api/customers/{id}/contacts) ──
// 조회는 넓은 역할, 추가·수정·삭제는 SALES/ADMIN(백엔드가 강제).

export async function getCustomerContacts(
  customerId: string | number,
): Promise<CustomerContact[]> {
  const { data } = await client.get<CustomerContact[]>(`/customers/${customerId}/contacts`)
  return data
}

export async function addCustomerContact(
  customerId: string | number,
  body: CustomerContactRequest,
): Promise<CustomerContact> {
  const { data } = await client.post<CustomerContact>(`/customers/${customerId}/contacts`, body)
  return data
}

export async function updateCustomerContact(
  customerId: string | number,
  contactId: number,
  body: CustomerContactRequest,
): Promise<CustomerContact> {
  const { data } = await client.put<CustomerContact>(
    `/customers/${customerId}/contacts/${contactId}`,
    body,
  )
  return data
}

export async function deleteCustomerContact(
  customerId: string | number,
  contactId: number,
): Promise<void> {
  await client.delete(`/customers/${customerId}/contacts/${contactId}`)
}

import client from './client'
import type { Customer, Item, Page, PageParams, Vendor, VendorItem, Warehouse } from '../types/api'

// 마스터 데이터 조회 — 폼의 드롭다운(고객·품목·창고·공급처) 채우기용.
// 응답은 Spring Page 이므로 content만 뽑아 쓴다.

export async function listCustomers(params: PageParams = { size: 200 }): Promise<Customer[]> {
  const { data } = await client.get<Page<Customer>>('/customers', { params })
  return data.content || []
}

export async function listItems(params: PageParams = { size: 500 }): Promise<Item[]> {
  const { data } = await client.get<Page<Item>>('/items', { params })
  return data.content || []
}

export async function listWarehouses(params: PageParams = { size: 100 }): Promise<Warehouse[]> {
  const { data } = await client.get<Page<Warehouse>>('/warehouses', { params })
  return data.content || []
}

export async function listVendors(params: PageParams = { size: 200 }): Promise<Vendor[]> {
  const { data } = await client.get<Page<Vendor>>('/vendors', { params })
  return data.content || []
}

// 특정 공급처의 취급품목(구매정보레코드). 발주 품목은 이 목록으로 제한된다.
export async function listVendorItems(
  vendorId: number,
  params: PageParams = { size: 300 },
): Promise<VendorItem[]> {
  const { data } = await client.get<Page<VendorItem>>('/vendor-items', {
    params: { vendorId, ...params },
  })
  return data.content || []
}

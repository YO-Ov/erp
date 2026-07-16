import client from './client'

// 마스터 데이터 조회 — 견적/수주 폼의 드롭다운(고객·품목) 채우기용.
// 응답은 Spring Page 이므로 content만 뽑아 쓴다.

export async function listCustomers(params = { size: 200 }) {
  const { data } = await client.get('/customers', { params })
  return data.content || []
}

export async function listItems(params = { size: 500 }) {
  const { data } = await client.get('/items', { params })
  return data.content || []
}

export async function listWarehouses(params = { size: 100 }) {
  const { data } = await client.get('/warehouses', { params })
  return data.content || []
}

export async function listVendors(params = { size: 200 }) {
  const { data } = await client.get('/vendors', { params })
  return data.content || []
}

// 특정 공급처의 취급품목(구매정보레코드). 발주 품목은 이 목록으로 제한된다.
export async function listVendorItems(vendorId, params = { size: 300 }) {
  const { data } = await client.get('/vendor-items', { params: { vendorId, ...params } })
  return data.content || []
}

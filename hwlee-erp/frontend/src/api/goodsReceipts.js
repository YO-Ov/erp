import client from './client'

// 입고 REST API (/api/goods-receipts) — 백엔드 GoodsReceiptController 와 1:1.
// 조회·쓰기 모두 PURCHASING/ADMIN 만 허용된다.

export async function searchGoodsReceipts(params = {}) {
  const { data } = await client.get('/goods-receipts', { params })
  return data
}

export async function getGoodsReceipt(id) {
  const { data } = await client.get(`/goods-receipts/${id}`)
  return data
}

export async function createGoodsReceipt(body) {
  // body: { vendorId, warehouseId, receiptDate, purchaseOrderId, lines: [{ itemId, quantity, unitCost }] }
  const { data } = await client.post('/goods-receipts', body)
  return data
}

export async function goodsReceiptAction(id, action) {
  // action: post(전기) | cancel
  const { data } = await client.post(`/goods-receipts/${id}/${action}`)
  return data
}

import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { getInventoryReport } from '../api/reports'
import { listItems, listWarehouses } from '../api/masters'
import { formatMoney } from '../domain/status'
import ReportTabs from '../components/ReportTabs'

// 재고 현황 리포트 — (품목, 창고)별 평가액 + 총계. 재고 조회 화면과 달리 평가액 중심.
export default function InventoryReportView() {
  const [itemId, setItemId] = useState<number | ''>('')
  const [warehouseId, setWarehouseId] = useState<number | ''>('')

  const { data: items = [] } = useQuery({ queryKey: ['items'], queryFn: () => listItems() })
  const { data: warehouses = [] } = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => listWarehouses(),
  })

  const params = {
    ...(itemId !== '' && { itemId }),
    ...(warehouseId !== '' && { warehouseId }),
  }
  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['inventory-report', params],
    queryFn: () => getInventoryReport(params),
    placeholderData: keepPreviousData,
  })

  return (
    <div className="container">
      <div className="page-head">
        <h1>리포트</h1>
      </div>
      <ReportTabs />

      <div className="toolbar">
        <div>
          <label>품목</label>
          <select
            value={itemId}
            onChange={(e) => setItemId(e.target.value === '' ? '' : Number(e.target.value))}
          >
            <option value="">전체</option>
            {items.map((it) => (
              <option key={it.id} value={it.id}>
                {it.name} ({it.code})
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>창고</label>
          <select
            value={warehouseId}
            onChange={(e) => setWarehouseId(e.target.value === '' ? '' : Number(e.target.value))}
          >
            <option value="">전체</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name}
              </option>
            ))}
          </select>
        </div>
        {data && (
          <div style={{ alignSelf: 'flex-end', marginLeft: 'auto' }}>
            <span className="muted">
              평가액 총계{' '}
              <strong className="mono" style={{ fontSize: 16 }}>
                {formatMoney(data.totalValuation)}
              </strong>{' '}
              원
            </span>
          </div>
        )}
      </div>

      <div className="panel">
        {isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : !data || data.rows.length === 0 ? (
          <p className="muted">조건에 맞는 재고가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>품목</th>
                <th>창고</th>
                <th className="num">보유수량</th>
                <th className="num">평균단가</th>
                <th className="num">평가액</th>
              </tr>
            </thead>
            <tbody>
              {data.rows.map((r, i) => (
                <tr key={`${r.itemCode}-${r.warehouseName}-${i}`}>
                  <td>
                    {r.itemName} <span className="muted mono">{r.itemCode}</span>
                  </td>
                  <td>{r.warehouseName}</td>
                  <td className="num mono">{formatMoney(r.qtyOnHand)}</td>
                  <td className="num mono">{formatMoney(r.averageCost)}</td>
                  <td className="num mono">{formatMoney(r.valuationAmount)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={4} style={{ textAlign: 'right' }}>
                  <strong>평가액 총계</strong>
                </td>
                <td className="num mono">
                  <strong>{formatMoney(data.totalValuation)}</strong>
                </td>
              </tr>
            </tfoot>
          </table>
        )}
        {isFetching && <p className="muted" style={{ fontSize: 12 }}>갱신중…</p>}
      </div>
    </div>
  )
}

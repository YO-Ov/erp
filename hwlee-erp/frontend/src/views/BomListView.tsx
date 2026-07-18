import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listBomsByProduct } from '../api/boms'
import { listItems } from '../api/masters'
import { formatMoney } from '../domain/status'

// BOM 조회 — 완제품을 고르면 그 제품 1개당 부품 소요량을 보여준다. 읽기 전용.
export default function BomListView() {
  const [productItemId, setProductItemId] = useState<number | ''>('')

  const { data: items = [] } = useQuery({ queryKey: ['items'], queryFn: () => listItems() })
  // 완제품만 BOM 을 갖는다 — 드롭다운을 FINISHED 로 제한한다.
  const finishedItems = items.filter((it) => it.itemType === 'FINISHED')

  const { data: boms = [], isLoading, isError, error } = useQuery({
    queryKey: ['boms', productItemId],
    queryFn: () => listBomsByProduct(productItemId as number),
    enabled: productItemId !== '',
  })

  return (
    <div className="container">
      <div className="page-head">
        <h1>BOM (자재명세서)</h1>
      </div>

      <div className="toolbar">
        <div style={{ flex: 1 }}>
          <label>완제품</label>
          <select
            value={productItemId}
            onChange={(e) => setProductItemId(e.target.value === '' ? '' : Number(e.target.value))}
            style={{ minWidth: 280 }}
          >
            <option value="">완제품 선택</option>
            {finishedItems.map((it) => (
              <option key={it.id} value={it.id}>
                {it.name} ({it.code})
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="panel">
        {productItemId === '' ? (
          <p className="muted">완제품을 선택하면 부품 소요량을 보여줍니다.</p>
        ) : isLoading ? (
          <p className="muted">불러오는 중…</p>
        ) : isError ? (
          <p className="error">{error.message}</p>
        ) : boms.length === 0 ? (
          <p className="muted">이 완제품에 등록된 BOM 이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>부품</th>
                <th className="num">소요량 (완제품 1개당)</th>
              </tr>
            </thead>
            <tbody>
              {boms.map((b) => (
                <tr key={b.id}>
                  <td>
                    {b.componentName} <span className="muted mono">{b.componentCode}</span>
                  </td>
                  <td className="num mono">{formatMoney(b.quantity)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {productItemId !== '' && boms.length > 0 && (
          <p className="muted" style={{ marginTop: 12, fontSize: 12 }}>
            부품 {boms.length}종 · 생산 작업지시 생성 시 이 BOM 을 전개해 소요 자재가 자동 산출됩니다.
          </p>
        )}
      </div>
    </div>
  )
}

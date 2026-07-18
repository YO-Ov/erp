import { NavLink } from 'react-router-dom'

// 리포트 3종이 공유하는 상단 탭. 활성 탭은 primary 버튼처럼 강조된다.
const TABS = [
  { to: '/reports/sales', label: '매출' },
  { to: '/reports/inventory', label: '재고 현황' },
  { to: '/reports/income-statement', label: '손익계산서' },
]

export default function ReportTabs() {
  return (
    <div className="actions" style={{ marginBottom: 16 }}>
      {TABS.map((t) => (
        <NavLink key={t.to} to={t.to}>
          {({ isActive }) => (
            <button className={isActive ? 'primary' : ''}>{t.label}</button>
          )}
        </NavLink>
      ))}
    </div>
  )
}

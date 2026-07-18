import { NavLink } from 'react-router-dom'

// 관리자 2종(사용자·역할)이 공유하는 상단 탭.
const TABS = [
  { to: '/admin/users', label: '사용자' },
  { to: '/admin/roles', label: '역할·권한' },
]

export default function AdminTabs() {
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

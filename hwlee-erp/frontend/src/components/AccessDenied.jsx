import { useAuth } from '../auth/AuthContext'

// 로그인은 했지만 이 화면에 필요한 역할이 없을 때 보여주는 안내.
// (백엔드가 403을 주기 전에 프론트 라우트 가드가 먼저 막는다)
const ROLE_LABEL = {
  SALES: '영업',
  PURCHASING: '구매',
  PRODUCTION: '생산',
  FINANCE: '재무',
  HR: '인사',
  ADMIN: '관리자',
  DIRECTOR: '임원',
}

function label(code) {
  return ROLE_LABEL[code] ? `${ROLE_LABEL[code]}(${code})` : code
}

export default function AccessDenied({ roles = [] }) {
  const { user } = useAuth()
  return (
    <div className="container">
      <div className="panel" style={{ maxWidth: 520, margin: '48px auto', textAlign: 'center' }}>
        <div style={{ fontSize: 40, marginBottom: 8 }}>🔒</div>
        <h1 style={{ fontSize: 20, margin: '0 0 8px' }}>접근 권한이 없습니다</h1>
        <p className="muted" style={{ margin: 0 }}>
          이 화면은 <strong>{roles.map(label).join(' 또는 ')}</strong> 권한이 필요합니다.
        </p>
        <p className="muted" style={{ marginTop: 12, fontSize: 13 }}>
          현재 로그인: {user?.username}{' '}
          <span style={{ opacity: 0.7 }}>({(user?.roles || []).map(label).join(', ') || '역할 없음'})</span>
        </p>
        <p className="muted" style={{ marginTop: 16, fontSize: 13 }}>
          권한이 있는 계정으로 다시 로그인하세요.
        </p>
      </div>
    </div>
  )
}

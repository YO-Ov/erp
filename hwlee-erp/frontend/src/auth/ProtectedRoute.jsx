import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'
import AccessDenied from '../components/AccessDenied'

// 화면 접근 가드.
//  - 미로그인 → 로그인 화면으로 (원래 가려던 경로 보존)
//  - roles 지정 시, 그 역할이 없으면 '권한 없음' 안내 (백엔드 403 전에 프론트가 먼저 막음)
export default function ProtectedRoute({ children, roles }) {
  const { user, hasRole } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (roles && roles.length > 0 && !hasRole(...roles)) {
    return <AccessDenied roles={roles} />
  }
  return children
}

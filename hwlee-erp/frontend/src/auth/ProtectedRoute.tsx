import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from './AuthContext'
import AccessDenied from '../components/AccessDenied'
import type { Role } from '../types/api'

// 화면 접근 가드.
//  - 미로그인 → 로그인 화면으로 (원래 가려던 경로 보존)
//  - roles 지정 시, 그 역할이 없으면 '권한 없음' 안내 (백엔드 403 전에 프론트가 먼저 막음)
//  - roles 생략 시 = 로그인만 하면 누구나(대시보드·전자결재)
export default function ProtectedRoute({
  children,
  roles,
}: {
  children: ReactNode
  roles?: readonly Role[]
}) {
  const { user, hasRole } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (roles && roles.length > 0 && !hasRole(...roles)) {
    return <AccessDenied roles={roles} />
  }
  // ReactNode 를 그대로 반환하면 컴포넌트 반환 타입과 안 맞아 fragment 로 감싼다.
  return <>{children}</>
}

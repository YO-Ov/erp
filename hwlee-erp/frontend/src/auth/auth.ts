import type { Role } from '../types/api'

// JWT 토큰 저장소 (localStorage 기반).
// 선택한 방식 = Bearer 헤더: 로그인 응답의 accessToken을 여기 저장하고
// api/client.ts 요청 인터셉터가 매 호출에 Authorization 헤더로 실어 보낸다.
// (백엔드는 Bearer 헤더·ACCESS_TOKEN 쿠키 둘 다 지원하지만 SPA 표준인 헤더 방식을 사용)

const TOKEN_KEY = 'erp_access_token'
const USER_KEY = 'erp_username'
const ROLES_KEY = 'erp_roles'

export interface AuthSnapshot {
  token: string
  username: string
  roles: Role[]
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function getUsername(): string | null {
  return localStorage.getItem(USER_KEY)
}

export function getRoles(): Role[] {
  const raw = localStorage.getItem(ROLES_KEY)
  if (!raw) return []
  try {
    // localStorage 는 무엇이든 담을 수 있어 파싱 결과를 신뢰할 수 없다 — 배열인지 확인한다.
    const parsed: unknown = JSON.parse(raw)
    return Array.isArray(parsed) ? (parsed as Role[]) : []
  } catch {
    return []
  }
}

export function setAuth({ token, username, roles }: AuthSnapshot): void {
  localStorage.setItem(TOKEN_KEY, token)
  if (username) localStorage.setItem(USER_KEY, username)
  localStorage.setItem(ROLES_KEY, JSON.stringify(roles || []))
}

export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(ROLES_KEY)
}

export function isLoggedIn(): boolean {
  return !!getToken()
}

// JWT 토큰 저장소 (localStorage 기반).
// 선택한 방식 = Bearer 헤더: 로그인 응답의 accessToken을 여기 저장하고
// api/client.js 요청 인터셉터가 매 호출에 Authorization 헤더로 실어 보낸다.
// (백엔드는 Bearer 헤더·ACCESS_TOKEN 쿠키 둘 다 지원하지만 SPA 표준인 헤더 방식을 사용)

const TOKEN_KEY = 'erp_access_token'
const USER_KEY = 'erp_username'
const ROLES_KEY = 'erp_roles'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getUsername() {
  return localStorage.getItem(USER_KEY)
}

export function getRoles() {
  const raw = localStorage.getItem(ROLES_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw)
  } catch {
    return []
  }
}

export function setAuth({ token, username, roles }) {
  localStorage.setItem(TOKEN_KEY, token)
  if (username) localStorage.setItem(USER_KEY, username)
  localStorage.setItem(ROLES_KEY, JSON.stringify(roles || []))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(ROLES_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}

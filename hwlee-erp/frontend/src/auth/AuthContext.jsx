import { createContext, useContext, useEffect, useState } from 'react'
import client from '../api/client'
import { getToken, getUsername, getRoles, setAuth, clearAuth } from './auth'

// 로그인 상태를 앱 전체에 공유하는 컨텍스트.
// - user: { username, roles } | null
// - login(username, password): POST /api/auth/login → 토큰 저장
// - logout(): 서버 로그아웃 + 로컬 토큰 제거
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() =>
    getToken() ? { username: getUsername(), roles: getRoles() } : null,
  )

  // client.js가 401을 만나면 쏘는 전역 이벤트를 듣고 로그아웃 상태로 되돌린다.
  useEffect(() => {
    const onUnauthorized = () => setUser(null)
    window.addEventListener('erp:unauthorized', onUnauthorized)
    return () => window.removeEventListener('erp:unauthorized', onUnauthorized)
  }, [])

  async function login(username, password) {
    const { data } = await client.post('/auth/login', { username, password })
    setAuth({ token: data.accessToken, username, roles: data.roles })
    setUser({ username, roles: data.roles })
    return data
  }

  async function logout() {
    try {
      await client.post('/auth/logout')
    } catch {
      // 서버 로그아웃 실패해도 로컬 세션은 반드시 정리한다.
    }
    clearAuth()
    setUser(null)
  }

  const hasRole = (...roles) => roles.some((r) => user?.roles?.includes(r))

  return (
    <AuthContext.Provider value={{ user, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import client from '../api/client'
import { getToken, getUsername, getRoles, setAuth, clearAuth } from './auth'
import type { LoginResponse, Role } from '../types/api'

// 로그인 상태를 앱 전체에 공유하는 컨텍스트.
// - user: { username, roles } | null
// - login(username, password): POST /api/auth/login → 토큰 저장
// - logout(): 서버 로그아웃 + 로컬 토큰 제거

export interface AuthUser {
  username: string
  roles: Role[]
}

interface AuthContextValue {
  user: AuthUser | null
  login: (username: string, password: string) => Promise<LoginResponse>
  logout: () => Promise<void>
  hasRole: (...roles: Role[]) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const token = getToken()
    const username = getUsername()
    // 토큰과 사용자명이 모두 있어야 유효한 세션이다.
    return token && username ? { username, roles: getRoles() } : null
  })

  // client.ts가 401을 만나면 쏘는 전역 이벤트를 듣고 로그아웃 상태로 되돌린다.
  useEffect(() => {
    const onUnauthorized = () => setUser(null)
    window.addEventListener('erp:unauthorized', onUnauthorized)
    return () => window.removeEventListener('erp:unauthorized', onUnauthorized)
  }, [])

  async function login(username: string, password: string): Promise<LoginResponse> {
    const { data } = await client.post<LoginResponse>('/auth/login', { username, password })
    const roles = (data.roles || []) as Role[]
    setAuth({ token: data.accessToken, username, roles })
    setUser({ username, roles })
    return data
  }

  async function logout(): Promise<void> {
    try {
      await client.post('/auth/logout')
    } catch {
      // 서버 로그아웃 실패해도 로컬 세션은 반드시 정리한다.
    }
    clearAuth()
    setUser(null)
  }

  const hasRole = (...roles: Role[]) => roles.some((r) => user?.roles?.includes(r))

  return (
    <AuthContext.Provider value={{ user, login, logout, hasRole }}>{children}</AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

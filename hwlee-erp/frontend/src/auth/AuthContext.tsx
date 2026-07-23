import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import client from '../api/client'
import { getToken, getUsername, getRoles, setAuth, clearAuth } from './auth'
import type { LoginResponse, MeResponse, Role } from '../types/api'

// 로그인 상태를 앱 전체에 공유하는 컨텍스트.
// - user: { username, roles } | null
// - bootstrapping: 부팅 시 저장 토큰을 서버(/auth/me)에 검증하는 중 (true 동안 로딩 게이트)
// - login(username, password): POST /api/auth/login → 토큰 저장
// - logout(): 서버 로그아웃 + 로컬 토큰 제거

export interface AuthUser {
  username: string
  roles: Role[]
}

interface AuthContextValue {
  user: AuthUser | null
  bootstrapping: boolean
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
    // ⚠️ 이건 localStorage 존재 여부만 본 '낙관적' 초기값이다 — 토큰이 이미
    //    만료/무효일 수 있으므로 아래 useEffect 가 /auth/me 로 서버 검증한다.
    return token && username ? { username, roles: getRoles() } : null
  })

  // 저장 토큰이 있으면 부팅 시 서버 검증이 끝날 때까지 게이트를 건다.
  // 토큰이 아예 없으면 검증할 것도 없으니 곧장 로그인 화면으로.
  const [bootstrapping, setBootstrapping] = useState<boolean>(() => !!getToken())

  // 부팅 검증: 저장된 토큰이 서버에서도 유효한지 /auth/me 로 확인한다.
  // - 유효 → 서버가 준 최신 username/roles 로 세션을 갱신
  // - 만료/무효(401) → client 인터셉터가 clearAuth + 'erp:unauthorized' 를 쏴
  //   아래 리스너가 user=null 로 되돌린다 → 로그인 화면
  // 이 게이트가 없으면 죽은 토큰으로 대시보드가 잠깐 렌더됐다가 튕긴다.
  useEffect(() => {
    if (!getToken()) return
    let alive = true
    client
      .get<MeResponse>('/auth/me')
      .then(({ data }) => {
        if (!alive) return
        const roles = (data.roles || []) as Role[]
        setAuth({ token: getToken()!, username: data.username, roles })
        setUser({ username: data.username, roles })
      })
      .catch(() => {
        // 401 은 인터셉터가 이미 정리했다. 그 밖의 실패(네트워크 등)도
        // 안전하게 미인증으로 떨궈 로그인부터 다시 하게 한다.
        if (alive) setUser(null)
      })
      .finally(() => {
        if (alive) setBootstrapping(false)
      })
    return () => {
      alive = false
    }
  }, [])

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
    <AuthContext.Provider value={{ user, bootstrapping, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

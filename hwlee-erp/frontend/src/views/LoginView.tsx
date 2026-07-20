import { useEffect, useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { canAccessPath } from '../auth/routeRoles'
import { errorMessage } from '../api/client'
import type { Role } from '../types/api'

// 시연용 빠른 선택 계정 (기존 Thymeleaf 로그인 화면과 동일).
// 계정을 고르면 이메일·공통 비밀번호(pass1234)가 자동 입력된다.
const DEMO_ACCOUNTS = [
  {
    group: '영업 (SD)',
    options: [
      { email: 'kim@hyunwoo.com', label: '담당 · kim' },
      { email: 'sales.global@hyunwoo.com', label: '담당(해외영업팀) · sales.global' },
      { email: 'sales.mgr@hyunwoo.com', label: '팀장(국내영업1팀) · sales.mgr' },
      { email: 'sales.dir@hyunwoo.com', label: '본부장(영업본부) · sales.dir' },
    ],
  },
  {
    group: '재무·경영지원 (FI)',
    options: [
      { email: 'lee@hyunwoo.com', label: '담당 · lee' },
      { email: 'finance.mgr@hyunwoo.com', label: '팀장(재무팀) · finance.mgr' },
      { email: 'mgmt.dir@hyunwoo.com', label: '본부장(경영지원본부) · mgmt.dir' },
    ],
  },
  {
    group: '구매 (MM)',
    options: [
      { email: 'purchase@hyunwoo.com', label: '담당 · purchase' },
      { email: 'purchase.mgr@hyunwoo.com', label: '팀장(구매팀) · purchase.mgr' },
    ],
  },
  {
    group: '생산 (PP)',
    options: [
      { email: 'park@hyunwoo.com', label: '담당 · park' },
      { email: 'prod.sw@hyunwoo.com', label: '팀장(수원생산팀) · prod.sw' },
      { email: 'prod.gm@hyunwoo.com', label: '팀장(구미생산팀) · prod.gm' },
      { email: 'prod.dir@hyunwoo.com', label: '본부장(생산본부) · prod.dir' },
    ],
  },
  {
    group: '인사 (HR)',
    options: [
      { email: 'jung@hyunwoo.com', label: '담당 · jung' },
      { email: 'hr.mgr@hyunwoo.com', label: '팀장(인사팀) · hr.mgr' },
    ],
  },
  {
    group: '대표·관리자',
    options: [{ email: 'admin@hyunwoo.com', label: '대표/관리자 (ADMIN) · admin' }],
  },
]

// 로그인 화면. 성공하면 원래 가려던 경로(또는 견적 목록)로 이동한다.
export default function LoginView() {
  const { login, user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  // 원래 가려던 경로가 없으면 대시보드로 — 역할 무관이라 어떤 계정이든 안전하다.
  // (예전 기본값이 /quotations 라 영업이 아닌 역할은 로그인하자마자 '권한 없음'을 봤다)
  const from = location.state?.from?.pathname || '/dashboard'

  // 🐞 로그아웃 → 다른 역할로 로그인하면 '권한 없음'이 뜨던 문제.
  //    로그아웃 시 ProtectedRoute 가 (아직 그 화면에 있는 채로) 세션이 끊긴 걸 보고
  //    <Navigate to="/login" state={{ from: 현재경로 }}> 를 태운다. 그래서 재무 화면에서
  //    로그아웃하면 from 에 '/journal-entries' 가 남고, 다음에 영업 계정으로 로그인해도
  //    거기로 끌려가 AccessDenied 를 봤다.
  //    → from 을 그대로 믿지 않고 **이 계정이 실제로 들어갈 수 있는 경로일 때만** 사용한다.
  //      (렌더 순서에 기대지 않으므로 안정적이고, 북마크·딥링크로 남의 화면에 로그인한
  //       경우까지 같이 해결된다.)
  const destinationFor = (roles: readonly Role[]) =>
    canAccessPath(from, roles) ? from : '/dashboard'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  // 이미 로그인돼 있으면(뒤로가기 등으로 /login 에 온 경우) 갈 곳으로 보낸다.
  // ⚠️ 렌더 도중 navigate 하면 React 경고가 나므로 effect 에서 처리한다.
  useEffect(() => {
    if (user) navigate(destinationFor(user.roles), { replace: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user])

  // 빠른 선택: 계정을 고르면 이메일/비밀번호(공통 pass1234) 자동 입력.
  function onQuickPick(email: string) {
    if (!email) return
    setUsername(email)
    setPassword('pass1234')
    setError(null)
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      // 컨텍스트의 user 는 아직 갱신 전일 수 있으므로 응답의 역할을 그대로 쓴다.
      const res = await login(username.trim(), password)
      navigate(destinationFor((res.roles || []) as Role[]), { replace: true })
    } catch (err) {
      // client.ts 인터셉터가 모든 실패를 Error 로 정규화하지만 catch 타입은 unknown 이다.
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-wrap">
      <div className="panel login-card">
        <h1>HYUNWOO ERP 로그인</h1>
        <p className="muted" style={{ marginTop: 0 }}>
          통합 자원관리 시스템
        </p>
        <form onSubmit={onSubmit}>
          <div className="field">
            <label>
              빠른 선택 <span className="muted">(시연용)</span>
            </label>
            <select
              defaultValue=""
              onChange={(e) => onQuickPick(e.target.value)}
              style={{ width: '100%' }}
            >
              <option value="">— 계정을 선택하면 자동 입력 —</option>
              {DEMO_ACCOUNTS.map((g) => (
                <optgroup key={g.group} label={g.group}>
                  {g.options.map((o) => (
                    <option key={o.email} value={o.email}>
                      {o.label}
                    </option>
                  ))}
                </optgroup>
              ))}
            </select>
          </div>
          <div className="field">
            <label>이메일</label>
            <input
              type="email"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="kim@hyunwoo.com"
              style={{ width: '100%' }}
              required
            />
          </div>
          <div className="field">
            <label>비밀번호</label>
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="pass1234"
              style={{ width: '100%' }}
              required
            />
          </div>
          {error && (
            <p className="error" style={{ marginTop: 0 }}>
              {error}
            </p>
          )}
          <button
            className="primary"
            type="submit"
            disabled={loading}
            style={{ width: '100%', marginTop: 4 }}
          >
            {loading ? '로그인 중…' : '로그인'}
          </button>
        </form>
      </div>
    </div>
  )
}

import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

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
  const from = location.state?.from?.pathname || '/quotations'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  // 이미 로그인돼 있으면 목록으로.
  if (user) {
    navigate(from, { replace: true })
    return null
  }

  // 빠른 선택: 계정을 고르면 이메일/비밀번호(공통 pass1234) 자동 입력.
  function onQuickPick(email) {
    if (!email) return
    setUsername(email)
    setPassword('pass1234')
    setError(null)
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(username.trim(), password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err.message)
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

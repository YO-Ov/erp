import axios from 'axios'
import { getToken, clearAuth } from '../auth/auth'

// 모든 API 호출의 공통 진입점.
// baseURL '/api' — 개발 중엔 Vite 프록시가 8080으로 넘기고,
// 운영 정적 배포에선 같은 도메인(erp.hyunwoo.pro)의 /api로 나간다.
const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

// 요청마다 저장된 JWT를 Authorization: Bearer 헤더로 첨부한다.
// (MES와 결정적으로 다른 점 — ERP는 로그인/토큰이 필수)
client.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 응답 인터셉터:
//  - 401(토큰 만료/무효) → 저장 토큰을 비우고 전역 이벤트를 쏜다.
//    AuthContext가 이 이벤트를 듣고 로그인 화면으로 되돌린다.
//  - 그 외 에러는 Spring ProblemDetail(detail/title/fieldErrors)을
//    사람이 읽을 수 있는 한 줄 메시지로 정규화한다.
client.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      clearAuth()
      window.dispatchEvent(new Event('erp:unauthorized'))
    }

    const data = error.response?.data
    let message =
      data?.detail ||
      data?.message ||
      data?.title ||
      (typeof data === 'string' ? data : null) ||
      error.message ||
      '알 수 없는 오류가 발생했습니다.'

    // 검증 실패(400)면 필드별 메시지를 덧붙인다.
    if (data?.fieldErrors) {
      const parts = Object.entries(data.fieldErrors).map(([k, v]) => `${k}: ${v}`)
      if (parts.length) message = `${message} (${parts.join(', ')})`
    }
    return Promise.reject(new Error(message))
  },
)

export default client

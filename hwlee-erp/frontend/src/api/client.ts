import axios, { AxiosError } from 'axios'
import { getToken, clearAuth } from '../auth/auth'

// 모든 API 호출의 공통 진입점.
// baseURL '/api' — 개발 중엔 Vite 프록시가 8080으로 넘기고,
// 운영 정적 배포에선 같은 도메인(erp.hyunwoo.pro)의 /api로 나간다.
const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

/**
 * Spring 이 에러 때 실어 보내는 본문 — RFC 7807 ProblemDetail.
 * GlobalExceptionHandler 가 도메인 에러에 code 를 덧붙인다(예: UNBALANCED_JOURNAL).
 */
interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  message?: string
  /** 도메인 에러 코드 — UNBALANCED_JOURNAL, INSUFFICIENT_STOCK 등. */
  code?: string
  /** Bean Validation 실패 시 필드별 메시지. */
  fieldErrors?: Record<string, string>
}

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
//  - 그 외 에러는 ProblemDetail(detail/title/fieldErrors)을
//    사람이 읽을 수 있는 한 줄 메시지로 정규화한다.
client.interceptors.response.use(
  (res) => res,
  (error: AxiosError<ProblemDetail | string>) => {
    const status = error.response?.status
    if (status === 401) {
      clearAuth()
      window.dispatchEvent(new Event('erp:unauthorized'))
    }

    const data = error.response?.data
    const problem = typeof data === 'object' && data !== null ? data : null
    let message =
      problem?.detail ||
      problem?.message ||
      problem?.title ||
      (typeof data === 'string' ? data : null) ||
      error.message ||
      '알 수 없는 오류가 발생했습니다.'

    // 검증 실패(400)면 필드별 메시지를 덧붙인다.
    if (problem?.fieldErrors) {
      const parts = Object.entries(problem.fieldErrors).map(([k, v]) => `${k}: ${v}`)
      if (parts.length) message = `${message} (${parts.join(', ')})`
    }
    return Promise.reject(new Error(message))
  },
)

/**
 * catch 로 잡힌 값에서 화면에 띄울 메시지를 뽑는다.
 * 위 인터셉터가 모든 실패를 Error 로 정규화하지만, catch 파라미터의 타입은 unknown 이라 좁혀줘야 한다.
 */
export function errorMessage(e: unknown): string {
  return e instanceof Error ? e.message : '알 수 없는 오류가 발생했습니다.'
}

export default client

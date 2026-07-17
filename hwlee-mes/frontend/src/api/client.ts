import axios, { AxiosError } from 'axios'

// 모든 API 호출의 공통 진입점.
// baseURL은 '/api' — 개발 중에는 Vite 프록시가 8082로 넘기고,
// 운영에서 정적 파일로 배포될 때는 같은 도메인(mes.hyunwoo.pro)의 /api로 나간다.
const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

/** 서버가 에러 때 실어 보내는 본문 — Spring 기본 에러 응답 / ProblemDetail 둘 다 커버한다. */
interface ErrorBody {
  message?: string
  error?: string
}

// 서버가 내려주는 에러 메시지를 최대한 사람이 읽을 수 있는 형태로 정규화한다.
client.interceptors.response.use(
  (res) => res,
  (error: AxiosError<ErrorBody | string>) => {
    const data = error.response?.data
    const message =
      (typeof data === 'object' && data !== null ? data.message || data.error : null) ||
      (typeof data === 'string' ? data : null) ||
      error.message ||
      '알 수 없는 오류가 발생했습니다.'
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

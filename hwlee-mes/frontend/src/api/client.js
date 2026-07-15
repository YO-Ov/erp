import axios from 'axios'

// 모든 API 호출의 공통 진입점.
// baseURL은 '/api' — 개발 중에는 Vite 프록시가 8082로 넘기고,
// 운영에서 정적 파일로 배포될 때는 같은 도메인(mes.hyunwoo.pro)의 /api로 나간다.
const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

// 서버가 내려주는 에러 메시지를 최대한 사람이 읽을 수 있는 형태로 정규화한다.
client.interceptors.response.use(
  (res) => res,
  (error) => {
    const data = error.response?.data
    const message =
      data?.message ||
      data?.error ||
      (typeof data === 'string' ? data : null) ||
      error.message ||
      '알 수 없는 오류가 발생했습니다.'
    return Promise.reject(new Error(message))
  },
)

export default client

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// ERP 백엔드(Spring Boot)는 8080 포트에서 동작한다.
// 개발 중에는 Vite dev 서버(5174)가 /api 요청을 8080으로 프록시해서
// CORS 설정 없이(=백엔드 무수정) 실제 REST API를 그대로 호출한다.
// 프록시가 same-origin이라 JWT는 Authorization 헤더로 그대로 전달된다.
const ERP_API = process.env.ERP_API_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    // MES(5173)와 포트가 겹치지 않게 5174 사용.
    port: 5174,
    proxy: {
      '/api': {
        target: ERP_API,
        changeOrigin: true,
      },
    },
  },
})

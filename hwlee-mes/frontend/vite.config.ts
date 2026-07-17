import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// MES 백엔드(Spring Boot)는 8082 포트에서 동작한다.
// 개발 중에는 Vite dev 서버(5173)가 /api 요청을 8082로 프록시해서
// CORS 설정 없이(=백엔드 무수정) 실제 REST API를 그대로 호출한다.
const MES_API = process.env.MES_API_TARGET || 'http://localhost:8082'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: MES_API,
        changeOrigin: true,
      },
    },
  },
})

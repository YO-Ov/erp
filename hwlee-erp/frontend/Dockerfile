# syntax=docker/dockerfile:1
# ─────────────────────────────────────────────────────────────
# ERP 프론트엔드(React SPA) 멀티스테이지 빌드
#   ① node 스테이지: npm ci → (tsc -b + vite build) → /app/dist 생성
#   ② caddy 스테이지: dist 정적 파일만 담아 초경량 정적 서버로 서빙
#
#   - 이 컨테이너는 순수 정적 파일 서버다(REST API 없음).
#   - '/api' 는 여기서 처리하지 않는다 → 바깥(메인 Caddy)이 erp:8080 으로 프록시.
#   - SPA 딥링크 폴백(try_files → index.html)은 아래 Caddyfile 이 담당.
#   - 인증은 JWT Bearer 헤더 방식이라 정적 서빙과 무관(localStorage 토큰).
# ─────────────────────────────────────────────────────────────

FROM node:22-alpine AS build
WORKDIR /app
# 의존성 레이어 캐시: package 파일만 먼저 복사해 설치
COPY package.json package-lock.json ./
RUN npm ci
# 소스 복사 후 빌드 (API baseURL 은 '/api' 고정 → 같은 도메인으로 나감)
# build 스크립트가 tsc -b 를 먼저 돌리므로 타입 에러가 있으면 이미지 빌드가 실패한다.
COPY . .
RUN npm run build

FROM caddy:2-alpine
# 빌드 산출물만 정적 루트로 복사
COPY --from=build /app/dist /srv
# 정적 서버 설정(SPA 폴백)
COPY Caddyfile /etc/caddy/Caddyfile
EXPOSE 80

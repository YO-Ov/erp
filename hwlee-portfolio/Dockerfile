# syntax=docker/dockerfile:1
# ─────────────────────────────────────────────────────────────
# 포트폴리오 정적 사이트 (hyunwoo.pro)
#   순수 HTML/CSS/JS 단일 페이지라 빌드 과정이 없다 — caddy 에 그대로 복사.
#   ERP·MES 프론트와 달리 node 빌드 스테이지가 필요 없다.
#   외부 포트를 열지 않는다 → 메인 Caddy 가 hyunwoo.pro 를 이 컨테이너로 프록시.
# ─────────────────────────────────────────────────────────────
FROM caddy:2-alpine
COPY index.html /srv/index.html
COPY Caddyfile /etc/caddy/Caddyfile
EXPOSE 80

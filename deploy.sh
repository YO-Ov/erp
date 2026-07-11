#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# 손배포 스크립트 — 서버에서 실행.
#   최신 코드 pull → 인프라+앱 통합 빌드·기동 → 상태 출력.
# 사용:  ./deploy.sh
# 전제:  같은 폴더에 .env 존재 (.env.example 복사해 값 채울 것)
# ─────────────────────────────────────────────────────────────
set -euo pipefail
cd "$(dirname "$0")"

COMPOSE_FILE="docker-compose.prod.yml"

if [ ! -f .env ]; then
  echo "⚠️  .env 가 없습니다. 'cp .env.example .env' 후 값을 채우세요." >&2
  exit 1
fi

echo "▶ [1/3] 최신 코드 가져오기 (git pull)"
git pull --ff-only

echo "▶ [2/4] 빌드 + 기동 (인프라 4개 + 앱 2개 + Caddy)"
docker compose -f "$COMPOSE_FILE" up -d --build

# Caddyfile 은 볼륨 마운트라 compose 가 caddy 컨테이너를 재생성하지 않는다.
# → Caddyfile 변경을 확실히 반영하려면 명시적으로 리로드(무중단), 실패 시 재시작 폴백.
echo "▶ [3/4] Caddy 설정 리로드"
if docker ps --format '{{.Names}}' | grep -q '^hwlee-caddy$'; then
  docker exec hwlee-caddy caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile \
    && echo "   caddy reload OK" \
    || { echo "   reload 실패 → 재시작"; docker restart hwlee-caddy; }
fi

echo "▶ [4/4] 컨테이너 상태"
docker compose -f "$COMPOSE_FILE" ps

echo ""
echo "✅ 배포 완료. 헬스 체크:"
echo "   curl localhost:8080/actuator/health   # ERP"
echo "   curl localhost:8082/actuator/health   # MES"

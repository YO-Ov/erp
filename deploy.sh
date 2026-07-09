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

echo "▶ [2/3] 빌드 + 기동 (인프라 4개 + 앱 2개)"
docker compose -f "$COMPOSE_FILE" up -d --build

echo "▶ [3/3] 컨테이너 상태"
docker compose -f "$COMPOSE_FILE" ps

echo ""
echo "✅ 배포 완료. 헬스 체크:"
echo "   curl localhost:8080/actuator/health   # ERP"
echo "   curl localhost:8082/actuator/health   # MES"

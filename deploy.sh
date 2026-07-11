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

# compose up 은 caddy 컨테이너를 재생성하지 않으므로(Caddyfile 은 볼륨 마운트),
# Caddyfile 변경·앱 재생성을 반영하려면 caddy 를 명시적으로 다뤄야 한다.
# ⚠️ 'caddy reload' 는 앱(erp/mes)이 --build 로 재생성되어 IP 가 바뀐 경우 새 upstream 을
#    부분만 반영해 빈 응답(content-length:0)이 나는 사례가 있었다(2026-07-11 mes/erp 둘 다 겪음).
#    → 확실한 반영을 위해 restart(순간 다운타임 1~2초, 학습용 서비스라 허용).
echo "▶ [3/4] Caddy 재시작 (새 upstream IP + Caddyfile 변경 반영)"
if docker ps --format '{{.Names}}' | grep -q '^hwlee-caddy$'; then
  docker restart hwlee-caddy >/dev/null && echo "   caddy restart OK"
fi

echo "▶ [4/4] 컨테이너 상태"
docker compose -f "$COMPOSE_FILE" ps

echo ""
echo "✅ 배포 완료. 헬스 체크:"
echo "   curl localhost:8080/actuator/health   # ERP"
echo "   curl localhost:8082/actuator/health   # MES"

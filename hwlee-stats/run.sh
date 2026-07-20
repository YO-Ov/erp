#!/bin/sh
# ─────────────────────────────────────────────────────────────
# 방문 통계 리포트 생성 루프
#   Caddy 액세스 로그(JSON) → jq → Combined 형식 → GoAccess → 정적 HTML
#   결과물을 /srv/stats/index.html 로 떨어뜨리면
#   메인 Caddy 가 hyunwoo.pro/stats/ 에서 (BasicAuth 뒤에서) 서빙한다.
#
#   실시간 웹소켓 모드를 쓰지 않고 주기적으로 정적 HTML 을 굽는 방식이다.
#   → 추가 포트·프로토콜이 필요 없고, Caddy 가 그냥 파일로 서빙하면 끝.
# ─────────────────────────────────────────────────────────────
set -eu

LOG_DIR="${STATS_LOG_DIR:-/var/log/caddy}"
OUT_DIR="${STATS_OUT_DIR:-/srv/stats}"
INTERVAL="${STATS_INTERVAL:-300}"   # 초. 기본 5분마다 갱신

mkdir -p "$OUT_DIR"

# 첫 생성 전에 /stats 로 들어와도 404 가 아니라 안내를 보도록.
if [ ! -f "$OUT_DIR/index.html" ]; then
  cat > "$OUT_DIR/index.html" <<'PLACEHOLDER'
<!doctype html><meta charset="utf-8"><title>방문 통계 준비 중</title>
<body style="font-family:system-ui;max-width:34rem;margin:15vh auto;padding:0 1.5rem;line-height:1.7">
<h1 style="font-size:1.3rem">방문 통계를 준비하고 있습니다</h1>
<p>아직 집계된 방문 기록이 없습니다. 로그가 쌓이면 최대 5분 안에 이 페이지가 리포트로 바뀝니다.</p>
<p style="color:#64748b;font-size:.9rem">기록은 이 기능을 켠 시점부터 쌓입니다. 그 이전 방문은 Cloudflare 대시보드에서 확인하세요.</p>
</body>
PLACEHOLDER
fi

echo "[stats] 시작 — 로그:$LOG_DIR 출력:$OUT_DIR 주기:${INTERVAL}s"

while :; do
  # Caddy 가 롤링하면 .log.gz 도 생기지만, 여기서는 현재 로그만 집계한다.
  if ls "$LOG_DIR"/hyunwoo.pro*.log >/dev/null 2>&1; then

    # 깨진 줄(로테이션 중 잘린 JSON 등)은 조용히 버린다 — 전체 실패로 번지지 않게.
    cat "$LOG_DIR"/hyunwoo.pro*.log \
      | jq -r -f /app/caddy-to-clf.jq 2>/dev/null > /tmp/clf.log || true

    if [ -s /tmp/clf.log ]; then
      # 임시 파일에 굽고 mv 로 교체 → 생성 도중 반쪽짜리 HTML 이 노출되지 않게.
      # ⚠️ 임시 파일도 반드시 .html 로 끝나야 한다.
      #    GoAccess 는 출력 형식을 확장자로 판단해서 .tmp 면 그냥 죽는다.
      if goaccess /tmp/clf.log \
           -o "$OUT_DIR/index.tmp.html" \
           --log-format=COMBINED \
           --tz=Asia/Seoul \
           --html-report-title="hyunwoo.pro 방문 통계" \
           --ignore-crawlers \
           --no-progress >/dev/null 2>&1
      then
        mv "$OUT_DIR/index.tmp.html" "$OUT_DIR/index.html"
        echo "[stats] 갱신 완료 — $(wc -l < /tmp/clf.log) 건 집계"
      else
        rm -f "$OUT_DIR/index.tmp.html"
        echo "[stats] 리포트 생성 실패 — 이전 리포트 유지"
      fi
    fi
  else
    echo "[stats] 아직 로그 파일 없음 — 대기"
  fi

  sleep "$INTERVAL"
done

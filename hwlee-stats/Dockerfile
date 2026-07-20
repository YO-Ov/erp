# ─────────────────────────────────────────────────────────────
# 방문 통계 집계 컨테이너
#   goaccess(집계) + jq(Caddy JSON → Combined 변환)만 있으면 되므로 alpine 그대로.
#   외부 포트 없음. 결과 HTML 을 공유 볼륨(/srv/stats)에 떨구는 역할만 한다.
# ─────────────────────────────────────────────────────────────
FROM alpine:3.20

RUN apk add --no-cache goaccess jq tzdata

WORKDIR /app
COPY caddy-to-clf.jq /app/caddy-to-clf.jq
COPY run.sh          /app/run.sh
RUN chmod +x /app/run.sh

CMD ["/app/run.sh"]

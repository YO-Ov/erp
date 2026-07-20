# ─────────────────────────────────────────────────────────────
# Caddy JSON 액세스 로그 → 표준 Combined Log Format 변환
#
#   GoAccess 의 JSON 파서는 Caddy 의 중첩 구조(request.headers 등)와
#   궁합이 나빠 필드가 어긋난다. 그래서 GoAccess 가 가장 잘 다루는
#   Combined 형식으로 한 번 변환해서 넘긴다.
#
#   ⚠️ client_ip 를 쓴다(remote_ip 아님).
#      Cloudflare 를 거치므로 remote_ip 는 전부 Cloudflare 서버 IP 다.
#      실제 방문자 IP 는 Cf-Connecting-Ip 헤더 → Caddy 가 client_ip 로 채워준다.
#      (caddy/Caddyfile 의 trusted_proxies + client_ip_headers 설정이 전제)
#
#   ts 는 epoch(초, 소수). +32400 = KST(UTC+9) 로 옮긴 뒤 +0900 으로 표기한다.
# ─────────────────────────────────────────────────────────────
select(.request != null)
| [ (.request.client_ip // .request.remote_ip // "-"),
    "-",
    "-",
    "[" + (.ts + 32400 | strftime("%d/%b/%Y:%H:%M:%S +0900")) + "]",
    "\"" + (.request.method // "-") + " " + (.request.uri // "-") + " " + (.request.proto // "-") + "\"",
    (.status // 0 | tostring),
    (.size // 0 | tostring),
    "\"" + ((.request.headers.Referer // ["-"])[0]) + "\"",
    "\"" + ((.request.headers["User-Agent"] // ["-"])[0]) + "\""
  ]
| join(" ")

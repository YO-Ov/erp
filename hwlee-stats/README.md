# 방문 통계 (hyunwoo.pro/stats)

포트폴리오 사이트의 방문 기록을 집계해 **비밀번호로 잠긴 페이지**로 보여준다.

## 어떻게 도는가

```
방문자 → Cloudflare → Caddy ──[액세스 로그 JSON]──> caddy-logs 볼륨
                       │                                  │
                       │                          stats 컨테이너 (5분마다)
                       │                          jq → Combined 형식 → GoAccess
                       │                                  │
                       └──[BasicAuth]── /stats/ ←── stats-html 볼륨 (index.html)
```

정적 HTML 사이트라도 방문 집계가 되는 이유: 기록을 남기는 주체는 HTML이 아니라
요청을 받는 **Caddy**다. Caddy는 액세스 로그가 기본 꺼져 있어서 켜주기만 하면 된다.

## 서버에서 해야 할 설정 (최초 1회)

```bash
# 1) 비밀번호 해시 생성 (평문 아님!)
docker run --rm caddy:2 caddy hash-password --plaintext '원하는비번'

# 2) .env 에 반영 — ⚠️ 해시의 $ 를 전부 $$ 로 바꿔서 넣는다
#    docker compose 가 .env 값의 $ 를 변수로 해석해 해시를 잘라먹기 때문.
#    안 바꾸면 비번이 맞아도 계속 401 이 난다.
#      STATS_USER=admin
#      STATS_PASSWORD_HASH=$$2a$$14$$....
#    변환:  echo '<해시>' | sed 's/\$/$$/g'

# 3) 배포
docker compose -f docker-compose.prod.yml up -d --build stats caddy
```

`https://hyunwoo.pro/stats` 접속 → ID/비번 입력 → 리포트.

## 알아둘 것

- **`.env` 를 안 채워도 사이트는 안 죽는다.** `STATS_PASSWORD_HASH` 기본값이
  폐기된 랜덤 문자열의 해시라, 미설정 시 `/stats` 만 아무도 못 여는 상태가 된다.
  (기본값을 빈 문자열로 두면 `basic_auth` 가 죽으면서 Caddy 전체가 기동 실패 →
  erp·mes·포트폴리오까지 동반 다운된다. 그래서 유효한 해시를 기본값으로 박아둠.)
- **과거 데이터는 없다.** 로그를 이제 켜는 것이라 켠 시점부터 쌓인다.
  그 이전 방문 기록은 Cloudflare 대시보드에만 있다.
- 첫 리포트는 최대 5분 뒤에 나온다. 그 전엔 "준비 중" 안내 페이지가 뜬다.
- 봇/크롤러는 `--ignore-crawlers` 로 걸러진다. 그래도 완벽하진 않아
  Cloudflare 숫자와 정확히 일치하지는 않는다.
- 내가 `/stats` 를 열어본 것은 집계에서 제외된다 (`log_skip /stats*`).
- 로그는 20MB 단위로 롤링되며 5개까지 보관(`roll_size`/`roll_keep`).
  롤링되어 `.gz` 로 넘어간 과거 로그는 집계에 포함되지 않는다.
- **국가별 통계는 안 나온다.** GeoIP DB(mmdb)가 없기 때문. 필요하면
  MaxMind 무료 계정으로 GeoLite2-City.mmdb 를 받아 컨테이너에 넣고
  `run.sh` 의 goaccess 호출에 `--geoip-database=` 를 추가하면 된다.

## 주의: Cloudflare 뒤에서의 방문자 IP

모든 트래픽이 Cloudflare를 거치므로 Caddy가 보는 접속 IP는 전부 Cloudflare 서버 IP다.
그대로 집계하면 **방문자 수가 완전히 엉터리가 된다.**

그래서 `caddy/Caddyfile` 전역 설정에서 Cloudflare 대역만 신뢰하도록 하고
(`trusted_proxies`), `Cf-Connecting-Ip` 헤더에서 실제 IP를 꺼낸다
(`client_ip_headers`). `caddy-to-clf.jq` 가 `remote_ip` 가 아니라
**`client_ip`** 를 쓰는 이유가 이것이다.

Cloudflare IP 대역은 바뀔 수 있다 → https://www.cloudflare.com/ips/

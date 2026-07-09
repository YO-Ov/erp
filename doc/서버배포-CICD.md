# 서버 배포 실행 & CI/CD — Oracle Cloud Always Free

> **이 글의 목적**: [`서버구성.md`](서버구성.md)가 "어디에·왜 올릴지"의 **검토 기록**이라면, 이 문서는
> 실제로 생성한 인스턴스 위에 **ERP·MES·Docker 인프라를 올리는 실행 절차**와 **CI/CD 도구 선택**을 정리한 기록이다.
> 인프라/네트워킹/도메인(Cloudflare)의 배경 설명은 [`서버구성.md`](서버구성.md) 참조.
>
> **빠른 길잡이** — 목적별로 이 절부터:
> - 서버에 **SSH 접속** → §0.5
> - 서버 **최초 세팅**(Docker 등) → §2
> - **앱까지 실제 배포**(따라 치는 순서) → **§3.4 runbook**
> - **CI/CD 자동화** 방향 → §4

---

## 0. 확정된 인스턴스 (재생성 불필요)

| 항목 | 값 |
|---|---|
| 이름 | `hyunwoo-server` |
| Shape | **VM.Standard.A1.Flex** (ARM Ampere A1, aarch64) |
| CPU / RAM | **2 OCPU / 12GB** |
| Public IP | `168.107.50.105` |
| Private IP | `10.0.0.151` |
| 위치 | AD-1 / FD-3 |
| 상태 | 정지됨 → **콘솔에서 Start** 만 누르면 됨 |

- 원하던 **ARM Ampere A1** 이 맞음. `서버구성.md`는 4 OCPU/24GB(무료 상한)를 가정했으나, 실제로는 **2 OCPU/12GB로 생성** — 이 스택엔 충분.
- **예상 실사용 ~4~5GB / 12GB** → 여유 있음.

### 이 스택의 메모리 개산

| 컨테이너 | RAM(개산) |
|---|---|
| MySQL ×2 (erp/mes) | ~0.8GB |
| Kafka (KRaft) | ~1GB |
| Zipkin | ~0.4GB |
| ERP (Spring Boot) | ~0.7~1GB |
| MES (Spring Boot) | ~0.7~1GB |
| **합계** | **~4~5GB / 12GB** |

> ⚠️ **유휴 회수 주의**: Always Free A1은 장기 유휴 시 리소스가 회수될 수 있다. 실습 중엔 되도록 켜두고,
> "start 실패 / out of capacity"는 설정 문제가 아니라 용량 이슈이니 시간을 두고 재시도. 100% 안전화는 계정을 **Pay As You Go로 업그레이드**(무료 범위 내 0원). — 상세: `서버구성.md` §2

---

## 0.5 SSH 접속 방법

서버에 붙는 첫 관문. 접속에 필요한 값은 **3가지**뿐이다: **① Public IP, ② OS 계정명, ③ 개인키 파일**.

### 접속 명령

```bash
# 형식:  ssh -i <개인키경로> <계정>@<Public IP>
ssh -i ~/.ssh/oracle-hyunwoo.key ubuntu@168.107.50.105
```

- `-i <개인키>`: 인스턴스 **생성 시 다운로드한 private key** 파일 경로.
- `<계정>`: 이미지 종류로 **고정**됨 — Ubuntu 이미지면 **`ubuntu`**, Oracle Linux면 **`opc`**. (이 문서 §2는 Ubuntu 기준 → `ubuntu`)
- `<Public IP>`: §0의 `168.107.50.105`. 단, **정지→재시작 시 바뀔 수 있으니**(고정 IP가 아니면) 접속 전 콘솔에서 재확인.

### 개인키 권한 (첫 접속 시 필수)

키 파일 권한이 느슨하면 `UNPROTECTED PRIVATE KEY FILE!` 에러로 접속이 거부된다:

```bash
chmod 600 ~/.ssh/oracle-hyunwoo.key
```

### 매번 -i 치기 귀찮으면 (선택) — `~/.ssh/config`

```
Host hyunwoo
    HostName 168.107.50.105
    User ubuntu
    IdentityFile ~/.ssh/oracle-hyunwoo.key
```

→ 이후 **`ssh hyunwoo`** 한 줄로 접속.

### ⚠️ 접속이 안 될 때 체크리스트

1. **인스턴스가 Start 상태인가** (정지돼 있으면 콘솔에서 Start)
2. **VCN Security List에 22번 Ingress가 열려 있나** (기본은 열려 있음 — §2.1 방화벽 2겹 참고)
3. **키/계정/IP가 맞나** — 계정명 틀리면 `Permission denied (publickey)` 로 떨어짐
4. **키 권한 600인가** (위 chmod)

---

### 📌 내가 직접 확인해야 할 값 — 어디서 보나

| 값 | 어디서 확인 | 현재 알려진 값 |
|---|---|---|
| **Public IP** | OCI 콘솔 → Compute → Instances → `hyunwoo-server` → **Instance details** 의 *Public IP address* | `168.107.50.105` (재시작 시 변동 가능) |
| **OS 계정명** | 인스턴스 상세의 **Image** 항목 확인 → Ubuntu면 `ubuntu`, Oracle Linux면 `opc` | (Ubuntu 기준이면) `ubuntu` |
| **개인키 파일** | 인스턴스 **생성할 때 "Download private key"로 받은 `.key`/`.pem`** (보통 `~/Downloads`) | 로컬 파일 확인 필요 |

> **키를 분실한 경우**: 콘솔에서 기존 키 재발급은 불가. 새 SSH 키쌍을 만들어 **콘솔의 인스턴스 콘솔 연결(Cloud Shell/시리얼 콘솔) 또는 메타데이터**를 통해 공개키를 주입해야 한다. (필요 시 별도 안내)

---

## 1. 현재 로컬 스택 (배포 대상)

| 구성 | 실행 방식(현재) | 포트 |
|---|---|---|
| **ERP** (Spring Boot, Gradle KTS) | `./gradlew bootRun` (호스트 직접) | 8080 |
| **MES** (Spring Boot, Gradle KTS) | `./gradlew bootRun` (호스트 직접) | 8082 |
| MySQL ×2 | docker-compose | 3307 / 3308 |
| Kafka (KRaft) | docker-compose | 9092 |
| Zipkin | docker-compose | 9411 |

- 현재 **인프라만 Docker**, 앱 2개는 호스트에서 gradle로 실행.
- 이미 **`aws` 프로파일**(외부 DB 접속)이 존재 → 서버 배포용 프로파일의 출발점으로 재활용 가능.
- GitHub 저장소: `YO-Ov/erp`.

---

## 2. 서버 기본 세팅 (1회)

OS는 Ubuntu 22.04 기준(Oracle Linux도 가능):

```bash
sudo apt update && sudo apt upgrade -y
sudo timedatectl set-timezone Asia/Seoul

# Docker + compose 플러그인 (공식 스크립트)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # 로그아웃→재접속 시 sudo 없이 docker
```

- 12GB이므로 swap 불필요.

### ⚠️ Oracle 방화벽 = 2겹 (대표 함정)

접속이 안 되는 대표 원인. **둘 다** 열어야 함:

1. **VCN Security List / NSG** (클라우드 콘솔) — Ingress 규칙 추가
2. **인스턴스 내부 iptables** — Oracle 이미지는 OS 안에도 22 외 전부 차단하는 규칙이 있음. 여기서 다들 막힘.

→ 권장: **외부엔 80/443만 노출**. 8080·8082·3307·9092 등은 열지 말고 리버스 프록시 뒤에 둔다. (상세·Cloudflare 구성: `서버구성.md` §4~5)

---

## 3. 앱도 컨테이너로 — 배포 실행 로드맵

서버에선 앱까지 docker-compose에 넣어 **한 번에 기동**하는 게 재현성·관리 면에서 유리.

### 3.1 만들 것 → ✅ 작성 완료

| 산출물 | 경로 | 역할 |
|---|---|---|
| ERP Dockerfile | `hwlee-erp/Dockerfile` | 멀티스테이지(JDK 빌드→JRE 실행). 멀티아치 → ARM에서 그대로 빌드 |
| MES Dockerfile | `hwlee-mes/Dockerfile` | 위와 동일, 포트 8082 |
| ERP prod 프로파일 | `hwlee-erp/src/main/resources/application-prod.yml` | DB `hwlee-erp-mysql:3306`, Kafka `kafka:29092`, Zipkin/MES 도 서비스명 |
| MES prod 프로파일 | `hwlee-mes/src/main/resources/application-prod.yml` | DB `hwlee-mes-mysql:3306`, Kafka `kafka:29092` |
| 통합 compose | `docker-compose.prod.yml` | 인프라 4개 + 앱 2개. 앱은 `127.0.0.1` 로만 바인딩(외부 비공개) |
| 손배포 스크립트 | `deploy.sh` | `git pull && compose up -d --build` + 상태 출력 |
| 민감값 템플릿 | `.env.example` | `JWT_SECRET`, `*_DB_PASSWORD`. 서버에서 `.env` 로 복사해 채움 |

> **왜 Java·MySQL 을 서버에 직접 설치하지 않나**: MySQL·Kafka·Zipkin 은 compose 가 컨테이너로 띄우고,
> Java 는 **Dockerfile 빌드 스테이지 안에서만** 쓰인다. 서버 OS엔 **Docker 만** 있으면 된다(§2에서 설치 완료).

### 3.2 Java/ARM 참고

- Java/Kotlin 산출물(jar)은 **아키텍처 무관**. 베이스 이미지만 멀티아치면 ARM에서 문제 없음.
- 서버(2 OCPU/12GB)에서 직접 빌드 가능 → 교차 컴파일·레지스트리 없이 진행 가능.

### 3.3 진행 순서

```
① 기본 세팅 + Docker (§2)
② 방화벽 2겹 개방 (80/443)
③ Dockerfile + prod 프로파일
④ docker-compose.prod.yml
⑤ deploy.sh 로 "손배포" 성공 → 검증   ← 여기까지가 "서버에서 도는 상태"
⑥ 도메인 구매 후 Cloudflare/Caddy 로 HTTPS
⑦ CI/CD 자동화 (§4)
```

> **원칙**: 수동 배포(⑤)가 먼저 되어야 자동화(⑦)가 된다. CI/CD는 손배포 위에 얹는 자동화일 뿐.

### 3.4 서버 배포 실행 순서 (runbook — 이대로 따라치면 됨)

§2(기본 세팅+Docker)까지 끝난 서버에서, 위 산출물(§3.1)이 커밋된 상태로 진행:

```bash
# ── 서버에 SSH 접속 (§0.5) ──
ssh -i <키> ubuntu@168.107.50.105

# ① 코드 받기 (최초 1회)
git clone https://github.com/YO-Ov/erp.git
cd erp

# ② 민감값 파일 생성 — .env 는 git 에 없으므로 서버에서 직접 만든다
cp .env.example .env
nano .env          # JWT_SECRET 을 긴 랜덤값으로 교체 (openssl rand -base64 48)

# ③ 배포 — 인프라 4개 + 앱 2개를 한 번에 빌드·기동
./deploy.sh
#   (내부적으로:  git pull && docker compose -f docker-compose.prod.yml up -d --build)

# ④ 검증 — 컨테이너 6개가 Up 인지 + 앱 헬스
docker compose -f docker-compose.prod.yml ps
curl localhost:8080/actuator/health     # ERP  → {"status":"UP"}
curl localhost:8082/actuator/health     # MES  → {"status":"UP"}
```

- **첫 빌드는 느리다**(gradle 의존성 다운로드 + 이미지 빌드, 수 분). 2회차부턴 레이어 캐시로 빨라짐.
- 앱 포트(8080/8082)는 **`127.0.0.1` 로만** 열려 있어 외부에선 안 보인다. 서버 안에서 `curl` 로만 확인.
  외부 공개는 이후 도메인+리버스 프록시(⑥)에서 80/443 로.
- 로그 보기:  `docker compose -f docker-compose.prod.yml logs -f erp`
- 이후 코드 변경 반영:  서버에서 `./deploy.sh` 한 번 더 (git pull → 재빌드 → 재기동).

> ⚠️ **DB 스키마(Flyway)**: 앱 기동 시 Flyway 가 `erp_db`/`mes_db` 에 마이그레이션을 자동 적용한다.
> 컨테이너 MySQL 은 빈 DB로 시작하므로 첫 기동에서 전체 스키마가 생성된다(별도 수동 작업 불필요).

---

## 4. CI/CD 도구 비교 & 추천

**서버 조건**: 단일 ARM 12GB, GitHub 사용 중, 학습 목적.

| 방식 | 동작 | 장점 | 단점 | 서버 RAM |
|---|---|---|---|---|
| **① 배포 스크립트**<br>(`git pull && compose up`) | SSH 접속해 스크립트 실행 | 제일 단순, 지금 당장 됨 | 자동화 아님(수동), CI 없음 | 0 (상주 X) |
| **② GitHub Actions + SSH 배포** ⭐목표 | push → GitHub 러너가 빌드 → SSH로 서버 배포 | **무료**, 서버 상주 X, YAML만 관리, 로그가 GitHub에 남음 | ARM 이미지 빌드 시 교차빌드 필요(또는 서버 빌드), SSH 키 시크릿 관리 | 0 (상주 X) |
| **③ GitHub Actions + Self-hosted Runner** | 서버에 러너 상주 → **서버에서 직접** 빌드·배포 | **ARM 네이티브 빌드**(교차빌드·레지스트리 불필요), 프라이빗도 무료 | 러너 프로세스 상주(~200MB), 러너 보안 관리 | ~200MB |
| **④ Watchtower** | 새 이미지 올라오면 자동 pull·재기동 | 초경량, 설정 거의 없음 | 레지스트리 push 파이프라인 별도 필요(단독 CI 아님) | ~50MB |
| **⑤ Jenkins** | 서버에 Jenkins 상주, 파이프라인 실행 | 업계 표준, 플러그인 방대 | **무겁다(1GB+)**, 유지보수·보안 부담, 이 규모엔 과함 | 1GB+ |
| **⑥ Gitea/Drone·GitLab CI** | 자체 호스팅 CI | GitHub 의존 없이 온프렘 | 서버에 CI 서버까지 상주 → 무거움 | 0.5~2GB |

### 추천 — 단계적으로

**결론: ②(GitHub Actions + SSH 배포)를 목표로, ①에서 시작.**

1. **지금**: ① 배포 스크립트로 "손으로 배포되는 상태"부터 만든다. (Dockerfile·compose·프로파일 검증이 먼저)
2. **다음(CI/CD 학습)**: ② GitHub Actions + SSH 배포로 승격. `main` push → 빌드·테스트 → SSH로 `compose up -d --build`. 서버 상주 프로세스가 없어 12GB를 앱에 온전히 쓰고, 표준 CI/CD 흐름(트리거→빌드→테스트→배포)을 배운다.
   - ARM 빌드 이슈가 걸리면 → ③ self-hosted runner로 전환 시 서버 네이티브 빌드로 가장 매끄러움.
3. **Jenkins는 보류**: "Jenkins 도구 자체 학습"이 목적일 때만. 리소스·유지보수 대비 이 프로젝트엔 GitHub Actions가 합리적. 굳이 배운다면 나중에 **로컬 PC에서 Docker로 잠깐** 실습(서버 상주 X).

---

## 5. 다음 할 일

- [x] 콘솔에서 인스턴스 **Start** → SSH 접속 확인 (§0.5, 접속 성공)
- [x] 기본 세팅 + Docker 설치 (§2)
- [x] **ERP·MES Dockerfile + `prod` 프로파일** 작성 (§3.1)
- [x] **docker-compose.prod.yml** 작성
- [x] **deploy.sh** + `.env.example` 작성
- [x] **서버에서 `./deploy.sh` 손배포 검증** (§3.4) — 컨테이너 6개 기동, ERP/MES `/actuator/health` = UP (MySQL 8.4 연결 정상)
- [x] **GitHub Actions(② SSH 배포)로 자동화** — `.github/workflows/deploy.yml`. main push → 러너가 SSH로 서버 접속 → `deploy.sh`. 첫 자동배포 success 확인
- [x] **도메인 구매** — `hyunwoo.pro` (가비아, 현재 네임서버=가비아 기본)
- [ ] **도메인 → HTTPS 연결** (`서버구성.md` §4~5) ← **다음 차례** (아래 §6 시작점 참고)

---

## 6. 다음 세션 시작점 — HTTPS 공개 (도메인 hyunwoo.pro)

> 여기까지: **서버에서 앱 6개 기동 + CI/CD 자동배포 완료.** 단, 앱은 `127.0.0.1` 바인딩이라
> **아직 외부(브라우저)에선 접속 불가.** 다음 목표 = `https://hyunwoo.pro` 로 외부 공개.

### 결정해야 할 것 (세션 시작 시 먼저)
- **경로 ① Caddy 단독**: 가비아 네임서버 그대로 + 가비아 DNS에 A레코드(→168.107.50.105).
  서버 Caddy가 Let's Encrypt 자동 HTTPS. 실서버 IP 노출됨. **간단.**
- **경로 ② Cloudflare(문서 추천, `서버구성.md` §5)**: 가비아 네임서버를 Cloudflare NS로 변경.
  무료 HTTPS + 실서버 IP 숨김 + DDoS/WAF. **포트폴리오용으로 더 그럴듯.**
  → SSL 모드는 **Full + Cloudflare Origin 인증서** 권장.

### 남은 작업 (경로 확정 후)
1. **DNS 연결** — A 레코드 `hyunwoo.pro → 168.107.50.105`
   - 경로②면: 먼저 Cloudflare에 도메인 추가 → 발급된 NS 2개를 가비아 "타사 네임서버"에 입력
2. **방화벽 2겹 80/443 개방** — VCN Security List Ingress + 서버 iptables (§2의 2겹 함정)
3. **리버스 프록시(Caddy) 컨테이너 추가** — `docker-compose.prod.yml` 에 caddy 서비스,
   `hyunwoo.pro → erp:8080` / (서브도메인 등으로) `mes:8082` 라우팅. Caddyfile 작성.
4. **앱 포트 바인딩 정리** — 8080/8082 는 `127.0.0.1` 유지(직접 노출 X), 외부는 Caddy 통해 443만.
5. 브라우저에서 `https://hyunwoo.pro` 접속 확인.

### 참고 (이번 세션에서 만들어둔 것)
- 서버 접속: `ssh -i ssh-key/ssh-key-2026-07-07.key ubuntu@168.107.50.105` (키 권한 600)
- CI 배포키: `ssh-key/ci_deploy_key` (서버 authorized_keys 등록됨, GitHub Secret `SSH_KEY`)
- GitHub Secrets: `SSH_HOST`/`SSH_USER`/`SSH_KEY` 등록 완료
- 임시로 브라우저 확인만 하려면: SSH 터널 `ssh -i <키> -L 18080:localhost:8080 ubuntu@168.107.50.105` → `http://localhost:18080`

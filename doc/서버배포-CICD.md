# 서버 배포 실행 & CI/CD — Oracle Cloud Always Free

> **이 글의 목적**: [`서버구성.md`](서버구성.md)가 "어디에·왜 올릴지"의 **검토 기록**이라면, 이 문서는
> 실제로 생성한 인스턴스 위에 **ERP·MES·Docker 인프라를 올리는 실행 절차**와 **CI/CD 도구 선택**을 정리한 기록이다.
> 인프라/네트워킹/도메인(Cloudflare)의 배경 설명은 [`서버구성.md`](서버구성.md) 참조.

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

### 3.1 만들 것

1. **ERP·MES에 Dockerfile 추가** (멀티스테이지, 베이스는 멀티아치 `eclipse-temurin:21-jre` 류 → ARM에서 그대로 빌드)
2. **`prod`(=oracle) 프로파일** 추가 — compose 네트워크 내부 주소로 변경:
   - DB: `localhost:3307` → **`hwlee-erp-mysql:3306`** (서비스명:내부포트)
   - Kafka: `localhost:9092` → **`kafka:29092`** (내부 리스너)
3. **`docker-compose.prod.yml`** — 기존 인프라(MySQL×2·Kafka·Zipkin) + **앱 2개(ERP·MES)** 통합
4. **배포 스크립트 `deploy.sh`** — `git pull && docker compose -f docker-compose.prod.yml up -d --build`

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

- [ ] 콘솔에서 인스턴스 **Start** → SSH 접속 확인
- [ ] 기본 세팅 + Docker 설치 (§2)
- [ ] 방화벽 2겹 개방 (Security List + iptables, 80/443)
- [ ] **ERP·MES Dockerfile + `prod` 프로파일** 작성 (§3.1)
- [ ] **docker-compose.prod.yml** 작성
- [ ] **deploy.sh** 로 손배포 검증
- [ ] 도메인 구매 → Cloudflare 연결 + HTTPS (`서버구성.md` §4~5)
- [ ] GitHub Actions(② SSH 배포)로 자동화

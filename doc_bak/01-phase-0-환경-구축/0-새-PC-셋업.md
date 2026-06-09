# 0. 새 PC 셋업 가이드

> 다른 PC에서 이 저장소를 처음 받았거나, 환경이 깨졌을 때 처음부터 다시 따라가는 절차서.
> macOS 기준. Windows/Linux는 각 도구의 공식 설치 페이지를 따라가면 됨.

## 0. 준비물 체크리스트

| 도구 | 용도 | 필수 버전 |
|------|------|----------|
| Git | 저장소 받기 | 2.x |
| Docker Desktop | MySQL 컨테이너 | 최신 안정판 |
| JDK | 애플리케이션 빌드/실행 | **21** (Gradle toolchain 고정) |
| IntelliJ IDEA | IDE | 2024.x 이상 (Ultimate 권장) |
| (옵션) MySQL 클라이언트 | DB 직접 조회 | `mysql` CLI 또는 DBeaver/TablePlus |

---

## 1. Git으로 저장소 받기

```bash
mkdir -p ~/IdeaProjects/my-app
cd ~/IdeaProjects/my-app
git clone <저장소 URL> erp
cd erp
```

> ⚠️ IntelliJ 프로젝트 루트는 `erp/`, 실제 Spring Boot 프로젝트는 `erp/hwlee-erp/`. (구조는 [README.md](../../README.md) 참고)

---

## 2. Docker Desktop 설치

### macOS — Homebrew
```bash
brew install --cask docker
```
설치 후 **Docker Desktop 앱을 한 번 직접 실행** → 라이선스 동의 → 메뉴바에 🐳 아이콘이 떠야 데몬이 살아있는 상태.

### macOS — 수동 다운로드
- https://www.docker.com/products/docker-desktop/ 에서 Apple Silicon / Intel 칩에 맞게 다운로드.

### 설치 확인
```bash
docker --version              # Docker version 25.x.x ...
docker compose version        # Docker Compose version v2.x.x
docker ps                     # 빈 표가 떠야 정상 (에러 X)
```

`Cannot connect to the Docker daemon` 에러가 뜨면 Docker Desktop 앱이 안 켜진 것.

---

## 3. JDK 21 설치

### Homebrew (Temurin / Eclipse Adoptium 권장)
```bash
brew install --cask temurin@21
```

### 설치 확인
```bash
/usr/libexec/java_home -V          # 21 버전이 목록에 보여야 함
java -version                       # openjdk version "21.x.x" 출력
```

> JDK 17·22 등 다른 버전이 같이 깔려 있어도 OK. Gradle이 `build.gradle.kts`의 toolchain 설정으로 21을 자동 선택함.

---

## 4. 인프라(MySQL) 기동

저장소 루트(`erp/`)에서:

```bash
docker compose up -d
```

### 정상 기동 확인
```bash
docker ps
# CONTAINER   IMAGE        STATUS                   PORTS                    NAMES
# xxxxxxxx    mysql:8.0    Up 30s (healthy)         0.0.0.0:3307->3306/tcp   hwlee-erp-mysql
```

- **STATUS에 `(healthy)` 가 떠야 한다.** `(health: starting)`이면 10~20초 더 기다리면 됨.
- 호스트 포트는 **3307** (3306은 맥 로컬 MySQL이 점유 가능성 → 일부러 비킴).

### MySQL 접속 테스트
```bash
mysql -h 127.0.0.1 -P 3307 -uerp -perp erp_db -e "SELECT 1;"
```
`1` 이 출력되면 성공. `mysql` CLI가 없으면:
```bash
brew install mysql-client
echo 'export PATH="/opt/homebrew/opt/mysql-client/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

---

## 5. IntelliJ 셋업

### 5-1. 프로젝트 열기
- IntelliJ → **Open** → `~/IdeaProjects/my-app/erp` 선택.
- 루트는 그냥 디렉토리로 열리고, Spring Boot 프로젝트는 자동 인식되지 않는다.

### 5-2. Gradle 프로젝트 연결
- Project 탭에서 `hwlee-erp/build.gradle.kts` 우클릭 → **Link Gradle Project**.
- 우측에 🐘 **Gradle 툴윈도우**가 생기고 `hwlee-erp` 모듈 + `build`, `bootRun`, `test` 태스크 트리가 보이면 OK.

### 5-3. JDK 지정
- `⌘ ;` (Project Structure) → **Project** 탭 → SDK = `21 (Temurin)` 선택.
- **Modules** 탭에서 `hwlee-erp` 가 Gradle 모듈로 잡혀 있는지 확인.

### 5-4. 코드 인식 확인
- `hwlee-erp/src/main/java/.../*.java` 아무 파일이나 열어서:
  - `@SpringBootApplication`, `@RestController` 등이 빨간 줄 없이 정상 색상이면 ✅
  - import 가 빨갛게 뜨면 ❌ → Gradle 동기화 다시 (Gradle 툴윈도우의 🔄 reload)

---

## 6. 애플리케이션 실행

### CLI
```bash
cd hwlee-erp
./gradlew bootRun
```

### IntelliJ
- `hwlee-erp/src/main/java/.../HwleeErpApplication.java` (또는 `@SpringBootApplication` 클래스) 열고 좌측 ▶ 버튼.
- Run configuration의 **working directory** 가 `hwlee-erp/` 인지 확인 (그래야 `application-local.yml` 이 로드됨).

### 기동 확인
```bash
curl http://localhost:8080/api/health | jq
```
기대 응답:
```json
{ "status": "UP", "db": "OK", "timestamp": "2026-05-12T..." }
```

브라우저에서:
- Swagger UI → http://localhost:8080/swagger-ui.html

---

## 7. 통합 테스트

```bash
cd hwlee-erp
./gradlew test
```

Testcontainers가 **별도의 임시 MySQL 컨테이너**를 띄워서 통과해야 함 (위 4번 docker-compose의 MySQL이 아님). Docker Desktop이 살아 있어야 한다.

---

## 8. 자주 막히는 곳 (FAQ)

### Q1. `docker compose up -d` 가 `port is already allocated` 에러
- 다른 컨테이너가 3307을 잡고 있을 가능성. `docker ps -a` → 충돌 컨테이너 `docker rm -f <name>`.

### Q2. 앱이 `Communications link failure` 로 죽음
- MySQL 컨테이너가 아직 `healthy` 가 아닌 상태에서 앱이 떴음. `docker ps` 로 healthy 확인 후 재시작.

### Q3. Flyway 가 `Validate failed: Migration checksum mismatch` 라고 함
- 마이그레이션 SQL 파일이 옛 버전으로 적용되어 있음. **DB 초기화 필요**:
  ```bash
  docker compose down -v          # -v 가 볼륨까지 지움 (데이터 전부 삭제)
  docker compose up -d
  ```

### Q4. IntelliJ가 Gradle 동기화 중 무한 로딩
- 처음 한 번은 의존성 다운로드 때문에 5~10분 걸릴 수 있음. 진행상황은 하단 상태바.
- 너무 오래 멈춰 있으면 File → Invalidate Caches → Restart.

### Q5. `./gradlew bootRun` 이 JDK 못 찾음
- `~/.gradle/gradle.properties` 또는 `JAVA_HOME` 확인.
- 또는 `./gradlew -Dorg.gradle.java.home=$(/usr/libexec/java_home -v 21) bootRun`.

---

## 9. 셋업 끝났는지 한 줄 체크

```bash
docker ps | grep hwlee-erp-mysql      # healthy ✓
curl -s localhost:8080/api/health     # status UP ✓
cd hwlee-erp && ./gradlew test        # BUILD SUCCESSFUL ✓
```

세 줄 다 통과하면 학습 시작할 준비 끝. → [Phase 0 시연 가이드](04-시연-가이드.md) 로.

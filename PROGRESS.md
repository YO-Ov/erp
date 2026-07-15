# 진행 현황판 (PROGRESS)

> **이 파일의 목적**: 여러 PC에서 ERP 학습을 이어갈 때, "어디까지 했고 다음에 뭘 할지"를 **한 화면**으로 복원하기 위한 단일 진행 현황판.
> 대화 기록은 PC 간 공유되지 않으므로, 진행 상태의 **유일한 진실(source of truth)**은 이 파일 + git 저장소다.

## 운영 규칙 (AI가 지킬 것)

> **⚡ 운영 방식 변경 (2026-06-09) — "구현 먼저, 학습 나중"**
> 사용자(hwlee)가 회사 업무로 스터디 시간이 부족 → 7단계 사이클을 매 Phase마다 다 밟지 않는다.
> - **개발 페이즈**: 각 Phase에서 **단계 5(구현)만 쭉** 진행한다. Phase들을 구현 위주로 빠르게 전진.
> - **설계(단계 3·4)는 AI가 합리적 기본값으로 결정·진행**하고, 되돌리기 어려운 큰 갈림길만 한 줄로 통보(원하면 그때 수정). 사용자가 설계를 직접 고민하지 않게 한다.
> - **학습 파트(단계 1·2 도메인 브리핑·Q&A, 단계 6 코드 워크스루, 단계 7 시연)는 전부 개발 완료 후로 미룬다.** 기능이 일단락되면 그때 몰아서 진행한다. `doc/` 학습 문서도 그때 작성.

1. **"다음 진도 실행" 요청 시** → 저장소 전체를 훑지 말고 **이 파일을 먼저 읽어** 현재 위치를 파악한 뒤, `git log -3` 으로만 가볍게 대조하고 바로 **다음 구현**으로 진행한다. (학습 단계는 건너뛴다.)
2. **하나의 구현이 끝날 때마다** → 아래 "현재 위치"와 "Phase 상태표"를 **갱신한다**. (커밋·푸시는 사용자가 직접 한다. 다른 PC에서 이어가려면 사용자가 작업 후 커밋+푸시해야 최신 상태가 공유된다.)
3. **전체 구현이 끝나면** → 사용자에게 미뤄둔 **학습 페이즈(도메인 브리핑·코드 워크스루·시연)** 진행 여부를 안내하고, 그것까지 끝나면 PROGRESS.md 삭제를 안내한다.

---

## 현재 위치

> ### 🧭 별도 트랙 — 프론트엔드 전환 & 포트폴리오 사이트 (ERP 커리큘럼과 무관, 2026-07-15 신설)
> ERP 커리큘럼(Phase 0~16)과 **독립된 별도 트랙**. 상세 계획·결정·다음 진도는 → **`doc/프론트엔드전환-포트폴리오사이트-계획.md`**.
> - **목표**: ⓐ `hyunwoo.pro`에 프로필+포트폴리오 사이트(ERP·MES·AI 3개 소개) ⓑ **Vue·React 직접 학습** = ERP를 SPA로 점진 전환.
> - **핵심 결정**: 포트폴리오는 ERP/MES/AI 어디에도 종속 안 시키고 **독립 정적 사이트**(서브도메인 분리) + 방문자 분석은 외부 analytics(프레임워크·DB 불필요, YAGNI). 전환은 **통째로 X → 모듈 단위 점진**(REST 39개 기반 활용, 첫 대상 SD 견적 유력).
> - **✅ 프레임워크 확정(2026-07-15)**: **ERP=React / MES=Vue**(A안, 목적=두 프레임워크 경험, 둘 다 초심). **둘 다 Vite 순수 SPA — Next.js/Nuxt 안 씀**(백엔드 Spring REST+JWT가 이미 있어 서버사이드 기능 중복·불필요·인증만 꼬임 + 초심자는 순수 React/Vue에 집중이 유리). 스택: ERP=Vite+React+React Router+TanStack Query+Axios / MES=Vite+Vue+Vue Router+Pinia. 배포는 정적 산출물 Caddy 서빙, 기존 REST API 호출(백엔드 불변). **진행 순서 = MES(Vue) 먼저 → ERP(React) 나중**(쉬운 것→어려운 것, 초심자 부담 완화). 상세 → `doc/프론트엔드전환-포트폴리오사이트-계획.md §3`.
> - **✅ MES Vue SPA 1차 스캐폴딩 완료(2026-07-15)**: `hwlee-mes/frontend/`에 **Vite + Vue3 + Vue Router + Pinia + Axios** 프로젝트 신설. 첫 모듈 = **작업지시(WorkOrder) 목록 + 현장 실행 상세**. 백엔드 무수정(Vite 프록시 `:5173 → :8082`). 구조: `api/`(client·workOrders — REST 1:1 매핑) / `stores/workOrders.js`(Pinia) / `domain/status.js`(enum 한글라벨·상태전이 `allowedActions`) / `views/`(목록=상태요약카드+테이블, 상세=기본정보·실적·BOM·현장실행 액션) / `components/StatusBadge.vue`. 실행 액션 = 시작(설비·작업자 선택)·일시정지·재개·실적등록·완료, 상태전이 규칙(RECEIVED→IN_PROGRESS⇄PAUSED→COMPLETED)을 프론트에서 버튼 노출로 반영. **✅ e2e 검증**: `npm run build` 그린(93모듈) / dev서버+프록시로 `/api/work-orders` 실데이터 3건 수신 / SPA 딥링크 폴백 200 / start 액션 RECEIVED→IN_PROGRESS 실동작 확인. 데모 데이터: id=4(RECEIVED), id=5(IN_PROGRESS) 생성해둠(mes_db docker :3308). ⚠️ `frontend/`는 erp git에 잡히나 `node_modules`·`dist`는 자체 `.gitignore` 제외.
> - **✅ MES Vue 화면 확장 완료(2026-07-15)**: 3개 화면 추가. ⓐ **설비 가동현황 대시보드**(`/equipments`, `EquipmentDashboardView`) — 상태별 요약 + 설비 카드(가동률 바 + 상태변경 버튼 RUNNING/IDLE/DOWN/MAINTENANCE) + **자동갱신 토글**(10초 setInterval, onUnmounted 정리). ⓑ **작업지시 접수 폼**(`/work-orders/new`, `WorkOrderCreateView`) — 헤더 + **동적 BOM 라인 추가/삭제**(v-for + push/splice) + 검증. ⓒ **품질검사 패널**(`QualityInspectionPanel`, 상세화면에 임베드) — 검사이력 테이블 + 신규검사 등록(PASS/FAIL 배지). API 확장: `equipmentApi`(list·changeStatus·utilization)·`workOrderApi.inspect`, `domain/status.js`에 설비/품질 라벨 추가. **🐞 버그 발견·수정**: 접수 요청 DTO 필드명이 `lines`가 아니라 **`components`**(응답은 `lines`) → 폼이 자재 누락하던 것 수정, components로 2건 반영 e2e 확인. **✅ 검증**: `npm run build` 그린(99모듈, 코드분할) + 설비 상태변경/가동률·접수폼(자재 2건)·품질검사(FAIL+사유) 전부 실 API e2e 통과. 현재 작업지시 7건(RECEIVED 3·IN_PROGRESS 1·COMPLETED 3).
> - **✅ 생산 시뮬레이터(백엔드) 완료(2026-07-15)**: "설비 가동 켜면 제품이 만들어지는 것처럼" 보이게 하는 학습용 데모. **신규 `simulation/ProductionSimulator.java`** — `@Scheduled(fixedDelay 2초)`로 **IN_PROGRESS + 배정설비 RUNNING**인 작업지시를 찾아(신규 `WorkOrderRepository.findRunningInProgress()` JPQL) 매 틱 지시수량의 12% 자동 생산, 15% 확률 불량 1개(불량사유 랜덤), 지시수량 도달 시 자동 완료. **기존 `PerformanceService.report()/complete()` 재사용** → 자재 투입·Outbox 이벤트까지 정상. 설정 `mes.simulator.{enabled,tick-ms,rate,defect-probability}`(application.yml, `@ConditionalOnProperty`로 on/off). `@EnableScheduling`은 기존 존재. **프론트 실시간 폴링**: `WorkOrderDetailView`가 IN_PROGRESS일 때만 2.5초마다 조용히 재조회(watch로 상태 감지, onUnmounted 정리) + "● 실시간 생산중" 표시. **✅ e2e 검증**: 신규 WO(수량100) 시작→설비 IDLE이면 생산0 확인→EQ 가동(RUNNING)→12→36→…→100 차오르며 자동완료, 실적 9틱(자재 투입 포함)·불량 확률발생 전부 확인. compileJava·npm build 그린. 브라우저 라이브 데모 준비: **id=9(WO-LIVE-DEMO-001, 200개, EQ-002 배정, 진행중 대기)** — 설비현황에서 EQ-002 '가동' 누르면 상세에서 실시간 차오름.
> - **🔧 가동률 실시간 표시 보정(2026-07-16, hwlee님 관찰)**: "가동 누르면 가동률이 안 오르고 대기 누르면 오른다"는 지적 → **백엔드는 정상, 화면 갱신 타이밍 문제**로 판명. 가동률=가동시간÷전체시간이라 RUNNING 상태로 시간이 흐르는 동안 오르는데, 대시보드가 상태변경 시점에만 조회하고 멈춰 있어 안 보였음(대기 누르는 순간 재조회돼 그동안 누적분이 튀어 보인 것). **실측 확인**: RUNNING 5초→100%, 10초→100%, IDLE 5초 후→66.7%(하락). **수정**: `EquipmentDashboardView`가 **가동중 설비가 하나라도 있으면 2.5초마다 가동률만 재조회**(watch(anyRunning)→startPolling, onUnmounted 정리) + "● 실시간 가동률 갱신중" 표시. 혼란스럽던 10초 수동 토글은 제거. npm build 그린. 라이브 데모 갱신: **id=10(WO-LIVE-DEMO-002, 300개, EQ-102 배정)** — EQ-102는 가동이력 0초라 '가동' 누르면 0→100% 빠르게 차오름.
> - **⚙️ 로컬 실행법**: ⓐ DB `docker compose up -d mes-mysql` ⓑ 백엔드 `cd hwlee-mes && ./gradlew bootRun`(:8082) ⓒ 프론트 `cd hwlee-mes/frontend && npm install && npm run dev` → 브라우저 **http://localhost:5173**.
>
> #### ▶▶ 다음 세션 시작점 (2026-07-16 저장)
> **지금까지**: MES Vue SPA = 화면 4종(작업지시 목록·접수폼·현장실행 상세·설비 가동현황) + 품질검사 패널 + **백엔드 생산 시뮬레이터**(가동 켜면 자동 생산) + 실시간 폴링(생산량·가동률) 까지 완성·검증. **MES Vue 1차 트랙은 기능적으로 일단락.**
> - **⚠️ 먼저 할 일 = 커밋·푸시** (다른 PC로 이어가려면 필수, 아직 전부 미커밋):
>   - 신규: `hwlee-mes/frontend/`(Vue 프로젝트 전체, node_modules는 자체 .gitignore로 제외 확인됨) · `hwlee-mes/src/main/java/com/hwlee/mes/simulation/ProductionSimulator.java`
>   - 수정: `hwlee-mes/src/.../workorder/WorkOrderRepository.java`(findRunningInProgress) · `hwlee-mes/src/main/resources/application.yml`(mes.simulator 설정) · `PROGRESS.md` · `doc/프론트엔드전환-포트폴리오사이트-계획.md`
> - **다음 진도 후보(택1)**:
>   1. **MES 정적 빌드 배포** — `npm run build` 산출물(`dist/`)을 Caddy로 `mes.hyunwoo.pro`에 서빙(운영 REST API 호출). 지금 로컬(:5173 dev)까지만 돼 있음.
>   2. **ERP SPA 착수(React)** — 확정 순서상 MES 다음. Vite+React+React Router+TanStack Query+Axios 골격 → SD 견적(quotation) 모듈부터. ERP는 **JWT 인증**이라 MES와 달리 로그인·토큰 처리 필요(ERP `JwtAuthenticationFilter`/`ACCESS_TOKEN` 쿠키).
>   3. **MES 코드 워크스루(학습)** — 이번에 만든 Vue 구조(반응성·라우터·Pinia·프록시·폴링)와 시뮬레이터를 처음 배우는 관점에서 짚기.
>   4. (병행 가능) 포트폴리오 콘텐츠 초안·정적 뼈대 — 프레임워크 무관, 지금도 가능.
> - **참고 데모 데이터**(mes_db docker, 다음에 초기화해도 무방): 작업지시 다수(COMPLETED/RECEIVED 혼재), 시뮬레이터 켜두면 IN_PROGRESS+가동설비는 자동 생산됨.

> ### 🌐 B 방식 완성 — 운영 웹챗봇 → 이 맥 에이전트 (Cloudflare Tunnel + 비밀키 인증) + 조회 버그 2건 수정 (2026-07-14 세션)
> A 방식(이 맥→운영 조회)에 이어, **운영 사이트 `erp.hyunwoo.pro` 챗봇이 이 맥에서 도는 에이전트를 호출**하도록 완성. 이 맥이 NAT 뒤라 인터넷에서 도달 불가하던 걸 **Cloudflare Tunnel**로 해결.
> - **네트워크 = Cloudflare Named Tunnel**: `cloudflared` 설치(brew) → 로그인(`hyunwoo.pro` zone, Cloudflare 관리 중) → 터널 `erp-agent`(ID `e3355dc5-fab3-429b-aec2-157b506adfe6`) 생성 → DNS `agent.hyunwoo.pro` 연결 → `~/.cloudflared/config.yml`(agent.hyunwoo.pro → `localhost:8000`). 경로: 운영 컨테이너 → Cloudflare → 터널 → 이 맥 :8000.
> - **인증 = 공유 비밀 헤더 `X-Agent-Secret`**: 에이전트가 인터넷에 노출되므로 무단호출 차단. `server.py`가 `AGENT_SECRET` 설정 시 `/chat`에 헤더 없거나 틀리면 **401**. 운영 `AssistantController`가 `X-Agent-Secret`(=`assistant.agent.secret`=서버 env `ASSISTANT_AGENT_SECRET`) 첨부. `application.yml`·`docker-compose.prod.yml`·`.env.example` 반영. **비밀키: `erp-agent/.env`의 `AGENT_SECRET` == 서버 `.env`의 `ASSISTANT_AGENT_SECRET`(동일 값)**. ERP 쪽 코드는 커밋 `9bc56b9 에이전트 설정`으로 푸시됨.
> - **✅ e2e 검증**: 공개 `https://agent.hyunwoo.pro/chat` — 헤더 없음/틀림 → 401, 맞음+사용자 JWT → 200 실데이터. 브라우저 챗봇도 실데이터 응답 확인(운영 배포됨). 에이전트는 **`ERP_TARGET=prod`** 로 구동(브라우저 로그인 사용자의 JWT로 그 사용자 RBAC대로 조회).
> - **🐞 조회 버그 2건 수정(`erp_api.py`, 이 맥 전용 재시작만으로 반영·운영 재배포 불필요)**: ⓐ "고객 20건"이 실은 페이지 크기(size=20)였음 → `totalElements` 반영해 **"고객 총 100건 (최근 20건 표시)"** 로. 고객·수주·결재 공통. (검증: 고객 100건·수주 782건, 100건은 totalElements+실행수+유니크코드 3중 확인) ⓑ **개수 질문("총 고객 수"/"몇 건")은 목록 없이 총계 한 줄만**(`_is_count_question`), "목록 보여줘"는 목록까지.
> - **⚠️ 지금 상태 = 임시 구동(상시화 안 됨!)**: 이 맥의 **에이전트·터널이 세션 백그라운드로만** 떠 있음 → 세션/맥 종료 시 꺼지고 `agent.hyunwoo.pro`는 502(챗봇 죽음). **재기동 방법**: ① 에이전트 `cd erp-agent && ERP_TARGET=prod .venv/bin/uvicorn server:app --port 8000` ② 터널 `cloudflared tunnel run erp-agent`. (Ollama는 brew services 상주)
> - **▶▶ 다음 진도(이번에 못 끝낸 것)**:
>   1. **상시화(durability)** — 터널 `sudo cloudflared service install`(재부팅에도 유지), 에이전트도 launchd/`brew services` 로 상시 실행(`ERP_TARGET=prod`). 안 하면 맥 끄면 운영 챗봇이 죽음.
>   2. **데모 안전화(A·B, hwlee님 "다른 사람이 물으면 값 안 나올까 걱정")** — (A) `chat.html` 예시 칩을 **운영 작동 질문**으로 교체: 지금 5개 중 3개(수주해줘·재고·상신)가 운영에선 "비활성/미연결" 막다른 답 → 고객/수주/손익/결재 조회 칩으로 바꾸고 커밋·배포. (B) 범위 밖 질문 안내문을 "이해 못함" 대신 **"고객·수주·손익·결재 조회 가능"** 가이드로.
>   3. **erp-agent 백업** — `erp-agent/`는 erp git **밖**(이 맥 전용)이라 이번 수정들(`erp_api.py`·`server.py`·`main.py`·`orchestrator.py`·`config.py`·`erp_client.py`·`.env`) **버전관리 안 됨** → 별도 백업 필요.
>   4. (선택) 재고 조회 실 API 매핑 / 쓰기 실연결은 `local` 한정(운영 보호).

> ### 🔌 A 방식 완성 — 이 맥의 에이전트 ↔ 운영 ERP 읽기 실연결 (2026-07-13 세션, 미커밋 · erp-agent 저장소 밖)
> hwlee님 결정 재확인 = **AI 계층(Ollama+에이전트)은 이 맥에만, ERP는 Oracle Cloud(운영), 둘은 HTTP·API 경유**(PROGRESS 상단 배치도). 그동안 "구조·읽기 매핑은 됐지만 CLI에서 운영 인증(JWT) 연결이 빠져" 실제로는 안 돌던 A 방식을 **완성·운영 대상 e2e 검증**.
> - **핵심 = 에이전트가 '그 계정으로' 스스로 로그인**: `erp_api.py`에 **`login(base_url, user, pw)`** 신설(`POST /api/auth/login` → `accessToken`·`roles`). 웹은 브라우저 JWT를 프록시가 실어주지만, CLI/서버가 실 ERP에 직접 붙을 땐 에이전트가 이 함수로 로그인해 토큰 확보 → 이후 모든 조회를 그 토큰으로(ERP RBAC 그대로 적용).
> - **`main.py`(CLI)**: http 모드면 기동 시 `config.ERP_USER/PASSWORD`로 자동 로그인 → 로그인 성공/역할 출력 → 매 `orch.handle(...)`에 `erp_token`·`user_email`·`erp_roles` 전달. 실패 시 명확한 안내 후 종료.
> - **`orchestrator.py`**: `config.ERP_MODE=="http"`면 신규 `_handle_http()`로 분기 — 조회(결재/수주/고객)는 사용자 토큰 `ErpReads`(RBAC), **쓰기(ORDER·SUBMIT_APPROVAL)는 운영 데이터 보호로 비활성**(안내 메시지), 재고는 실 API 미연결 안내. QUERY(손익·자유)는 기존 `_free_query` 토큰 경로 그대로. **mock 모드는 무회귀**(쓰기 Plan 확인게이트 유지).
> - **`config.py`**: `ERP_API_BASE_URL`이 대상 ERP와 동일하게 해석되도록 수정(하드코딩 localhost:8080 버그 → prod면 `erp.hyunwoo.pro`). **`erp_client.py`**: http 모드에선 조회를 JWT 경로가 하므로 `HttpERPClient`의 (운영에 안 맞는) 폼 로그인 생략. **`.env` 신설**(이 맥 전용, gitignore): 대상별 계정 채워 `ERP_TARGET=prod`만으로 동작.
> - **✅ 운영 서버 실 검증**: `ERP_TARGET=prod .venv/bin/python main.py` — admin 자동 로그인 → 고객·수주·손익 **운영 실데이터** 조회(CLI+LLM 전체 흐름) / 쓰기 시도 → 안전 차단. **RBAC 강제 확인**: `PROD_USER=kim@`(SALES)이면 손익 🔒권한없음·수주 OK. 컴파일·mock 무회귀 통과. `erp/실행방법.md` AI 섹션에 운영 조회 사용법 반영.
> - **▶ 다음(선택)**: ⓐ **쓰기 실연결은 `local` 대상 한정**으로 별도 매핑(운영 보호 유지) — `erp_client.py` HttpERPClient TODO/NotImplemented 부분. ⓑ 재고 조회 실 API 매핑. ⓒ 운영 웹사이트 화면 챗봇을 이 맥 에이전트에 물리려면 별도 세팅 필요(운영 컨테이너→이 맥 도달: 공개 URL/터널 + `ERP_AGENT_BASE_URL`).

> ### 📊 역할별 대시보드 개편 — 공통 위젯 + 영업 대시보드 (2026-07-13 세션, 미커밋)
> 기존 대시보드는 재무 전용 KPI 카드 4개(비-재무는 전부 "—") + "준비중" 플레이스홀더라 **대부분 역할에게 빈 화면**이었음. hwlee님 요청으로 역할별로 개편.
> - **1단계 공통 위젯(모든 역할)** — `dashboard.html` 개편: 요약카드 4개(결재 대기/진행중 상신/반려됨/미확인 알림) + **내 결재 대기 목록**(inbox) + **내 상신 현황**(outbox, 반려사유 표시) + **알림 목록** + **바로가기(전 역할)**. 재무 KPI 카드는 `sec:authorize(FINANCE/DIRECTOR/ADMIN)`로 감싸 비-재무엔 안 뜸. 기존 API만(`/api/approvals/inbox·outbox`, `/api/notifications`).
> - **2단계 영업(SD) 대시보드** — 집계 엔드포인트 신설 **`GET /api/sd/dashboard`**(`@PreAuthorize SALES/ADMIN`, 신규 `sd/dashboard/` 패키지: Controller·Service·DTO). 목록 API 클라합산은 페이징 부정확 → **서버 집계**(`SalesOrderRepository.aggregateByStatus`·`countByOrderDateBetween`·`sumAmountByOrderDateBetween`·`findTop5...` + `QuotationRepository.countByStatus`). 위젯: 이번 달 수주액·건수 / 출하대기(CONFIRMED) / 미청구(SHIPPED) / 견적 발송대기(APPROVED) + 최근 수주 5 + 상태별 파이프라인 바. `sec:authorize(SALES,ADMIN)` 블록.
> - **✅ 검증**: `compileJava` 그린. `/api/sd/dashboard` kim(SALES)=200 정상 집계 / lee(FINANCE)=403(RBAC). ⚠️ 시드가 2026-06까지라 **이번달(7월) 수주·진행중 수주는 0**(전부 청구완료49/종료732) — 파이프라인·최근수주는 실데이터 표시. 프론트 순수 템플릿(강력새로고침 필요), 브라우저 육안은 hwlee님 확인.
> - **▶ 다음(선택)**: 같은 패턴으로 구매·생산·재무·인사 역할 대시보드(각 집계 API 1개 + 위젯). 영업 KPI를 '내 실적'으로 스코프(→ 과제① 데이터레벨 보안과 연결).


> ### 🔎 Text-to-SQL 자유 조회(Phase 17) 도입 — "반려 일자 언제?" 실 DB로 답함 (2026-07-12 세션, 미커밋)
> hwlee님이 챗봇에 "결재 반려 일자가 언제야?"를 물었는데 통짜 결재목록만 나옴 → **오분류 아님, "방식 2(뭉툭한 액션+고정응답)"의 구조적 한계**(날짜·금액·집계·필터 같은 세밀한 질문을 정형 액션이 못 담음). hwlee님 선택 = **보류했던 Phase 17(Text-to-SQL) 도입**. 이제 자유 분석성 읽기 질문을 **실 DB 조회**로 답한다.
> - **신설 `erp-agent/sqlquery.py`(`SqlQueryTool`)**: 자연어→SELECT 생성(qwen2.5:7b, num_ctx 8192, 스키마 introspection + 도메인 힌트 + few-shot) → **가드레일** → 읽기전용 실행 → 한글 포맷. **가드레일 4중**: ⓐ SELECT/WITH 로만 시작 ⓑ INSERT/UPDATE/DDL 등 쓰기 키워드 차단 ⓒ 세미콜론 다중문 차단 ⓓ **테이블 화이트리스트 강제**(FROM/JOIN 파싱 → 업무 25개 테이블 외 `app_user`·`role`·`audit_log` 등 민감테이블 접근 거부) + LIMIT 자동주입 + `SET TRANSACTION READ ONLY`. **LLM은 SQL 생성까지만, 검증·실행·포맷은 파이썬**(설계 철학 유지).
> - **라우팅**: `schemas`에 `QUERY` 액션 + extractor 시스템프롬프트에 액션목록·경계규칙("날짜·금액·집계·순위·조건 붙으면 QUERY, 단순 정형은 CHECK_*") + few-shot 2개. `orchestrator.handle(intent, raw_text)`로 원문 전달 → `_free_query`→`sql_tool.answer`. `Orchestrator(erp, sql_tool=)` 주입, server/main이 config로 생성. `config.SQL_DB_URL` 있을 때만 활성(`SQL_QUERY_ENABLED`), 없으면 QUERY는 "비활성" 안내(기존 동작 무회귀). deps: `sqlalchemy`·`pymysql`.
> - **⚠️ 실 DB 사실 확인**: 실행 중 ERP는 **`aws` 프로파일 = AWS RDS(`erp_db`, MySQL8.4, user `erp_app`, 비번=`hwlee-erp/secret.properties`의 `ERP_DB_PASSWORD`)** 에 붙어있음(로컬 docker 3307 아님). Text-to-SQL도 이 RDS를 읽기전용으로 가리킴. **운영 Oracle은 미적용**(외부 미노출 유지) — `SQL_DB_URL` 로컬 전용.
> - **✅ 실 RDS e2e 검증**: "결재 반려 일자가 언제야?"→`SELECT decided_at,return_reason FROM approval_request WHERE status='REJECTED'`→**2026-07-05 16:06:03**(원하던 답!) / "여신한도 top3"→한국전력·서울대·부산시청(실데이터) / "이번달 수주합계"→집계SQL 정상. 7B가 세 질문 다 올바른 SQL 생성. 가드레일: DELETE·UPDATE·다중문·app_user/role/audit_log 조회 **전부 차단** 확인. HTTP `/chat` e2e도 통과(라우팅 QUERY 정상).
> - **⚠️ 한계**: 7B SQL 정확도 100% 아님(틀리면 가드레일이 막고 "조회 실패" 안내). 화이트리스트 25개 테이블 내에서만. FROM/JOIN 정규식 파싱이라 특수 서브쿼리는 보수적으로 거부될 수 있음.
> - **⚠️ 미커밋(erp-agent, 저장소 밖)**: `sqlquery.py`(신설)·`schemas.py`·`extractor.py`·`orchestrator.py`·`server.py`·`main.py`·`config.py`·`.env.example`·`requirements.txt`. 실행 시 `SQL_DB_URL` 필요(`.env` 또는 env). 서버는 nohup으로 SQL 활성 상태 기동해둠(:8000).
> - **🔧 후속 보정(hwlee님 실사용 3연속 지적 → 같은 세션 처리)**: 챗봇이 ⓐ 매출/이익을 `total_revenue=…` raw 컬럼으로 답함 ⓑ "반려된 것만" 물었는데 전부(승인·결재중) 보여줌 ⓒ 그 데이터가 mock 가짜라 후속질문서 "없음" 모순. **공통뿌리 = 읽기가 두 세계(정형=가짜mock+필터불가 / QUERY=실DB)로 쪼개짐**. 조치 3종:
>   1. **읽기 일원화**: extractor를 개편해 **모든 조회성 읽기 질문을 QUERY(실 DB)로** 라우팅(결재·수주·고객·재고 포함). 정형 CHECK_* mock 읽기는 few-shot에서 제거(은퇴, enum·핸들러는 잔존하나 미사용). 쓰기(ORDER·SUBMIT_APPROVAL)만 mock 유지.
>   2. **"내가" 인식**: `AssistantController`가 JWT principal(email)을 프록시 body `erp_user`로 첨부 → server→orchestrator→sqlquery 전달 → SQL 프롬프트에 "현재 사용자=X, 1인칭=created_by=X" 컨텍스트 + few-shot. 검증: kim@=반려견적 1건 정확 표시 / lee@=없음(양방향 필터 정상). (principal.getName()=JWT subject=로그인 email=`created_by` 형식 일치 확인)
>   3. **결과 포맷**: LLM 2차 요약을 **시도했다가 폐기** — 7B가 행이 있는데도 "없습니다"로 **데이터를 왜곡**(정확성 위협). 대신 **결정론적 한글 포맷**(컬럼→한글 라벨 `_LABELS` + 상태/문서종류 코드값→한글 `_VALUES` + 집계컬럼은 SQL이 한글 별칭 AS). 거짓말 없이 읽기 좋음. 재검증: "문서번호=APV-…, 문서종류=견적, 결재일시=…" 정상.
>   - ⚠️ **남은 한계(7B 본질)**: 복잡한 파생지표(영업이익 등)는 SQL이 매 실행 달라져 값 불안정 — 단순 조회·필터·집계는 안정적. 프론트 강력새로고침. `./gradlew compileJava` 그린. `chat_log.jsonl`(server.py, append-only 대화로그)도 추가 — 조회/답변 사후 확인용.
>   4. **반려사유 JOIN + 예쁜 포맷(hwlee님 지적)**: "반려사유=-"로 비어 보임 → 실은 사유가 `approval_request.return_reason`(NULL)이 아니라 **`approval_step.comment`(반려단계)** 에 있었음("메모리 비용 상승으로…", 반려자 안나윤). 도메인힌트+few-shot에 approval_step JOIN 추가 → 사유·반려자 표시. 포맷도 개선: **한 건=세로 카드, 여러 건=번호목록**(`_format` 재작성), comment/approver_name 등 라벨 추가.
>   5. **재무제표는 freeform SQL 금지 → ERP 검증 로직 이식(hwlee님이 오류 목격)**: "올해 1분기 매출·영업이익" → 7B가 없는 컬럼 `tax_amount`를 지어내 SQL 오류 + 애초에 영업이익≠매출−세금(틀린 공식). **매출·영업이익·손익 등 재무제표 지표는 7B가 컬럼·공식을 지어내 위험** → 고정 처리로 전환. ERP `ReportService.incomeStatement`(전표 POSTED 집계, 매출원가 계정 5100, 수익=대변−차변/비용=차변−대변, 영업이익=매출총이익−판관비) **JPQL을 sqlquery.py에 SQL로 그대로 이식** + **기간 파서**(`_parse_period`: 올해/작년/N분기/이번달/N월/연간, 기본=올해누계). orchestrator가 재무 키워드(`FIN_KEYWORDS`) 감지 시 freeform 대신 `income_statement()` 호출. 검증: 1분기 매출 79.95억·영업이익 21.2억(판관비 0=시드에 판관비 전표 없음, ERP와 동일). **이제 재무 수치는 결정론적·정확·안정**(7B 흔들림 제거).
>   6. **권한(RBAC) — "정석"으로 = 에이전트가 '사용자로서' ERP API 호출(hwlee님 보안 질문 → 채택)**: 논점 = "에이전트를 권한 다 있는 사람만? vs 계정권한 내에서만?" → **후자(최소권한)** 가 정답, 인가는 LLM/에이전트가 흉내내지 말고 **ERP가 단일 통제점**에서 강제. 발견된 구멍 = Text-to-SQL·손익이 `erp_app`(전체읽기)로 **DB 직결**이라 역할 무시(예: 영업담당이 손익 열람 가능 = ERP 화면에선 막히는데 챗봇으론 샘). **1차 조치(손익 경로)**: ⓐ `AssistantController`가 사용자 JWT(ACCESS_TOKEN 쿠키/Bearer)+역할을 에이전트에 전달(`erp_token`/`erp_user`/`erp_roles`, 토큰은 로그 미기록) ⓑ 신규 `erp_api.py`(`ErpApiClient`, Bearer) ⓒ 손익 질문 → DB이식 대신 **`GET /api/reports/income-statement`를 사용자 토큰으로 호출** → ERP `@PreAuthorize(FINANCE/ADMIN)`가 강제 ⓓ 403이면 "권한 없음" 안내. **✅ 실 검증(로그인 토큰)**: kim(SALES)=🔒권한없음 / lee(FINANCE)=손익표시, **브라우저 흐름(kim 쿠키→프록시→에이전트→ERP)도 403 확인**(실행 중 ERP는 devtools로 자동반영). `ERP_API_BASE_URL`(기본 localhost:8080) config 추가. `./gradlew compileJava` 그린.
>   7. **나머지 조회도 API 경유(RBAC)로 매핑 + 자유 조회 게이팅(hwlee님 "다른 조회도 처리")**: `erp_api.py`에 **`ErpReads`**(결재/수주/고객/손익 = 사용자 토큰으로 ERP API 호출) 신설. orchestrator가 질문을 **도메인 분류**(`_DOMAIN_KEYWORDS`: finance/approval/order/customer)해 각 ERP API로 라우팅. 결재=`/api/approvals/outbox`(Principal이라 **본인 것만** 자동 스코프, steps에서 반려사유 추출), 수주=`/api/sales-orders`, 고객=`/api/customers`. **매핑 안 된 질문(자유 Text-to-SQL)은 RBAC 못 하므로 ADMIN만 허용**(비관리자는 "지원 안 함" 안내 = RBAC 우회 차단). server→handle에 `erp_roles` 전달. **✅ 매트릭스 검증**: kim(SALES)=반려결재(사유표시)·수주 OK / 손익 🔒거부 / 자유질문 🔒관리자만. lee(FINANCE)=손익 OK / 수주 🔒거부. admin=자유질문(품목 top3) OK. **이제 챗봇 조회가 ERP 화면과 동일한 권한을 그대로 따름.**
>   - ⚠️ **남은 것**: 재고·발주·급여 등 아직 미매핑 도메인(질문 시 "지원 안 함"). 새 업무영역은 도메인 매핑 1개씩 추가 = 케이스별(엔드포인트 단위). 집계형 질문("수주 합계")은 목록 API라 합계 미제공(리포트 API 매핑 시 해결). CLI(토큰 없음)는 기존 DB직결 동작 유지(로컬 개발용).
>   - **▶▶ 다음 과제(실무화, 문서화됨 = `doc/AI에이전트-실무화-로드맵.md`)**: 실무 대비 남은 3대 과제를 정리·등록. 권장 순서 = **②직무분리(SoD, 본인상신≠본인결재) → ①데이터레벨 보안(본부장은 자기 본부 실적만) → ③상용 코파일럿 아키텍처 정리(감사·PII·인젝션 방어)**. 한 번에 몰지 말고 하나씩 만들고 검증.
>   8. **임원(DIRECTOR) 역할 신설 → 손익 열람 확대(hwlee님 요청)**: "매출·영업이익을 모든 직원이? → 아니오, FINANCE/ADMIN만"이었고, hwlee님이 "임원도 보게" 요청. **정정 발견 = 이 ERP엔 DIRECTOR 역할이 없었음**(역할은 SALES/PURCHASING/PRODUCTION/FINANCE/HR/ADMIN 6개뿐, 컨트롤러의 'DIRECTOR' 문자열은 죽은 조건). `Role`은 JPA 엔티티(데이터기반). **Flyway V72 신설**: `role`에 DIRECTOR('임원') 추가 + **본부(HQ) 소속 본부장에게 부여**(DEPT-SALESHQ 서동현·DEPT-PRODHQ 조인성·DEPT-MGMT 문재현, V36 부서기준 부여 패턴). `ReportController` `@PreAuthorize` → `hasAnyRole('FINANCE','DIRECTOR','ADMIN')`(매출·재고·손익 리포트 전부). **✅ 검증(V72 RDS 적용됨)**: 서동현(SALES+DIRECTOR)=손익 열람 O / 김영업(SALES만)=🔒거부 → 역할 기반 enforcement 정상, 챗봇이 ERP RBAC 그대로 따름. **권한 규칙은 ERP `@PreAuthorize` 한 곳에서만 정하면 화면·챗봇 동시 적용**(단일 통제점 이점 실증). ⚠️ V72는 공유 RDS에 적용됨(다른 PC는 앱 재기동 시 자동 반영).
>   - ⚠️ 회계 로직 이식본(손익 폴백)은 ERP 원본과 동기화 필요. 회계 로직은 ERP 원본과 동기화 필요(계정체계 바뀌면 이식본도 갱신).
> - **▶ 다음(선택)**: ⓐ 화이트리스트/도메인힌트 확장 ⓑ 파생지표(손익) 안정화 = 정형 리포트 쿼리로 고정 ⓒ 챗봇 UI에 생성SQL 접기 표시(투명성) ⓓ 상위모델(Llama3/14B+)로 SQL 정확도 비교.
>
> ### 💬 ERP 웹 챗봇 → 에이전트 실행 = 코드 완성(mock e2e 검증), 라이브 ERP e2e만 남음 (2026-07-12 세션, 미커밋)
> hwlee님이 원한 것 = **"ERP 화면에서 텍스트 입력 → 에이전트가 실행"**(방향: 브라우저 챗봇 → ERP 프록시 → 파이썬 에이전트 → ERP REST API). 기존 에이전트는 CLI(`main.py`)뿐이라 웹에서 못 불렀는데, **두뇌(extractor·orchestrator·erp_client·Plan)는 전혀 안 고치고** 얇은 HTTP 계층 + ERP 프록시 + 챗봇 UI 3조각만 신설해 실현.
> - **③ 에이전트 HTTP 서버(신설 `erp-agent/server.py`)**: FastAPI로 `extract`+`handle`을 감싼 `POST /chat` + `GET /health`. **쓰기 확인(휴먼 인 더 루프)을 stateless 2단계로**: 1차 `{message}`→LLM추출→`{type:"plan",summary,intent}`(미실행) / 2차 `{intent,confirm:true}`→LLM 건너뛰고 `apply()` 실행→`{type:"result"}`. 읽기·UNKNOWN은 `{type:"message"}` 즉시. **`main.py`(CLI)는 그대로 유지**. requirements에 `fastapi`·`uvicorn` 추가(venv 설치 완료). **실행: `cd erp-agent && ERP_TARGET=mock .venv/bin/uvicorn server:app --port 8000`**.
>   - **✅ 검증(mock)**: TestClient로 plan→confirm 2단계·조회·상신·UNKNOWN 정상. **실 uvicorn+실 Ollama(qwen2.5:7b)로 자연어 e2e도 통과** — "신원전자에 노트북 100대 수주해줘"→plan(재고부족→생산지시→CPU발주 미리보기), "모니터 재고?"·"내 결재?"→즉시 조회.
> - **② ERP 프록시 컨트롤러(신설 `hwlee-erp/.../assistant/`)**: `AssistantController`(`POST /api/assistant/chat`, `@PreAuthorize("isAuthenticated()")` 전 부서 공용)가 `RestClient`로 에이전트 `/chat`에 그대로 프록시(요청/응답 JSON 통과). 에이전트 꺼지면 503, 오류면 502를 **`{type:"error",lines}` 모양으로** 내려 프런트가 동일 렌더. `AssistantViewController`(`GET /assistant`). base-url = `assistant.agent.base-url`(기본 `http://localhost:8000`, application.yml).
> - **① 챗봇 UI(신설 `templates/assistant/chat.html`)**: 말풍선 채팅 + 예시 칩 + 자동확장 입력(Enter 전송). plan 응답 시 **[실행]/[취소]** 버튼 → 실행 클릭하면 1차가 받은 `intent`를 `confirm:true`로 재전송(2단계 확정). 인증은 기존 `ACCESS_TOKEN` 쿠키 자동첨부(CSRF disabled라 헤더 불필요). 사이드바 "전자결재" 아래 **"AI 어시스턴트"** 메뉴(전 부서).
> - **부수 확인**: 실 ERP 인증 = **JWT**(세션 폼 아님, CSRF disabled) — 원래 로드맵 "인증방식 확정"의 답. `JwtAuthenticationFilter`는 `Authorization: Bearer` 또는 `ACCESS_TOKEN` 쿠키에서 토큰.
> - **🐞 오분류 수정(hwlee님 실사용 발견)**: 챗봇에서 "고객 정보 줘"가 첫 답(수주 목록)과 똑같이 나옴 → UI 캐시버그 아님, **에이전트에 "고객 조회" 액션이 없어** 7B가 CHECK_ORDER로 억지 분류한 것. 조치 = **`CHECK_CUSTOMER` 액션 신설**(schemas+extractor few-shot+orchestrator `_check_customer`+MockERPClient `list_customers`/Http 스텁, "1건만" 개수제한 `quantity` 반영) + **시스템프롬프트에 지원 액션 목록 명시 + 범위 밖은 UNKNOWN 강제**(직원·전표 등). 재검증: "고객 1건만"→CHECK_CUSTOMER(quantity=1), "직원 명단"→UNKNOWN(정직하게 못 알아들음), "수주"→CHECK_ORDER 유지. ⚠️ 잔여 미세: "거래처"(vendor)도 CHECK_CUSTOMER로 감(전용 CHECK_VENDOR 없음, 후속).
> - **✅ `./gradlew compileJava` 그린**(신규 경고 0, 기존 deprecation 2개만).
> - **⚠️ 남은 것 = 라이브 ERP e2e 미검증**: 로컬 ERP는 **docker MySQL(3307/`erp_db`)** 필요한데 이 세션엔 **Docker 데몬이 꺼져 있어** 앱 기동 불가(3306은 무관한 네이티브 MySQL). → **Docker Desktop 켠 뒤 아래 순서로 육안 확인 필요**:
>   1. Docker 켜고 DB 올리기(레포 `docker-compose.yml` 또는 기존 방식) → 2. `cd hwlee-erp && ./gradlew bootRun`(local 프로파일) → 3. `cd erp-agent && ERP_TARGET=mock .venv/bin/uvicorn server:app --port 8000`(+ Ollama 실행) → 4. 브라우저 `localhost:8080` 로그인 → 사이드바 **AI 어시스턴트** → "신원전자에 노트북 100대 수주해줘" 입력 → plan 뜨면 **[실행]**.
> - **⚠️ 미커밋**: erp 저장소 = `assistant/` 2파일·`chat.html`·`layout.html`·`application.yml`. **`erp-agent/`는 저장소 밖**(`server.py`·`requirements.txt`) — 별도 백업. 프런트 강력새로고침 필요.
> - **▶ 다음(선택)**: ⓐ Docker 켜고 라이브 e2e 육안(위) ⓑ 액션 확장(QUOTE 등) ⓒ **`ERP_TARGET=local`로 에이전트↔실 ERP 실연결**(= 원래 로드맵 HttpERPClient 매핑, JWT 로그인 `/api/auth/login`·엔드포인트 확정) → 그러면 챗봇이 mock 아닌 진짜 데이터로 동작.
>
> ### 🌐 배포 HTTPS 공개 완료 + 🧠 에이전트 다중대상 전환 구조 (2026-07-11 세션)
> **오늘 한 것**:
> - **✅ ERP·MES HTTPS 외부 공개 완료** — **`https://erp.hyunwoo.pro`**(ERP)·**`https://mes.hyunwoo.pro`**(MES), 루트/www→erp 301 리다이렉트. Cloudflare(주황 구름, 실IP 숨김) + Caddy(Origin 인증서 `*.hyunwoo.pro`, 2041 만료) + 방화벽 2겹(OCI Security List + iptables 80/443). 계정 `admin@hyunwoo.com`/`pass1234`. 상세·트러블슈팅 = **`doc/서버배포-CICD.md §6`**.
>   - 겪은 함정(문서·`deploy.sh`에 반영): ⓐ iptables 삽입 위치(REJECT **앞**) ⓑ **Caddy reload 부분 반영**(앱 `--build` 재생성으로 IP 바뀌면 빈 응답 content-length:0 → `deploy.sh` 를 `caddy reload`→**`docker restart hwlee-caddy`** 로 확정) ⓒ 배포 직후 앱 부팅 중 수십 초 502(정상).
>   - **⚠️ 미커밋(erp 저장소)**: `deploy.sh`(restart 개선)·`doc/서버배포-CICD.md`. hwlee님이 커밋+푸시해야 다른 PC 반영.
>   - **남은 선택(Cloudflare 콘솔)**: `Always Use HTTPS` ON(http 80 접속 521 방지), SSL 모드 `Full`→`Full (strict)`.
> - **✅ 에이전트 다중대상 전환 구조** — `erp-agent/config.py` 를 **`ERP_TARGET=mock|local|prod`** 로 확장(local=`localhost:8080`, prod=`erp.hyunwoo.pro`, 대상별 base_url·계정 분리). `main.py`(접속 대상 표시 + 운영 쓰기 경고)·`.env.example`(로컬/운영 분리) 갱신. **실행: `ERP_TARGET=local python main.py` / `ERP_TARGET=prod python main.py`.** 검증: 세 대상 config 로딩 + 오류값 ValueError 정상. **⚠️ `erp-agent/` 는 erp 저장소 밖 별도 폴더 — erp git에 안 잡힘, 이 맥미니에서 별도 백업 필요.**
> - **결정(hwlee님)**: 로컬 개발=로컬/AWS DB, 운영=Oracle 자립 DB(외부 미노출 유지), 에이전트=DB 직결 아닌 **API 경유**. Oracle DB 로컬 직접연결은 **안 하기로**(필요시 SSH 터널이 안전한 방법).
>
> **▶▶ 다음 세션 시작점 = 에이전트 `HttpERPClient` 실 API 매핑** (현재 로그인·조회 골격만 있고 엔드포인트/스키마 대부분 예시(TODO)·일부 `NotImplementedError`. 전환 구조는 위에서 완성됨):
> 1. **인증 방식 확정**(첫 관문) — 실 `hwlee-erp` 가 세션 폼 로그인인지 JWT인지 조사(SecurityConfig / 로그인 컨트롤러). 폼 로그인이면 **CSRF 토큰** 처리가 필요할 수 있음(`HttpERPClient._login` 보강).
> 2. **읽기 조회 실연결** — `list_orders`(SalesOrderController)·`list_my_approvals`(ApprovalController)를 실 경로/응답에 매핑 → **`ERP_TARGET=prod`(운영 서버 상시 가동)로 읽기가 실제 되는지 확인**(읽기라 안전).
> 3. **쓰기 API 매핑** — `create_sales_order`·발주·상신 등. ⚠️ **로컬에서만 테스트**(운영 데이터 보호).
> - 착수 파일: `erp-agent/erp_client.py`(HttpERPClient 108~188행). 조사 대상: `hwlee-erp` 컨트롤러·시큐리티 설정.
>
> ### 🧠 로컬 LLM ERP 에이전트 — 1단계 프로토타입 구현·검증 완료 (2026-07-07 세션, 미커밋)
> hwlee님이 "AI로 ERP 업무 자동화"를 논의 → **행동형 에이전트**(견적/수주/재고→생산→발주까지)로 확장 아이디어. 로컬 7B(Ollama)는 다단계 판단이 불안정하고, 유료 API(Claude 등)는 비용 발생. **결론 = 역할을 쪼개는 "방식 2" 채택**: **AI는 자연어→구조화 JSON 추출만, 비즈니스 흐름 판단은 파이썬 코드가** 결정론적으로. 완전 무료(로컬)이고 실무의 안전한 AI 패턴이며 이 프로젝트의 "도메인 흐름 직접 설계" 목적과 일치.
> - **위치**: 자바 ERP 저장소와 **분리된 별도 폴더 `erp-agent/`**(`/Users/hwlee/IdeaProjects/my-app/erp-agent`, 이 맥미니 전용). ⚠️ **이건 erp 저장소 밖이라 erp git에 안 잡힘** — 별도 관리/백업 필요.
> - **물리 배치**: AI 계층(Ollama+파이썬)은 **이 맥미니에만**, ERP/MES 웹서비스는 **Oracle Cloud**. HTTP 통신. (Oracle Cloud 배포는 병행 트랙, 하단 참조)
> - **아키텍처 3계층**: ① 의도추출기(`extractor.py`, Ollama+LangChain `with_structured_output`, Pydantic 스키마 강제, 실패 시 UNKNOWN 안전복귀) → ② 오케스트레이터(`orchestrator.py`, 순수 파이썬 if/else = "두뇌", 재고→생산→MRP 분기) → ③ ERP 클라이언트(`erp_client.py`, 지금은 Mock, 나중에 requests로 Oracle Cloud 연결). **의도적으로 LangChain Agent(자율루프)는 안 씀** — 판단을 AI에 다시 넘기는 것이라 목적에 반함.
> - **환경**: Ollama(brew 설치, `brew services`로 상시 실행) + `qwen2.5:7b`(4.7GB) + Python 3.14 venv(`langchain-ollama 1.1.0`·`langchain-core 1.4.8`·`pydantic 2.13.4`). **실행**: `cd erp-agent && .venv/bin/python main.py`.
> - **✅ 검증(쓰기)**: `"신원전자 노트북 100대 수주해줘"` → 추출 `{ORDER,노트북,100,신원전자}` → 수주생성→재고부족(100/30)→생산지시(70)→자재부족 CPU→구매발주(50) **전체 분기 정상**. **LLM은 JSON 추출만, 분기는 전부 파이썬**이 판단(설계 의도 달성).
> - **✅ 조회형(읽기) 액션 3종 추가·검증**: `CHECK_APPROVAL`("내가 상신한 결재 어때?"→상신함 상태: 승인/결재중/반려+사유, 상태코드 한글변환) · `CHECK_ORDER`("신원전자 수주 보여줘"→고객필터 or 전체, 상태 한글) · `CHECK_STOCK`. **읽기라 확인 없이 즉시 응답** → hwlee님이 원한 "조회페이지 대신 채팅으로 업무" 첫 실현. 실 ERP의 `/api/approvals/outbox`·`/api/sales-orders`를 Mock으로 흉내(나중에 requests 교체).
> - **✅ 2단계 휴먼 인 더 루프(plan-then-apply) 추가·검증**: 쓰기 액션(ORDER)은 즉시 실행 안 하고 **① 계획 수립(읽기만으로 재고→생산→발주 분기 전부 미리 계산) → ② 미리보기 → ③ y/n → ④ 승인 시에만 실제 쓰기(apply)**. 오케스트레이터에 `Plan(summary, apply)` 도입, `_order`→`_plan_order`로 "읽기 계획 / 쓰기 실행" 분리, `main.py`가 Plan이면 확인 게이트. **검증**: 수주 100대+y→계획대로 생성 / 10대+n→"취소(아무것도 생성 안 됨)" / 읽기→확인없이 즉시. 잘못 알아들어도 n으로 차단 가능 = 쓰기 안전장치 확보. (실무 전자결재·Terraform 패턴)
> - **✅ 결재 상신(SUBMIT_APPROVAL) 액션 추가·검증**: "PAY-2026-0015 상신해줘"→`{SUBMIT_APPROVAL, ref_no}`(스키마에 `ref_no` 필드 추가). 쓰기라 확인게이트 자동 적용. **계획 미리보기에 실 ERP 전결 규정 재현**: 금액별 결재선 자동 계산(<1천만=팀장 / 1천만~5천만=팀장→본부장 / ≥5천만=팀장→본부장→대표→재무합의). **검증**: 300만→팀장, 9천만→4단계, 없는문서/이미상신은 코드가 차단. "채팅으로 결재 올리기" 실현. Mock: `_drafts`+`get_document`/`resolve_approval_line`/`submit_for_approval`(실 ERP `submitForApproval`+`approval_rule` 흉내).
> - **🐞 구현 중 함정 2개(해결)**: ① few-shot JSON 중괄호를 LangChain이 템플릿 변수로 오인 → `KeyError`(전부 UNKNOWN) → **`AIMessage` 메시지 객체로 넣어 해결**. ② Qwen2.5가 한국어를 중국어로 번역(`노트북→笔记本电脑`) → Mock 재고키 불일치 → **시스템프롬프트 "번역금지" + few-shot 한국어 앵커로 해결**.
> - **✅ 환경별 ERP 연결 계층(config-by-env) 추가·검증**: hwlee님 질문 "로컬/클라우드/외부 어디서든 연결" → **"코드는 하나, 대상은 환경변수로"**(기존 Spring `aws` 프로파일+`ERP_DB_PASSWORD`와 동일 철학). 신규 `config.py`(env: `ERP_MODE`=mock|http, `ERP_BASE_URL`, `ERP_USER/PASSWORD`, python-dotenv로 `.env` 로드) + `.env.example` + `.gitignore`. `erp_client.py`에 **`HttpERPClient`(실 ERP HTTP 연결 골격: requests.Session 폼로그인→JSESSIONID, `_get`/`_post` 헬퍼, Mock과 동일 메서드계약)** + **`make_erp_client()` 팩토리**(env로 Mock/Http 자동선택). `main.py`가 팩토리 사용. **`ERP_MODE=mock` 기본이라 기존 동작 무회귀 확인.** ⭐**추천 = Oracle Cloud에 공개 URL 하나 → 외부·에이전트 전부 그 URL, 환경마다 `.env` 한 줄만 다름.** requirements에 requests·python-dotenv 추가.
> - **⏳ 실 연결 남은 일**: ① `HttpERPClient` 메서드의 **엔드포인트/스키마를 실 `hwlee-erp` 컨트롤러에 맞게 확정**(현재 예시/일부 NotImplementedError) ② ERP 로그인 성공판정 실 설정 확인 ③ Oracle Cloud면 **보안리스트+VM iptables 포트개방** ④ 실행 중·접근가능한 ERP 인스턴스 필요. 확정 전까지 `ERP_MODE=mock`으로 개발.
> - **산출물**: 코드 = `erp-agent/`(액션 6종 + Plan 확인게이트 + config/factory/HttpERPClient 골격 + venv), 문서 = **`doc/AI에이전트-로컬LLM-업무자동화-설계.md`**.
> - **▶ 다음 단계**: ① 쓰기 액션 더 확장(QUOTE 견적생성·결재 승인/반려·취소·다품목) ② 실 ERP 연결(`erp_client.py`→Oracle Cloud API+인증) ③ 정확도 보강(Qwen vs Llama3·품목명→코드 매핑) ④ (선택) 구 Phase 17 Text-to-SQL 읽기 접목(자유 조회) ⑤ (선택) 대화 맥락·웹UI. (미세: CHECK_APPROVAL은 아직 정적 mock이라 방금 상신한 건 반영 안 됨 — 실 ERP 연결 시 자연 해소)
> - **미결정**: 실 ERP API 매핑표(엔드포인트·스키마, `hwlee-erp` 컨트롤러서 확정), 인증방식(세션/토큰), 품목명→품목코드 변환, UI(현재 CLI).
> - **🧭 방향 결정(2026-07-07 세션 말, hwlee님 선택)**: ⓐ **배치** = 맥미니에서 **ERP + 에이전트 함께** 구동(로컬 개발은 agent→`localhost:8080`). 외부(집/회사) 접속용은 **Oracle Cloud 공개 URL 하나로 통일**. ⓑ **다음 트랙 = 에이전트 실 연결보다 "Oracle Cloud 배포"를 먼저** 진행하기로. → **에이전트 트랙은 여기서 일시 정지**(액션6종+확인게이트+연결계층까지 완성·검증됨, Mock 모드로 동작). 이후 Oracle Cloud에 ERP 배포 완료되면 그 URL로 `.env` 설정 후 HttpERPClient 엔드포인트 매핑하면 실 연결.
> - **➡ 다음 세션 시작점**: Oracle Cloud 배포 트랙(하단 "🖥 병행 트랙 — Oracle Cloud Always Free 배포" 참조). 착수 시 배포파일(ERP·MES Dockerfile + `docker-compose.prod.yml`) 작성부터. 배포 전 Oracle Cloud 계정 상태(종량제 업그레이드 완료 여부·A1 인스턴스 생성 여부) 먼저 확인 필요. 참고: 로컬 `docker-compose.yml`은 이미 KRaft Kafka + ERP/MES MySQL 2개 + Zipkin 구성(단, 앱 컨테이너는 없음 — prod compose에 ERP/MES 앱 서비스 추가 필요).
>
> ### 🆕 구매발주(PO) ↔ 입고(GR) 연동 = 2차 (발주 대비 입고 집계·RECEIVED 자동전이) 완료 (2026-07-07 세션, 미커밋)
> 직전 커밋(`8b610ed` PO 신설)에서 **의도적으로 2차로 분리**했던 "입고 역참조 연동"을 구현. 이제 발주와 입고가 데이터로 이어지고 **발주수량 / 입고누계 / 미납**을 라인별로 추적한다.
> - **상태 확장**: `PurchaseOrderStatus`에 **`RECEIVED`(입고완료)** 신설 → `DRAFT→CONFIRMED→RECEIVED→CLOSED`. 부분입고 중엔 CONFIRMED 유지, **전 라인 입고누계 ≥ 발주수량이면 자동 CONFIRMED→RECEIVED**(입고 취소로 미달되면 RECEIVED→CONFIRMED 복귀). `PurchaseOrder.syncReceiptStatus(boolean)` + `close()`를 CONFIRMED·RECEIVED 양쪽 허용으로 확장(부분입고 강제종결 가능). status가 varchar(enum name)라 **DB 마이그레이션 불필요**.
> - **GR→PO 참조**: `GoodsReceipt`에 `@ManyToOne PurchaseOrder purchaseOrder` **nullable** FK + `assignPurchaseOrder()`. 무발주 입고(과거 3년치 시드 V60~V63, 긴급 입고)는 그대로 NULL. **Flyway V71**(`goods_receipt.purchase_order_id` nullable 컬럼+FK+인덱스).
> - **집계**: `GoodsReceiptRepository.sumReceivedQuantityByPurchaseOrder(poId)`(POSTED 입고만, 품목별 SUM, projection `ReceivedQtyRow`). `GoodsReceiptService.post/cancel` 끝에서 PO 참조 있으면 `purchaseOrderService.syncReceiptStatus(poId)` 호출(auto-flush로 방금 POSTED/CANCELLED 반영). `PurchaseOrderService.findById`는 이 집계를 `@Context Map`으로 mapper에 넘겨 **라인별 receivedQuantity·openQuantity** 채움(목록/생성/수정은 빈 맵 = N+1 방지). ⚠️ **단순화**: PO 라인에 동일 품목 중복 없음 전제(item별 집계를 라인에 귀속) — javadoc 명시.
> - **입고 생성 검증**: `GoodsReceiptCreateRequest`에 `purchaseOrderId`(nullable) 추가 + **기존 4-arg 보조 생성자 유지**(테스트 14곳+ 무수정). create 시 발주 상태(CONFIRMED/RECEIVED)·거래처 일치 검증(`resolvePurchaseOrder`). `GoodsReceiptResponse`에 purchaseOrderId·number 노출.
> - **화면**: ① PO 상세 라인에 **발주/입고/미납 컬럼**(미납>0 주황, 0이면 초록 완료 체크, DRAFT는 '-') + RECEIVED 배지·"입고 처리" 딥링크 **`/mm/goods-receipts/new?poId=`** + RECEIVED 시 발주종료 버튼. ② 입고 폼이 `?poId=`면 **발주로부터 입고 모드**(거래처·창고·미납 라인 자동 프리필 + 매입단가, 발주 참조 안내 배너, body에 purchaseOrderId). ③ 입고 상세에 발주 링크 행(무발주면 "무발주 입고"). 순수 프론트, 강력새로고침 필요.
> - **검증**: 도메인 `PurchaseOrderTest` **9건 그린**(RECEIVED 전이·복귀·close/cancel 규칙 3건 신규). 통합 신규 `PurchaseOrderReceiptIntegrationTest` **2건 그린**(부분→전량 RECEIVED 전이 + 입고취소 복귀, 라인별 집계 단언) = 집계 쿼리·매퍼·post/cancel 동기화 실 MySQL 검증. `MmScenarioTest` 그린 = V71 적용+무발주 입고 무회귀. `compileJava/compileTestJava` 그린. **전체 `./gradlew test` BUILD SUCCESSFUL = 회귀 0**(입고 사용 통합테스트 SdMm·PartialDelivery·ConcurrentGoodsIssue·FiAccounting 포함).
> - **▶ 남은 후속(선택)**: ① PO 결재 e2e 통합테스트(생성→상신→승인→CONFIRMED) ② 초과입고 방지 옵션(현재는 허용) ③ 실 앱 육안 e2e(발주 확정→발주로부터 입고→RECEIVED) ④ 구매요청(PR) 상위 문서 ⑤ 대결/위임.
> - ⚠️ **미커밋** — 다른 PC에서 이어가려면 hwlee님이 커밋+푸시. ⚠️ 프론트 **강력새로고침**(PO 상세·입고 폼/상세 JS 변경).
>
> ### 🆕 전자결재 3단계 — 구매발주(PO) 문서 신설 + 결재 연동 (2026-07-06 세션, 커밋 `8b610ed`)
> hwlee님이 "다음 진도"로 **구매발주(PO) 신설**을 선택. 그동안 이 ERP는 매입 쪽에 PO 문서가 없어 입고(GoodsReceipt)만 독립 생성됐는데, 직전 세션에 깔아둔 거래처 취급품목(VendorItem)을 게이트로 재사용해 **매입 사이드의 상위 발주 문서**를 신설했다. 입고(MM)·수주(SD)·결재(견적/지급) 패턴을 그대로 본뜸.
> - **상태머신**: `DRAFT →(전자결재 상신·최종 승인)→ CONFIRMED(발주확정) →(입고 완료)→ CLOSED`, `CANCELLED`(DRAFT/CONFIRMED에서). **결재 없이는 확정 불가**(지출 통제) — 견적/지급처럼 승인 콜백(`PurchaseOrderApprovalListener`, BEFORE_COMMIT)이 `confirmByApproval`로 CONFIRMED 전이. `ApprovalDocType.PURCHASE_ORDER`는 이미 enum에 있었고 전결 규정 시드만 없어서 그것만 추가.
> - **채번**: prefix `PO`는 **생산지시(ProductionOrder)가 선점** → 구매발주는 **`PORD`**(`PORD-20260706-001`). `TransactionNumberGenerator`에 `PREFIX_PURCHASE_ORDER`+`nextPurchaseOrderNumber` 추가.
> - **신설 파일**(`mm/purchaseorder/`): `PurchaseOrder`(헤더: vendor·warehouse·orderDate·expectedDate·remark·lines) / `PurchaseOrderLine`(item·quantity·unitPrice·lineTotal) / `PurchaseOrderStatus` / Service · Controller(`/api/purchase-orders`, `@PreAuthorize PURCHASING/ADMIN`) · Mapper · Repository · Specifications · DTO 5종 + `mm/integration/approval/PurchaseOrderApprovalListener`. **라인 검증**은 입고와 동일 게이트 = `vendorItemRepository.existsByVendorIdAndItemIdAndStatus(ACTIVE)`(취급품목 아니면 발주 거부), 매입단가는 VendorItem.supplyPrice에서 폼이 자동채움.
> - **Flyway V70**: `purchase_order`·`purchase_order_line` 테이블 + `approval_rule` PURCHASE_ORDER 시드(<1천만 팀장 / 1천만~5천만 본부장 / ≥5천만 대표+재무합의 — 지급과 유사).
> - **화면**(`templates/mm/purchaseorder/` 신설 3종): `list.html`(발주금액·상태 + **작성중 발주엔 결재상태 배지** `/api/approvals/status` 병기) / `form.html`(입고 폼 재활용 — 거래처 선택 시 취급품목만 드롭다운 + 매입단가 프리필 + expectedDate·remark) / `detail.html`(견적 상세식 결재 연동: 상신 버튼·결재 진행중/반려 배지·재상신, CONFIRMED 시 **입고 처리 딥링크**+종료+취소). `MaterialsViewController`에 라우팅 4개 + 사이드바 MM에 "구매발주"(입고 위) 메뉴.
> - **➕ 결재 모달 미리보기 연동(hwlee님 실사용 발견, 미커밋)**: 결재함에서 구매발주 열면 "문서 미리보기를 표시할 수 없습니다" fallback이 뜨던 문제 = `approval/list.html` `loadPreview` switch에 **PURCHASE_ORDER 케이스가 없었음**. `previewPurchaseOrder()` 신설(거래처·발주일·입고예정·창고·품목라인/매입단가/발주합계, 견적서 미리보기 본뜸) + switch 연결. **부수 개선 = 타 부서 결재자 열람**: `PurchaseOrderController` 조회(GET)를 `SALES,PURCHASING,PRODUCTION,FINANCE,DIRECTOR,ADMIN`으로 넓히고(결재선의 팀장·본부장·재무합의자가 원본 미리보기 가능) 쓰기·상신·종료·취소는 메서드 단위 `PURCHASING/ADMIN`으로 좁힘(WarehouseController "조회 넓게·변경 좁게" 패턴). ⚠️ 견적(SALES만)·지급(FINANCE만)은 여전히 자기부서만 열람 = 동일 한계 잔존(전 문서 일괄 개선은 후속). `compileJava` 그린, 프론트 강력새로고침.
> - **검증**: 도메인 단위 `PurchaseOrderTest` **6건 그린**(확정/라인편집동결/종료/취소/합계). 통합 `MmScenarioTest`(Testcontainers) **BUILD SUCCESSFUL** = V70 테이블+시드가 실 MySQL에 정상 적용·기존 시나리오 무회귀. `compileJava` 그린(신규 deprecation 경고만).
> - ⚠️ **1차 슬라이스 범위**(큰 갈림길 통보): **입고 역참조 연동(발주 대비 입고수량 집계 → RECEIVED 자동전이)은 2차로 분리**. 기존 3년치 시드(V60~V63)·입고 통합테스트 5개를 건드릴 위험이 커서 1차는 PO 문서+결재+CRUD까지만. 현재 PO→입고는 상세의 "입고 처리" 딥링크(입고 폼 이동)로만 이음.
> - **▶ 남은 후속(선택)**: ① PO 결재 e2e 통합테스트 1건(`PaymentApprovalIntegrationTest` 패턴 — 생성→상신→승인→CONFIRMED) ② 입고에 `purchase_order_id` nullable 참조 + 발주 대비 입고 집계(2차) ③ 실 앱 육안 e2e(구매 담당 로그인→발주 작성→상신→결재→확정) ④ 구매요청(PR) 상위 문서.
> - ⚠️ **미커밋** — 다른 PC에서 이어가려면 hwlee님이 커밋+푸시. ⚠️ 프론트 **강력새로고침**(CSS/JS 캐시).
>
> ### 🅐 여신관리 = 개념 워크스루 + 용어 통일 + 모델 개선(입금 시 여신 자동 해제) 완료 (2026-07-06 세션)
> hwlee님과 신용한도 3종 표기 워크스루를 채팅으로 진행하고, 이어서 **여신 계산 모델을 실무형으로 개선**했다. 요약:
> - **개념 정리(워크스루, doc 미작성)**: 여신한도=외상 상한(`Customer.creditLimit`, 여신 상향 결재로만 변경) / 여신잔액=사용 중인 신용 / 가용한도=한도−여신잔액. hwlee님 오해 지점 교정 = "입금하면 자동으로 여신이 풀리는 게 아니라, (기존 모델은) CLOSED 마감 시에만 풀린다"였고, 이를 **아래 모델 개선으로 실무처럼 바꿈**.
> - **① 용어 통일(순수 프론트, 미커밋)**: 수주 화면 첫 항목 `한도`→**여신한도**(`detail.html`128·`form.html`199), 고객 마스터 `신용한도`→**여신한도** 5곳(`customer/list·form·detail.html`, dt 배지 포함). `가용한도`/`한도 초과`는 유지. 여신 상향 요청 화면(`fi/credit`)은 맥락상 명확해 미변경(원하면 후속).
> - **② 여신사용액 산식 재정의 = "입금 시 여신 자동 해제"(백엔드, 미커밋)**: 되돌리기 어려운 갈림길이라 AI가 AR 기반으로 확정. **여신사용액 = ① 미청구 활성수주(CONFIRMED·SHIPPING·SHIPPED) + ② 미수금(고객 ISSUED 인보이스 합 − POSTED 입금 합, 하한 0)**. 입금(POSTED RECEIPT)이 들어오면 ②가 줄어 여신사용액 자동 감소·가용한도 회복. 과거 3년치 CLOSED 거래는 인보이스↔입금 상쇄로 ②에 0 기여(naive 전체입금 차감이 음수로 깨지는 문제 회피). 부수효과로 "CLOSED인데 미입금"이면 미수 잔존 = 개선.
>   - 신규: `sd/order/creditcheck/CreditExposureCalculator`(3 repo 주입, `CreditExposure(orderBacklog, receivable)` record). `CreditLimitChecker`·`SalesOrderService.creditStatus` 둘 다 이걸 공유(단일 산식).
>   - repo 쿼리: `SalesOrderRepository.sumActiveOrderAmountByCustomer`→**`sumUninvoicedActiveOrderAmount`(INVOICING·INVOICED 제외)** / `InvoiceRepository.sumIssuedInvoiceTotalByCustomer` 신규 / `PaymentRepository.sumPostedReceiptAmountByCustomer` 신규. `CreditStatusResponse`에 `orderBacklog`·`receivable` 추가.
>   - UI: `detail.html`·`form.html` 여신잔액 옆에 `(미청구수주 X + 미수금 Y)` 분해 표시(입금으로 미수금 주는 걸 눈으로 확인).
>   - **정합성**: `InvoiceService.create()`가 `invoice.issue()`+`order.recordInvoicing()`을 한 트랜잭션에 묶어 INVOICED=전량 ISSUED 인보이스 존재 → 백로그에서 빠진 금액이 미수금에 정확히 잡힘(여신 안 샘). 단순화=부분청구(INVOICING) 잔량은 잠시 과소집계(javadoc 명시).
>   - **검증**: 단위 `CreditExposureCalculatorTest` 5건 그린(Docker 불필요). ⚠️ **Testcontainers 통합테스트는 이 세션에 Docker 꺼져 있어 미실행** — hwlee님이 Docker 켜고 `./gradlew test`로 회귀 확인 권장(기존 여신 테스트는 CONFIRMED 단계까지만 단언·한도 1억이라 회귀 위험 낮게 분석됨).
> - **▶ 남은 후속(선택)**: 실 앱 e2e(입금→여신잔액 감소 육안), 통합테스트 1건 추가(수주→인보이스→입금→creditStatus 감소), `doc/` 여신관리 워크스루 글, 여신 상향 요청 화면 용어 통일(B).
>
> **(이전) ✅ 2026-07-06 초 — 표기 실무 용어(B안) 1차 교체**: `사용중→여신잔액`, `남은→가용한도` 등(위 ①에서 여신한도까지 확장 완료).
>
> **✅ 2026-07-06 세션 추가 — 거래처 취급품목(구매정보레코드) 신설 + 입고 제약(미커밋)**: hwlee님 "거래처가 취급하는 품목이 정해져 있어야 실무 맞다"(정석형 선택). 신규 마스터 **`VendorItem`**(거래처↔품목 N:M, 유니크(vendor,item), 필드=매입단가·리드타임·상태) = SAP 구매정보레코드. 신설: `master/vendoritem/`(엔티티·Repository·Service·Controller·ViewController·Mapper·DTO 3종) + **Flyway V69**(테이블 + 부품(COMPONENT)↔거래처 결정론적 시드, 부품마다 ≥1 공급처). **입고 제약**: `GoodsReceiptService.addLines`가 `vendorItemRepository.existsByVendorIdAndItemIdAndStatus(ACTIVE)` 검증 → 취급품목 아니면 입고 거부. **입고 폼**(`mm/receipt/form.html`): 거래처 선택 시 `/api/vendor-items?vendorId=`로 그 거래처 취급품목만 드롭다운 + 매입단가 자동채움 + 거래처 변경 시 라인 초기화(편집모드는 저장 거래처의 취급품목 로드). **관리 화면** `master/vendoritem/list.html`(거래처·상태 필터 + 등록 모달 + 삭제) + 사이드바 MM에 "거래처 취급품목" 메뉴(PURCHASING/ADMIN). **테스트 영향**: 입고 만드는 통합테스트 5개(Mm·SdMm·PartialDelivery·ConcurrentGoodsIssue·FiAccounting) setup에 vendor-item 매핑 추가 + Mm에 거부 케이스 테스트 1건 신규(ProductionScenario는 생산완료 입고라 무관). `compileJava`·`compileTestJava` 그린. ⚠️ Testcontainers는 Docker로 회귀 확인 권장, 프론트 강력새로고침. **미구현(선택 후속)**: 구매발주(PO) 문서, 취급품목 수정 UI(현재 등록/삭제만), 편집 화면(1:N 상세).
>
> **✅ 2026-07-06 세션 추가 — 견적 승인과 발송 분리(미커밋)**: hwlee님 "결재 승인=즉시 발송은 실무와 안 맞다, 분리". 견적 상태에 **`APPROVED`(승인됨·발송대기)** 신설. 흐름 `DRAFT →상신→ (결재중) →최종승인→ APPROVED →담당자 발송→ SENT`. 변경: `QuotationStatus` enum에 APPROVED / `Quotation.approve()`(DRAFT→APPROVED) 신설·`send()`는 APPROVED→SENT로 가드 변경 / `QuotationService.approve(id)` 추가 / `QuotationApprovalListener`가 승인 콜백에서 `send()`→`approve()` 호출 / 견적 상세 renderActions에 APPROVED 시 **발송 버튼**(`act('send','발송')`) + 상태배지 `승인됨(발송대기)` / 목록 상태배지·필터 옵션 추가. **부수효과(개선)**: 목록 일괄발송이 이제 APPROVED에만 적용 → 예전 DRAFT 직접발송으로 결재 우회하던 구멍이 막힘. 테스트 `ApprovalScenarioTest.소액_견적_e2e` 수정(승인→APPROVED, 이어 send()→SENT). `compileJava`·`compileTestJava` 그린. DB 마이그레이션 불필요(status varchar에 enum name 저장). ⚠️ 프론트 강력새로고침 + Testcontainers 통합테스트는 Docker로 회귀 확인 권장.
>
> **✅ 2026-07-06 세션 추가 — 결재 모달에 원본 문서 미리보기 임베드(순수 프론트, 미커밋)**: hwlee님 지적 "결재함에서 원본 클릭하면 페이지 이동→결재함 복귀→재선택이라 불편". 실무 전자결재처럼 **결재창 안에서 원본 내용을 바로** 보게 함. `approval/list.html` 상세 모달에 `#dt-preview` 추가 + `loadPreview(r)`가 `r.docType`+`refId`로 원본 API를 불러 읽기전용 카드 렌더(견적=품목라인/합계, 지급=유형·거래처·금액, 전표=차대 라인, 여신=한도 현재→요청). 모달 `modal-dialog-scrollable`(본문 스크롤·처리버튼 고정), 원본 링크는 `target="_blank"`(보조 참조, 컨텍스트 유지). 권한/실패 시 previewFallback(새 탭 링크)로 graceful. JS 문법 그린. ⚠️ 강력새로고침.
>
> **✅ 2026-07-06 세션 추가 — 판매 품목 드롭다운 완제품 한정(미커밋)**: hwlee님 지적 "수주/견적 품목에 부품이 섞여 나온다". 원인=`GET /api/items`에 `item_type` 필터가 없어 판매 폼이 COMPONENT까지 조회(도메인상 판매대상=`item_type=FINISHED`인데 화면이 안 걸렀음. 실제 시드 판매라인엔 부품 0건이라 데이터 오염은 없음). 수정: `ItemSpecifications.itemTypeEquals` + `ItemController` `itemType` 파라미터 추가(백엔드), 수주·견적 폼 `loadItems()`를 `?itemType=FINISHED`로 호출(프론트 각 1줄). `compileJava` 그린. ⚠️ 프론트는 강력새로고침.
>
> **✅ 2026-07-06 세션 추가 — 검색 콤보박스 전역 확장(순수 프론트, 미커밋)**: hwlee님 요청 "데이터 많은 select 전부 검색형으로". 기존 자동향상은 목록 필터바(`form.filter-bar`)만 덮었는데, `erp.js` 스캔 범위를 **화면 내 모든 `select` + 동적 추가 select(라인 아이템 행·모달)**로 확장(DOMContentLoaded 전체 스캔 + `document.body` childList MutationObserver). 옵션 12개 초과만 콤보화(짧은 enum·페이지크기는 네이티브, `data-no-search` opt-out). 이제 **작성 폼(견적·수주 등)의 고객·품목·거래처·직원·계정 select까지 한 번에** 검색형. `enhanceSelect`에 **required 해제** 추가(display:none 된 required select 이 네이티브 검증에서 "not focusable"로 제출을 막는 문제 → 앱/서버 검증에 위임). **템플릿 무수정 — erp.js 1파일만**. ⚠️ 강력새로고침 필요. 라인테이블 셀 안 콤보 메뉴 클리핑은 실렌더 육안 확인 권장.
>
> **🖥 병행 트랙 — Oracle Cloud 배포 = ✅ HTTPS 외부 공개 완료 (2026-07-11)**: hwlee님 개인 Oracle 계정에서 ERP/MES 무료 서비스. **`https://hyunwoo.pro` 실제 접속 200 확인.** 상세 실행기록·트러블슈팅은 **`doc/서버배포-CICD.md`**(§6 최신) 참조.
> - **인프라**: Oracle A1(`hyunwoo-server`, 2 OCPU/12GB, IP `168.107.50.105`, Ubuntu 22.04). Docker compose로 컨테이너 **7개**(MySQL×2·Kafka·Zipkin·ERP·MES·**Caddy**). DB는 VM 안 Docker MySQL 자립(Flyway가 스키마+3년치 시드 생성). 앱은 `127.0.0.1` 바인딩, 외부는 Caddy만 80/443.
> - **CI/CD**: `main` push → GitHub Actions가 SSH로 서버 `deploy.sh`(git pull + `docker compose -f docker-compose.prod.yml up -d --build`). 자동배포 검증됨.
> - **HTTPS**: 도메인 `hyunwoo.pro`(가비아 등록, NS→Cloudflare `boyd`/`june.ns.cloudflare.com`). Cloudflare(주황 구름, 실IP 숨김) → Caddy(Cloudflare Origin 인증서, 2041 만료) → erp:8080. 방화벽 2겹(OCI Security List + iptables 80/443).
> - **⚠️ 남은 마무리(선택, Cloudflare 콘솔)**: ① `Always Use HTTPS` ON(안 켜면 http(80) 접속이 521) ② SSL 모드 `Full`→`Full (strict)`. ③ 문서 변경(§6·PROGRESS) 커밋·푸시.
> - 기존 AMD 서버 prod·dev(E2.1.Micro)는 이 A1과 무관, 건드리지 말 것.
>
> ---
>
> **▶▶ (이전 항목) 모바일 반응형 실렌더 점검·보정**
> 2026-07-05 세션에서 목록 20개를 표→카드 반응형으로 만들었으나(커밋 `a725699`), **실제 폰/좁은 폭 렌더는 육안 확인 안 됨**(이 환경은 헤드리스 없음). 다른 PC 브라우저에서 창을 768px 아래로 줄이거나 폰으로 접속해 **카드가 어색하거나 깨지는 화면을 찾아 개별 보정**한다.
> - 점검 대상: 목록 20개(견적·수주·고객·재고·지급·전표=커스텀 카드 / 나머지 14개=`.rtable` data-label 카드) + 상세·필터바·모달·결재함 타임라인.
> - 흔한 보정 포인트: 카드 안 값이 너무 길어 넘칠 때, 배지 줄바꿈, 금액 정렬, data-label 라벨-값 정렬, 필터바 세로 스택, 상세 dl(정의목록) 2열→1열.
> - ⚠️ CSS 캐시 때문에 확인 시 **강력새로고침(Cmd+Shift+R)**. 공통 규칙은 `static/css/app.css` 하단(`@media max-767`), 개별은 각 `list.html`.
>
> **✅ 2026-07-05 추가 보정(미커밋)** — 폰 실접속에서 발견된 2건 처리:
> - **상단바 압축**: 폰에서 제목("고객")이 세로로 줄바꿈되고 "로그아웃"도 세로로 깨지던 문제 → `@media max-767`에서 **제목 말줄임(ellipsis)·이메일 텍스트 숨김(아이콘만)·로그아웃 아이콘만**. `layout.html`에 `.uc-name`/`.logout-text` span 래핑 추가.
> - **상세 라인 테이블 반응형**: 전 상세 라인 표를 모바일 처리. ① CSS 베이스라인 `.app-content table.table:not(.rtable)` → **가로 스크롤**(총계·정렬 보존). ② 단순 영업 라인 5개(**수주·견적·인보이스·출하·출고 detail**)는 `.rtable`+`data-label`로 **카드 승격**(품목 셀=`rt-wide` 라벨 위·값 아래). 다열/합계 표(전표·급여·입고·재고이력·생산·직원)는 스크롤 유지. 순수 프론트(HTML/CSS), 컴파일 무관.
> - **필터바 넘침**: 폰에서 주문일 날짜범위(고정 146px×2)가 카드 밖으로 넘치던 문제 → `@media max-767`에서 필터 필드 `width:100%` + 날짜 입력 `flex:1;min-width:0`. `.filter-bar`/`.date-range` 공통이라 **20개 목록 필터 전부** 적용.
> - **조회조건 접기 + 검색 콤보박스**(hwlee님 요청): 폰에서 필터가 화면 절반 차지 + 긴 셀렉트(고객·영업담당) 검색 불가 문제. **파일 18개 무수정, `erp.js` 공통 자동향상(progressive enhancement)으로 구현**:
>   - ① **접이식 필터**: `erp.js`가 `form.filter-bar` 앞에 "조회조건" 토글 헤더(`d-md-none`) 주입, 모바일 **기본 닫힘**(`.is-collapsible` max-height 트랜지션). 헤더에 **활성 조건 개수 배지**(닫아도 조건 걸린 것 인지). 펼침 완료 후 `overflow:visible`(콤보 메뉴 안 잘리게, transitionend+타임아웃 폴백).
>   - ② **검색 콤보박스** `ERP.enhanceSelect(select)`: 네이티브 `<select>`를 **display:none로 숨겨 값/폼reset/기존 filters() 그대로 두고** 위에 검색 입력형 드롭다운을 씌움(선택 시 select.value 세팅 + change 디스패치). **옵션 12개 초과 셀렉트만** 자동 향상(짧은 enum=상태 등은 네이티브 유지). 셀렉트가 API로 나중 채워지므로 `MutationObserver`로 임계치 초과 감지 후 향상. 키보드 내비(↑↓/Enter/Esc)·✓선택표시·검색없음 처리. 데스크탑에도 적용(긴 목록 검색이 어디서나 유용).
>   - CSS: `.erp-combo*`(전역) + `.filter-toggle*`·`.is-collapsible`(모바일). 순수 프론트, 컴파일 무관.
> - ⚠️ 실 폰 렌더는 hwlee님 **강력새로고침 후 육안 확인** 권장. (변경 파일: `js/erp.js`, `css/app.css`, `layout.html` — 목록 18개 template은 무수정)

- **🎉 Phase 0~16 전체 구현 완료·검증.** 이후 hwlee님 요청으로 **실무형 기능을 점진적으로 확장 중**(아래 2026-06-10 항목들). 학습 문서(doc/)는 별도 트랙.
- **▶ 진행 중 = "실무 리얼리즘 확장" 프로젝트**(2026-06-27 착수, 포트폴리오 외부 공개용). 바로 아래 항목 참조. **STEP 1(KRaft)·2(카테고리 마스터화)·3(Factory)·4(마스터 대량확장)·5(3년치 실거래 백필) 완료. STEP 6은 테스트 정리 완료·`doc/` 학습문서만 남음.**
- **🆕 전자결재 엔진(전사 횡단) 1~3단계 + 구매 프로세스 확장 진행 중**(2026-07-05~07). 1~2단계=범용 결재 엔진 + 견적·지급·전표 연동 + 여신 완전통합(전체 112개 테스트 그린 + 실 앱 RDS e2e). 3단계=구매발주(PO) 문서 신설 + 결재 연동(커밋 `8b610ed`). **PO↔입고(GR) 2차 연동=발주 대비 입고 집계·RECEIVED 자동전이**(2026-07-07, 위 최상단 🆕 항목, 미커밋).
- **🧠 로컬 LLM ERP 에이전트(방식 2 = AI는 추출·파이썬이 판단) — 1단계 프로토타입 구현·검증 완료**(2026-07-07). 코드=`erp-agent/`(저장소 밖 별도 폴더), 설계=`doc/AI에이전트-로컬LLM-업무자동화-설계.md`. Ollama `qwen2.5:7b`로 수주→생산→발주 분기 정상. 최상단 🧠 항목 참조.
- (보류) Phase 17(자연어 데이터 검색, Text-to-SQL + 가드레일) — STUDY-PLAN Part 3 편입 완료, 구현 미착수. **행동형 에이전트(위 🧠)와는 별개 기술**(읽기 vs 쓰기), 후에 조회 액션에 접목 가능.

### 🗓 2026-07-05 세션 — 전자결재 엔진(전사 횡단) 1단계: 코어 + 견적 연동

> hwlee님 요청 "전자결재를 실무 방식으로". 설계 검토 후 **범용 결재 엔진**을 신설하고 **견적(Quotation)** 에 첫 적용. 실무 4대 요소(전결 규정·결재 유형·반송 루프·조직도 결재선) 모두 반영.

- **확정 스펙(hwlee님 선택)**: ① 결재선=**금액별 전결 규정 자동**(release strategy식) ② 결재 유형=**승인(순차)+합의(병렬)+참조** ③ 반려=**반송(재상신 루프)+반려종결** ④ 결재자 매핑=**조직도 기반(부서장 FK + 본부장 계정 신설)** ⑤ 착수 범위=**엔진 코어 + 견적 1종**.
- **조직 정비(Flyway V64)**: `Department` 에 **부서장(`manager_id`) FK 신설** + **본부장 3명 계정 신설**(영업/생산/경영지원=`sales.dir`·`prod.dir`·`mgmt.dir`@hyunwoo.com, DIRECTOR, 비번 pass1234) + 부서장 지정(팀→팀장 6명, 본부→본부장 3명, 회사 DEPT-HQ→admin=대표 대행). `Department.manager` 엔티티 필드 추가.
- **결재 엔진(`com.hwlee.erp.approval`, Flyway V65)**: 
  - 엔티티 `Approval`(approval_request, 상태머신 DRAFT/PENDING/APPROVED/REJECTED/WITHDRAWN + 순차승인/병렬합의/반송 로직 캡슐화) · `ApprovalStep`(결재선 단계, 유형 APPROVAL/AGREEMENT/REFERENCE, 결재자 스냅샷) · `ApprovalRule`(전결 규정, 금액구간→레벨+재무합의여부). enum 6종.
  - `ApprovalLineResolver`: **상신자 부서→조직 트리 상향, 각 노드 부서장을 결재자로**(자기 자신·미지정 노드 skip → 상위로). 전결 레벨(TEAM/DIVISION/COMPANY)이 확보할 결재자 수 결정. 고액이면 재무팀장 **병렬 합의** 추가.
  - `ApprovalService`(상신/승인/반려/반송/회수/재상신 + 알림 라우팅 + 중복상신 방지) · `ApprovalController`(`/api/approvals` inbox/outbox/처리, `isAuthenticated`) · `ApprovalViewController`(`/approvals`). `NotificationType` 4종 추가, 채번 prefix `APV`.
  - **전결 규정 시드(견적)**: <1천만=팀장전결(TEAM) / 1천만~5천만=본부장까지(DIVISION) / ≥5천만=대표까지(COMPANY)+재무합의.
- **견적 연동**: `QuotationService.submitForApproval`(DRAFT 견적→결재 상신, `QuotationController POST /{id}/submit-approval`). 최종 승인→`ApprovalApprovedEvent`(BEFORE_COMMIT)→`QuotationApprovalListener`가 견적 `send()`(SENT) — 결재→문서 역방향 의존을 이벤트로 끊음.
- **화면**: `approval/list.html`(상신함/결재함 탭 + 결재선 타임라인 + 승인/합의/반려/반송/회수/재상신) · 견적 `detail.html`(직접발송→**결재 상신** 버튼 교체 + 진행상태 배지) · `layout.html`(사이드바 "전자결재" 메뉴 + 알림 아이콘 4종).
- **검증**: 단위 `ApprovalTest`(상태머신 7건: 순차·반려·반송재상신·병렬합의·회수) + 통합 `ApprovalScenarioTest`(Testcontainers 3건: 전결차등·견적 e2e자동발송·중복상신방지). **전체 107개 테스트 그린**(V64/V65 실제 MySQL 적용 확인).
#### ➕ 전자결재 2단계 — 타 문서 연동 + 여신 완전통합 + 실앱 검증 (2026-07-05 같은 세션 이어서, hwlee님 "1·2·3 다 해줘")

- **1번 — 타 문서(지급·전표) 결재 연동**: 구매발주는 이 ERP에 문서 자체가 없어 제외(입고 GoodsReceipt만 존재). **지급(Payment 출금)·수동전표(JournalEntry MANUAL)** 에 결재 게이트 추가.
  - 기존 "생성 즉시 전기(post)" 경로는 **유지**하고(회귀 방지), 결재 경로를 **추가**: `createDraft`/`createManualDraft`(DRAFT 저장) → `submitForApproval`(PAYMENT는 DISBURSEMENT만, JOURNAL은 MANUAL만) → 승인 콜백 리스너(`fi/integration/approval/{Payment,JournalEntry}ApprovalListener`, BEFORE_COMMIT)에서 `post()`(+지급은 출금 자동분개). `JournalEntry.totalDebit()` 추가(전결 금액). 컨트롤러에 `/draft`·`/{id}/submit-approval` 엔드포인트.
  - **Flyway V66**: PAYMENT·JOURNAL 전결 규정 시드(금액 구간별, 재무 문서라 합의 없음).
- **2번 — 여신 완전통합(결재함 일원화)**: hwlee님 선택. 여신 상향을 결재 엔진 문서(`CREDIT_LIMIT`)로 편입. **계획오더는 "시스템 자동 제안→전환" 성격이라 결재와 안 맞아 제외**(통보).
  - 엔진 확장: `ApprovalRule.fixedApproverDeptCode`(고정 결재 부서) — 지정 시 상신자 조직 상향 대신 그 부서장이 결재. 여신=`DEPT-FINANCE`(재무팀장). `ApprovalRejectedEvent` 신설(승인 이벤트와 대칭, 반려 콜백용) + `ApprovalApprovedEvent`에 `decidedBy` 추가. resolver에 고정부서 분기.
  - 여신 개편: `CreditLimitRequestService.create`가 저장 후 **결재 상신**(재무팀장 결재선), 자체 approve/reject 제거 → `applyApproval`/`applyRejection` 콜백(`CreditApprovalListener`가 APPROVED→한도반영, REJECTED→종결). `CreditLimitRequestController` 승인/거부 엔드포인트 제거(결재함으로 이관). 여신 화면 `fi/credit/list.html` 승인/거부 버튼 → "결재 진행 중"(결재함 링크)로 교체 + `?id=` 딥링크. **Flyway V67**(fixed_approver_dept_code 컬럼 + 여신 규칙).
- **검증**: 통합테스트 3건 신규(`CreditApprovalIntegrationTest` 여신 승인/반려, `PaymentApprovalIntegrationTest` 지급 전기) → **전체 110개 테스트 그린**(V64~V67 실 MySQL 적용). **+ 실 앱(localhost:8080, AWS RDS, devtools 핫리로드로 반영) HTTP e2e 검증 성공**: ⓐ 견적 상신→영업본부장(서동현) 승인→견적 SENT ⓑ 여신 요청→재무팀장(우태윤) 승인→고객 한도 100M→130M 반영.
  - ⚠️ **RDS에 검증 데이터 생성됨**: 견적 Q-20260705-001(SENT)·결재 APV-20260705-001(APPROVED), 여신 CLR-20260705-001(APPROVED)·결재 APV-20260705-002(APPROVED), **신원전자(CUST-2026-0001) 한도 1억→1.3억 변경됨**. 결재함 데모용으로 남기거나 원복 가능(hwlee님 판단).
- **➕ 후속 보강(같은 세션)**:
  - **결재 상태 목록 가시성**(hwlee님 지적 "초안만 보이면 상신/반려를 모른다"): 배치 조회 API `GET /api/approvals/status?docType=&refIds=`(refId→최신 상태 맵, N+1 없음) + **견적·지급·전표 목록에 결재 상태 배지**(결재중/결재반려/반송/회수) + 견적/지급/전표 **상세에 반려 사유 안내 + "다시 상신"**. (승인=문서 발송/전기와 중복이라 배지 생략)
  - **지급·전표 화면 결재 경로 완성**: 지급 폼 "결재 상신하여 등록"(출금 전용)·전표 폼 "결재 상신하여 등록"(수동전표) + 상세 진행상태/반려/재상신. 초안 저장(`/draft`)→상신(`/{id}/submit-approval`) 흐름. **실 앱 검증**: 지급 PAY-20260705-001·전표 JE-20260705-001 상신→재무팀장(우태윤) 결재선 자동구성→목록 '결재중' 배지 확인.
  - **로그인 빠른선택 확장**(`login.html`): 부서별 담당·팀장·본부장 계정 전체(sales.mgr/dir·finance.mgr·mgmt.dir·prod.sw/gm/dir 등) optgroup 그룹핑 + 결재 시연 안내. 결재 흐름별 로그인 전환 용이.
  - **참조(REFERENCE) 단계 활용**: `ApprovalRule.referenceDeptCode`(참조 부서, V68) + resolver가 참조 단계 생성(결재자·자기 중복 제외) + 상신 시 참조자에게 `APPROVAL_REFERENCED` 통보(딥링크 `/approvals?id=`) + 결재함 타임라인 "참조·열람 대상" 표시 + `?id=` 딥링크로 문서 열람. **견적 본부장 전결(1천만~5천만)에 대표(DEPT-HQ) 참조** 설정. 단위 2건. **실 앱 검증**: kim 2천만 견적→[팀장·본부장 결재 + 대표 참조], 대표에게 참조 알림 전송. (참조는 완료 판정 무관 — 결재만 끝나면 APPROVED.) **전체 112개 테스트 그린.**
  - **모바일 반응형 — 목록 20개 전부 표→카드 전환**(hwlee님 요청): 공통 CSS 1벌(`app.css` `.lc-*` 커스텀 카드 + `.rtable` data-label 자동변환, `@media max-767`). **핵심 6개는 커스텀 카드**(견적·수주·고객·재고·지급·전표 — 번호+상태/결재 배지 상단, 금액/수량 강조, 메타; 표는 `d-none d-md-block`, 카드는 `list-cards d-md-none`). **나머지 14개는 `.rtable`+`data-label`**(출하·인보이스·계정·여신·직원·급여·알림·출고·입고·창고·BOM·생산지시·계획오더·결재함 — 좁은 폭에서 "라벨: 값" 카드 자동). 순수 프론트(HTML/CSS/JS), 컴파일 무관. ⚠️ CSS 캐시 때문에 브라우저 **강력새로고침(Cmd+Shift+R)** 필요.
- **▶ 다음 단계(전자결재 3단계~)**: 대결/위임(부재 시 대리 결재), 참조함(참조로 걸린 문서 목록), 구매발주 문서 신설 후 연동. 설계문서 `doc/실무리얼리즘확장-전자결재-설계.md`.
- ⚠️ **미커밋** — 다른 PC에서 이어가려면 hwlee님이 커밋+푸시. (1단계 신규 approval 패키지 + V64/V65 + approval/list.html / 2단계 추가: V66/V67 + fi/integration/approval/*Listener 3종 + Payment·Journal·Credit 서비스·컨트롤러 변경 + credit/list.html)

### 🗓 2026-07-04 세션 — 수주 마감(CLOSED) 기능 완성 (백엔드+UI)

> 이전 세션에서 백엔드만 만들다 `/clear` 됨. 이번 세션에 UI 버튼까지 붙여 수직 슬라이스 완결.

- **배경**: 전량 출하·청구된 수주(INVOICED)를 영업상 명시적으로 **거래 종료(CLOSED)** 처리하는 흐름. 수금 여부와 무관(실입금은 FI Payment 소관). CLOSED 는 terminal 상태로 이후 출하/청구/재계산에서 동결.
- **백엔드**(이전 세션 산출, 미커밋): `SalesOrder.close()`(INVOICED→CLOSED 가드) + `recomputeStatus()`/`ensureProgressable()` 가 CLOSED 를 동결(추가 출하/청구 거부) + `SalesOrderService.close(id)` + `SalesOrderController` `POST /api/sales-orders/{id}/close`(클래스 `@PreAuthorize hasAnyRole('SALES','ADMIN')` 상속). 테스트 3건(정상마감/INVOICED아니면거부/마감후동결) **그린**.
- **UI**(이번 세션 추가): `sd/order/detail.html` `renderActions()` 에 **INVOICED 상태일 때 "마감 (거래 종료)" 버튼**(기존 `act('close','마감')` 패턴, `POST .../close` 호출). 상태 배지 맵 `detail.html`·`list.html` 양쪽에 **CLOSED='거래종료'** 추가(STEP5 시드한 다수 CLOSED 수주가 영문 회색으로 나오던 것 교정). `list.html` 상태 필터 드롭다운에 **거래종료** 옵션 추가.
- **검증**: `SalesOrderTest` BUILD SUCCESSFUL(4건 그린). 프론트는 순수 HTML — ⚠️ 헤드리스 없어 실제 버튼 클릭→마감 렌더는 hwlee님 육안 권장.
- ⚠️ **미커밋** — 다른 PC에서 이어가려면 hwlee님이 커밋+푸시. (변경 파일: SalesOrder.java·Service·Controller·Test + detail.html·list.html)

### 🗓 2026-06-27 세션 ② — 실무 리얼리즘 확장 프로젝트 (포트폴리오 공개용)

> **목표**: 외부 공개 포트폴리오 → 데이터 정합성·디테일 최우선. hwlee님 요청.
> **확정 스펙**: 회사 hyunwoo전자 **설립 2024**. 연 매출(실거래 합) **2024=150억 / 2025=200억 / 2026 상반기≈160억**(연목표 300억). 매출을 **요약분개가 아니라 실거래(구매→생산→판매→입금 전 체인)**로 깐다. 과거 거래는 완결(CLOSED) 상태로 시드해 현재 ATP·MRP·여신 비오염, 오늘 시점 재고·미수금 정상값.

- **STEP 1 — KRaft 전환 ✅ (2026-06-27 완료)**: `docker-compose.yml` 에서 **zookeeper 서비스 제거**, kafka 를 KRaft 단일노드(broker+controller)로 재구성(`KAFKA_PROCESS_ROLES=broker,controller`, CONTROLLER 리스너 29093, `CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk`→정규화 `...Qg`). **앱 코드 변경 0**(여전히 localhost:9092). cp-kafka 이미지 익명볼륨에 zookeeper 시절 데이터가 남아 첫 기동 때 옛 클러스터ID로 떠서 → `docker compose rm -sfv kafka` 로 볼륨 비우고 재포맷. 검증: `kafka-metadata-quorum describe --status` LeaderId 1·CurrentVoters[1]·healthy, zookeeper 컨테이너 소멸. README 2곳 "Kafka+Zookeeper"→"Kafka(KRaft)" 정리. (⚠️ 앱 레벨 Outbox→Kafka→ERP E2E 는 앱 기동 시 확인 권장 — 브로커 레벨은 검증됨.)
- **STEP 2 — 카테고리 코드 마스터화 ✅ (2026-06-27 완료)**: `ItemCategory` enum → **마스터 엔티티/테이블**(`item_category`)로 일반화. 이제 데스크탑·키보드 등 카테고리를 **코드 수정 없이 데이터로** 추가 가능.
  - Flyway **V46**: `item_category`(code uniq·name·sort_order·status·soft-delete) + 기존 enum 3종(NOTEBOOK/MONITOR/PART) 이관 + `ALTER item ADD FK fk_item_category (category→item_category.code)`.
  - 신규: `ItemCategory`(enum→@Entity, BaseEntityWithCode), `ItemCategoryRepository`, `ItemCategoryController`(`GET /api/item-categories`, 드롭다운용), `dto/ItemCategoryResponse`.
  - 변경: `Item.category` enum→**String 코드**, DTO 3종(`@NotBlank @Size(20) String category`), `ItemService`(생성/수정 시 `requireActiveCategory` 검증), `ItemSpecifications.categoryEquals(String)`, `ItemController` 검색 파라미터 String. 품목 전용 화면 없어 템플릿 변경 0.
  - 테스트 11파일 `ItemCategory.NOTEBOOK/.MONITOR` → 문자열 치환. **검증**: `compileJava`/`compileTestJava` 그린 + `ItemCrudIntegrationTest`(Testcontainers) 통과(V46 적용·FK·카테고리 검증 동작). 부수효과로 KRaft Kafka 앱 연결도 확인됨.
- **STEP 3 — Factory(공장) 마스터 신설 ✅ (2026-06-27 완료)**: 공장 3개 거점 + 창고/설비를 공장에 소속. 생산지시는 자기 창고를 통해 공장에 연결(정규화, production_order 무변경).
  - ERP: 신규 `master/factory`(Factory[BaseEntityWithCode], FactoryRepository, FactoryController[`GET /api/factories`·`/by-code/{code}`], dto/FactoryResponse). `Warehouse` 에 `@ManyToOne Factory factory`(nullable)+`assignFactory()`, `WarehouseResponse`+`WarehouseMapper` 에 factoryCode/factoryName 노출.
  - ERP Flyway **V47**: factory 테이블 + 3공장 시드(**FAC-01 수원·FAC-02 구미·FAC-03 광주**) + `warehouse.factory_id` FK(nullable) + WH-HQ→FAC-01 배정.
  - MES Flyway **V8**: `equipment.factory_code` 컬럼(ERP factory.code 코드참조, 공유FK 아님) + 기존 EQ-001/002→FAC-01. `Equipment` 엔티티에 factoryCode 필드(생성자 4-arg).
  - **검증**: ERP/MES compileJava·compileTestJava 그린 + `MmScenarioTest`(Testcontainers) 통과(V47 적용·창고 런타임). ⚠️ MES는 테스트 모듈이 없어 V8은 다음 MES 기동 시 적용(SQL 단순 ALTER+UPDATE).
- **STEP 4 — 마스터 대량확장 ✅ (2026-07-01 완료)**: 카탈로그(`doc/실무리얼리즘확장-STEP4-카탈로그.md`)를 **결정론적 Python 생성기**(`tools/step4-seed/generate.py`, seed 고정)로 SQL 산출 → Flyway 편입. 재실행 시 동일 SQL 보장.
  - **결정 사항**(설계 세부, AI가 합리적 기본값으로 확정): ① 코드에 **실제 연도 반영**(고객 CUST-2024/25/26·직원 EMP-2024~26 분산, code_sequence 연도별 UPSERT) ② STEP4는 **부품 기초재고만** 3공장창고 적재(완제품 재고는 STEP5 생산에 위임) ③ 논리 단위 파일 분할.
  - **ERP Flyway V48~V58**: V48 카테고리 확장(완제품 6+부품세분 10, 기존 PART→INACTIVE) / V49 품목(완제품 14+부품 24 신규, 기존 부품 10 재분류, ITEM next=51) / V50 BOM 14세트(77라인, 부품명 JOIN) / V51 창고(WH-HQ 중앙물류 재배정 factory_id=NULL + WH-SW/GM/GJ 신설) / V52 부서 본부-팀 2계층 재편(신규 11+기존 4 재편, 코드 불변으로 FK·user_role 보존) / V53 고객 98 신규(=총 100, 파레토 한도 4단계) / V54 거래처 17 신규(=총 18) / V55 직원 105 신규(=총 110, 사무 18+생산 92) / V56 급여계약 106(전원 발효) / V57 로그인 8 신규(담당+팀장, user_role 매핑) / V58 부품 기초재고(24종×500×3창고)+개시분개(차 1410/대 3100, **43.5억 균형**).
  - **MES Flyway V9**: 공장별 설비 6(EQ-1xx/2xx/3xx)+작업자 6(OP-1xx/2xx/3xx). 기존 EQ-001/002·OP-001/002와 무충돌.
  - **검증**: `MmScenarioTest`·`ItemCrudIntegrationTest`(Testcontainers) **통과** = V48~V58 전체가 실제 MySQL에 성공 적용(FK/UNIQUE/카테고리 제약 통과). Python으로 BOM 완제품명↔품목명 일치(0-row 방지)·개시분개 차대균형·business_no/코드 무충돌 검증. ⚠️ MES V9는 테스트 모듈 없어 다음 MES 기동 시 적용(단순 INSERT).
- **STEP 5 — 2024~2026상반기 실거래 백필 ✅ (2026-07-01 완료)**: **결정론적 시뮬레이터**(`tools/step5-seed/generate.py`, seed 고정)가 3년치 ERP 운영을 일자순 시뮬레이션해 완결(CLOSED) 거래를 SQL 산출. 구매→생산→판매→입금 전 체인 + 이동평균 원가 + 분개 차대균형.
  - **사용자 선택**: 거래 상세도=**개별 거래 다수**, 정합성=**완전 시간순 시뮬**(가장 리얼).
  - **결정 사항**(AI 확정): ① 창고=**WH-HQ 중앙창고 단일 기준**(부품입고→생산→출하; STEP4 공장창고 기초재고는 정적 보유) ② 모든 거래 날짜 **≤2026-06-30**(오늘 전)으로 제한 → 라이브 채번(오늘 이후)과 완전 분리, code_sequence 갱신 불필요 ③ 부품/완제품 **대량 로트**(입고 빈도↓) ④ 입금일>컷오프면 미입금(오늘 시점 미수금으로 잔존).
  - **ERP Flyway V60~V63**: V60(2024)·V61(2025)·V62(2026H1) 연도별 완결 트랜잭션 + V63 종료시점 WH-HQ 재고 UPSERT. 세션변수(@i_*/@c_*/@v_*/@a_*/@wh)+LAST_INSERT_ID()로 id 참조. 완결판매=SO(CLOSED)+line+delivery(SHIPPED)+goods_issue(delivery_id,GOODS_ISSUE 이동평균)+invoice(VAT10%)+payment(RECEIPT)+분개3(매출 1200/4100·2200, 매출원가 5100/1400, 입금 1100/1200). 생산=production_order(COMPLETED)+PRODUCTION_OUT/IN+분개(1400/1410). 입고=goods_receipt(POSTED,이동평균)+분개(1410/2100)+출금(2100/1100).
  - **산출 규모(scale=1.0)**: 판매 781·생산 547·입고 492·전표 3,772. **매출(공급가) 525억**(목표 510억, +3%). **음수 재고 0**. 오늘 미수금(미입금 AR) ~36억. SQL 총 ~6.3MB.
  - **검증**: `MmScenarioTest`(Testcontainers)로 V60~V63 전체가 실제 MySQL에 성공 적용 + **전체 94개 테스트 그린**. 시뮬레이터가 이동평균·차대균형·음수재고·채번상한(하루≤27)·오늘이후 날짜유출(0) 자체 검증.
  - ⚠️ **RDS 적용 시 주의**: STEP5는 마이그레이션 시드 기준 clean 3년치 → 이전 세션들의 **런타임 검증데이터(SO-20260610 등)는 정리 권장**(정합성 오염 방지).
- **STEP 6 (테스트 영향 정리) — 대부분 완료 ✅ (2026-07-01)**: STEP5 도입 시 발견한 **통합테스트 회귀 18건 수정**(실제 원인=대부분 STEP4 커밋 `9733d64`의 미검증 회귀):
  - **신용한도 12건**(FiAccounting·SalesOrderCrud·SdMm·PartialDelivery·CustomerCrud): 테스트가 `findById().changeCreditLimit();flush()`로 **detached 엔티티**를 변경해 한도 미반영 → `saveAndFlush`로 수정.
  - **부서 재편 3건**(Department·Employee): STEP4 V52 재편(DEPT-SALES→국내영업1팀/부모 DEPT-SALESHQ, DEPT-RND 실존)을 테스트 기대값에 반영.
  - **회계/재고 격리 3건**(FiAccounting 전체시나리오·Production·SdMm): 전역 계정잔액/원가/원장이 STEP5 시드로 오염 → 델타 검증·그 시점 이동평균 기반 기대값·null-safe refId 비교로 격리.
  - ▶ **남은 STEP 6 = `doc/` 학습 문서 + 최종 PROGRESS 정리**(학습 트랙).
- (신규 기능·데이터 STEP 밖) **전자결재(전사 횡단)**: 결재는 견적만이 아니라 구매발주·지급·전표·근태/휴가·생산지시 등 공통(hwlee님 지적). **설계 문서 = `doc/실무리얼리즘확장-전자결재-설계.md`**. 권장=범용 결재 엔진(approval_request/step/rule + 알림 + 문서 콜백), 단계적 도입(코어+견적→구매→지급→…). 기존 여신/계획오더 승인흐름과 통합 고려. ▶ **미결정**: 범위·엔진방식·타이밍(데이터 후 권장). 부서별 담당+팀장 로그인(STEP4)이 상신자/결재자.
- ⚠️ **미커밋** — 다른 PC에서 이어가려면 hwlee님이 커밋+푸시.

### 🗓 2026-06-27 세션 ① — DB를 Docker MySQL → AWS RDS 전환 (PC 간 공유 DB)

> ⚠️ **비밀번호 값은 git 에 두지 않는다.** 아래 환경변수 이름만 기록하고, **실제 값 2줄은 개인 비밀번호 관리자(키체인/메모 등)에 따로 보관** → 각 PC에서 한 번씩 환경변수로 주입. (회사 RDS 접속정보를 평문 커밋하면 git 히스토리에 영구 노출되어 비번 교체가 필요해짐.)

- **변경 내용**: 로컬 Docker MySQL 대신 **회사 AWS RDS(MySQL 8.4)** 를 학습 DB로 사용 → 집/회사 어느 PC에서 켜도 같은 데이터를 본다. `aws` 스프링 프로파일 신설(커밋 `aa68614`).
  - 추가 파일: `hwlee-erp/src/main/resources/application-aws.yml`, `hwlee-mes/src/main/resources/application-aws.yml` (둘 다 비번은 `${ERP_DB_PASSWORD}` / `${MES_DB_PASSWORD}` 환경변수 참조, 평문 없음).
- **접속 정보**:
  - 호스트: `pro-ddakple.cltffqc0oqvb.ap-northeast-2.rds.amazonaws.com:3306`
  - ERP: 스키마 `erp_db` / 유저 `erp_app` / 비번 환경변수 `ERP_DB_PASSWORD`
  - MES: 스키마 `mes_db` / 유저 `mes_app` / 비번 환경변수 `MES_DB_PASSWORD`
  - 🔑 **비번 값 2줄은 개인 비밀번호 관리자에서 꺼내 채운다**(git 에 없음).
- **다른 PC(집)에서 처음 켜는 법** — 둘 중 하나로 환경변수 설정(한 번만):
  - **방법 A — 셸 `~/.zshrc`** (터미널 `./gradlew bootRun` 용):
    ```bash
    export ERP_DB_PASSWORD='<관리자에서 복사>'
    export MES_DB_PASSWORD='<관리자에서 복사>'
    ```
    추가 후 `source ~/.zshrc`(또는 터미널 재시작).
  - **방법 B — IntelliJ 실행 구성**(IDE Run 버튼용): 각 Application 실행 구성 → *Edit Configurations* → **Environment variables** 에 `ERP_DB_PASSWORD=...` (MES 구성엔 `MES_DB_PASSWORD=...`). `.idea/` 는 gitignore 라 저장소에 안 올라감.
- **실행 시 프로파일 지정**: `SPRING_PROFILES_ACTIVE=aws` (예: `SPRING_PROFILES_ACTIVE=aws ./gradlew bootRun`, 또는 IntelliJ 실행 구성의 Active profiles=`aws`). ⚠️ 회사 RDS 보안그룹에 그 PC의 공인 IP가 3306 인바운드로 열려 있어야 접속됨.

### 🗓 2026-06-25 세션 — Phase 17(자연어 검색/Text-to-SQL) 설계 합의 + 계획 편입

> ⚠️ **문서 편집만 함(STUDY-PLAN Part 3 신설·이 항목 추가). 코드 미착수·미커밋** — 다른 PC에서 이어가려면 hwlee님이 커밋+푸시.

- **요청**: hwlee님 "Ollama 설치해 자연어 검색(Text-to-SQL) 구현해볼까?" → 정식 Phase로 편입 결정.
- **합의된 설계(STUDY-PLAN `Part 3 > Phase 17`에 상세)**:
  - **런타임**: 로컬 **Ollama**. **연동 위치 = hwlee-erp 통합**(`/api/nl-query`), MES DB는 읽기전용으로 같이 조회. **대상 = ERP+MES 둘 다**. **깊이 = 가드레일까지 제대로**.
  - **모델**: `qwen2.5-coder:7b`(17-A에서 `qwen2.5:7b`와 비교 후 확정). ⚠️ **사양 제약 = M2 Pro/통합 16GB** → Ollama 가용 4~6GB(Docker 인프라+ERP/MES 앱+IDE가 나머지). 7B≈5GB가 상한, **14B는 무리**(Docker 내리고 단독 시만).
  - **구성**: 17-A 단일DB PoC → 17-B **가드레일 4종**(읽기전용 계정 erp_ro/mes_ro·JSqlParser 단일SELECT 검증·강제LIMIT+타임아웃·화이트리스트 스키마카드) → 17-C 정확도(스키마카드+few-shot+self-correct 1회+**SQL 미리보기 후 실행**) → 17-D 두 DB 라우팅 + **Function Calling 비교**.
  - **vs Function Calling**: 기존 2026-06-15 "읽기전용 조회 비서" 아이디어(REST API 도구호출)와 **접근이 다름**. 실무 안전성은 FC 우위지만, 이 Phase는 자연어→SQL 변환·가드레일 학습이 목적이라 Text-to-SQL을 메인으로 하고 17-D에서 비교.
- **▶ 다음 액션(구현 착수 시)**: ① `ollama` 설치 + `qwen2.5-coder:7b`·`qwen2.5:7b` pull ② hwlee-erp `build.gradle.kts`에 `spring-ai-ollama` + `JSqlParser` 추가 ③ 17-A PoC 1건.

### 🗓 2026-06-15 세션 — 버그 수정 2건 + 💡 AI 접목 아이디어(미래 적용 후보)

> ⚠️ **이 세션 코드 작업 전체 미커밋** — 다른 PC에서 이어가려면 **hwlee님이 직접 커밋+푸시** 해야 함.

- **① 수주 상세 ATP "출고가능 수량" 빨강 오표시(이중 차감) 버그 수정**(hwlee님이 /sd/sales-orders/7 보다 발견).
  - **증상**: CONFIRMED 수주 상세에서 출고가능 수량이 양수(예 37, 183)인데도 빨강으로 표시됨.
  - **원인**: `committed`(출하대기) 쿼리(CONFIRMED~INVOICED 미출하 합)에 **지금 보고 있는 그 수주의 물량이 이미 포함** → ATP는 그걸 이미 뺀 값인데, 빨강 판정 `orderQty > atp`가 **그 수주 물량을 한 번 더 차감**(이중 차감)해서 멀쩡한 주문도 빨강.
  - **수정**(`sd/order/detail.html` `loadLineAtps`): CONFIRMED/SHIPPING 수주는 이미 committed에 들었으므로 추가 수요 0으로 본다 → `demand = (CONFIRMED|SHIPPING) ? 0 : orderQty; over = demand > atp`. 결과적으로 **확정수주는 atp < 0 일 때만 빨강**(= 진짜 부족). **DRAFT 수주는 기존대로 `orderQty > atp`**(확정 시 한도 초과 예고). `form.html`(신규 작성=DRAFT)은 손대지 않음.
- **② 출하 폼 "권한 없음" 배너 버그 수정**(hwlee님이 kim(SALES)으로 출하 생성 시도 중 발견).
  - **증상**: 출하 신규 폼은 떴는데(=페이지 GET 통과) 빨간 "이 작업을 수행할 권한이 없습니다" 배너 + 창고 드롭다운 빈 상태.
  - **원인**: 출하 폼이 창고 목록을 부르는 `GET /api/warehouses` 가 `WarehouseController` 클래스 단위 `@PreAuthorize`로 **PURCHASING/PRODUCTION/ADMIN만 허용 → SALES 빠짐** → kim 호출이 403. 출하 자체 권한(`DeliveryController`·`SalesViewController`=SALES/ADMIN)은 정상.
  - **수정**(`mm/warehouse/WarehouseController.java`): "창고 조회는 넓게, 변경은 좁게" 원칙(StockController와 동일 사상). **클래스 단위(=GET 조회)** → `SALES,PURCHASING,PRODUCTION,FINANCE,ADMIN` 로 확대. **쓰기(POST/PUT/DELETE)** → 메서드 단위 `@PreAuthorize`로 기존 `PURCHASING,PRODUCTION,ADMIN` 유지(영업은 창고 생성·수정·삭제 불가).
  - ⚠️ 적용에 앱 재시작(또는 클래스 핫리로드) 필요. 헤드리스 없어 실제 드롭다운 채워짐은 hwlee님 육안 권장.

- **💡 AI 접목 아이디어 — 나중에 적용해볼 후보(학습 커리큘럼 외 별도 트랙, hwlee님 "LLM 파인튜닝/Agent AI 가 뭐고 이 ERP에 어떻게 접목?" 대화)**:
  - **개념 정리**: ① **LLM 파인튜닝** = 범용 LLM의 가중치를 우리 데이터로 추가 학습해 특정 용도(말투·형식·도메인)에 고정. ② **Agent AI** = LLM에 "도구(tool/function calling) + 자율 실행 루프"를 붙여 자연어 요청을 실제 행동(API 호출)으로 수행. (지금 쓰는 Claude Code가 에이전트 사례.)
  - **이 ERP의 강점**: REST API ~180개가 이미 있음 = 에이전트가 호출할 "도구"가 준비됨 → 접목 쉬움.
  - **Agent 접목 후보(ROI 높음, 권장 우선)**: ⓐ **읽기 전용 조회 비서**(ATP·신용한도·재고·리포트 자연어 질의 → 기존 GET API 호출) ← *추천 1단계, 안전*. ⓑ **분개 추천 에이전트**(자연어 거래 → 차/대 분개 제안 → `/api/journal-entries`, 기존 자동분개의 수동 버전, FI 복습가치). ⓒ **MRP/생산 어시스턴트**(자재 가용성 질의 → `/material-availability`). ⓓ **이상징후 알림 에이전트**(야간 배치 결과를 LLM이 요약 → 기존 Notification 푸시).
  - **기술 메모**: Spring Boot에 `/api/assistant/chat` 류 엔드포인트 추가 → Claude API tool use에 각 기존 엔드포인트를 함수로 정의해 LLM↔기존 API 중개. 두뇌는 최신 Claude(Opus 4.8 등).
  - **파인튜닝**: ERP 학습 단계에선 우선순위 낮음 — 대부분 **프롬프트 + RAG(계정과목표·업무규칙 검색해 프롬프트에 주입)로 충분**. 말투·형식·특수 도메인을 모델에 "못 박아야" 할 때만 마지막 카드로 고려.
  - **추천 로드맵**: 1) 읽기전용 조회 에이전트(function calling 체득) → 2) RAG 추가 → 3) 쓰기 액션 에이전트(사람 승인 끼고) → 4) (선택) 파인튜닝.
  - ▶ **상태 = 아이디어만, 미착수.** 실제 진행하려면 위 1단계를 구현 계획으로 풀어 별도 Phase로 편성 가능.

### 🗓 2026-06-14 세션 — 출고가능 수량(ATP) UI 개선 + 로컬 핫리로드 설정

> ⚠️ **이 세션 작업 전체 미커밋** — 다른 PC에서 이어가려면 **hwlee님이 직접 커밋+푸시** 해야 함.

- **① 출고가능 수량(ATP) 표시 디자인 변경**(hwlee님 요청, 순수 프론트). 신규 수주 폼·수주 상세 양쪽 동일하게:
  - **분해(현재고 − 출하대기 + 입고예정)를 ⓘ 호버 툴팁에서 빼서 수량 옆에 항상 인라인 표시.** (예: `📦 출고가능 수량 40 (현재고 50 − 출하대기 10 + 입고예정 0)`)
  - **"주문량이 출고가능 수량 초과" 빨강 배지 제거.**
  - **색상 규칙 통일**: 주문량 > 출고가능이면 "출고가능 수량 N"이 빨강(`text-danger fw-semibold`), **주문량 ≤ 출고가능(많거나 같음)이면 일반(회색 text-muted)**. 두 화면 모두 판정식 `over = 주문량 > ATP` 로 일치.
  - 파일: `sd/order/form.html`(`renderLineAtp` 재작성, ⓘ·배지 제거), `sd/order/detail.html`(`loadLineAtps`에 주문량 비교 추가 — 라인 `<tr>`에 `data-order-qty` 심고 빨강/회색 분기).
  - ⚠️ 헤드리스 없어 실제 렌더(빨강 표시)는 hwlee님 육안 확인 권장.
- **② 로컬 핫리로드 설정**(hwlee님 "화면 수정이 재시작 없이 반영 안 됨" 질문). 원인 = thymeleaf 캐시 기본 on + devtools 없음.
  - `application-local.yml`: `spring.thymeleaf.cache=false` + `spring.web.resources.cache.period=0` 추가.
  - `build.gradle.kts`: `developmentOnly("org.springframework.boot:spring-boot-devtools")` 추가.
  - ⚠️ **적용에 필요한 액션(다른 PC에서도)**: Gradle reload(동기화) + 앱 1회 재시작. 이후 HTML 수정은 IDE→브라우저 전환(`ON FRAME DEACTIVATION` 리소스 복사) + `Cmd+Shift+R`로 반영. (IntelliJ Run config는 이미 Update classes and resources 설정됨.)

### ▶ 다음 작업 (2026-06-11 세션 종료 — 다음에 여기부터)

> **✅ 재고 가시성(영업 ATP) = 레벨② 완료**(2026-06-11, 바로 아래 구현 요약 참조).
>
> **🅰 우선 후보 = 보조 후보들** — 착수 전.
> - ① **거래처(Vendor) 마스터 등록 화면 + 권한 분리** — 여신 결재 이력은 했고, 거래처도 고객 마스터와 같은 단순화(현재 구매팀 등록 화면 없음, API만). 고객 마스터(`master/customer/`) 패턴 그대로 복제.
> - ② (선택·확장) **재고 가시성 레벨③** = 영업용 재고/ATP 조회 화면 추가(현재는 수주 화면 인라인까지만 = 레벨②). 사이드바 영업 그룹에 "재고 현황" 메뉴.
> - ③ 학습 문서 `doc/03 MM` 부터(아직 01·02만 작성). ④ doc/에 추가분(MRP·알림·여신·고객권한·ATP) 워크스루 글.
>
> ⚠️ **2026-06-11 작업분(재고 가시성 ATP) 미커밋** — 다른 PC에서 이어가려면 **hwlee님이 직접 커밋+푸시** 해야 함. (그 이전 6/10분은 커밋됨 = `42af6fe`.)

### ➕ 재고 가시성 — 영업 ATP(약속가능재고) 레벨② (2026-06-11, hwlee님 요청 "재고 가시성 기능 착수")

- **배경(갭)**: 영업(SALES)이 재고를 조회조차 못 했음(`StockController` 가 PURCHASING/ADMIN). 영업이 출하·약속하려면 재고를 알아야 하는데 못 봄. 또 "총 재고"만 보면 이미 다른 수주에 묶인 물량을 중복 약속하는 사고.
- **ATP 공식(설계 결정)**: `ATP = 현재고(전 창고 합) − 미출하 확정수주(orderQty−shippedQty, status CONFIRMED~INVOICED) + 진행 중 생산예정(PLANNED/RELEASED 생산지시 수량)`. **입고예정(구매발주)은 이 시스템에 별도 모델이 없고, 영업이 약속하는 완제품은 생산으로 충당되므로 생산예정으로 대표**(입고예정 제외). DRAFT 수주는 약속으로 보지 않음(신용한도 검증과 같은 사상).
- **백엔드 추가**: `sd/atp/`(AtpService·AtpController·dto/AtpResponse), `sd/order/SalesOrderLineRepository`(신규, `sumUnshippedCommittedByItem`). 변경: `ProductionOrderRepository.sumOpenProductionQtyByProduct`(PLANNED/RELEASED 완제품 수량 합), **`StockController` 권한 PURCHASING/ADMIN → SALES 추가**(재고 "조회는 넓게, 변경은 PURCHASING만" 원칙). API `GET /api/atp?itemId=`(권한 SALES/PURCHASING/FINANCE/ADMIN). **Flyway 변경 없음**(기존 데이터만 사용).
- **화면(순수 프론트)**: 화면 라벨은 **"출고가능 수량"**(hwlee님 선택 2026-06-11; 내부 도메인 용어는 ATP). 분해(현재고/출하대기/입고예정)는 **ⓘ 아이콘 호버 툴팁(native title)** 으로 숨김. ① **수주 폼**(`sd/order/form.html`): 품목 라인마다 선택 시 ATP 조회→라인 아래 "출고가능 수량 N ⓘ" 인라인 표시, 주문량 > ATP면 빨강 "주문량이 출고가능 수량 초과" 배지(주문량 입력마다 재평가). ② **수주 상세**(`detail.html`): 진행 중(DRAFT/CONFIRMED/SHIPPING) 수주의 각 라인에 "출고가능 수량 N ⓘ"(툴팁=현재고−출하대기+입고예정) 표시.
- **검증(end-to-end, 컴파일 그린·앱 재기동)**: ① 노트북(1): onHand 68 − committed 5 + 생산예정 5 = **ATP 68** / 모니터(2): 7 − 10 + 0 = **ATP −3**(음수=중복약속 위험 정확 노출) — **API 응답 = DB 직접 쿼리 정확 일치** ② kim(SALES)→`/api/stocks` 200(권한 확장 동작)·`/api/atp` 200 ③ park(역할없음)→`/api/atp` **403** ④ 수주 폼·상세 HTTP 200 + ATP 마크업(loadLineAtp/약속가능재고/atp-cell) 포함. ⚠️ 헤드리스 없어 품목 선택→ATP 렌더·초과 빨강 경고는 hwlee님 육안 권장.

- **▶ 학습 문서 트랙 = `03 MM` 부터**(hwlee님 요청 2026-06-09). 04·05 완료 → 01 마스터 ✅ → 02 SD ✅ → **03 MM** 채운 뒤 06~16. 각 주제 **①도메인 → ②구현** 2단계.
- **화면 방식**(hwlee님 선택 2026-06-09): **글+코드로 인용**. 앱 안 띄움. 구현 단계에서 화면 템플릿/REST 흐름을 글에 인용. (이 환경은 헤드리스 브라우저 없어 직접 렌더 불가.)
  - 🖥️ 실행/종료: 루트 `실행방법.md` (ERP 8080 / MES 8082 / docker). Zipkin 9411.

### ➕ SD↔PP 연계 기능 추가 — 계획오더(MRP) (2026-06-10, hwlee님 요청 "수주 확정→재고 부족분 계획오더 자동생성→담당자 승인→생산지시 전환")

- **흐름**: 수주 확정 → `SalesOrderConfirmedEvent` 발행(`BEFORE_COMMIT`) → PP 리스너가 완제품별 **주문량 vs 현재고(전 창고 합)** 비교 → 부족분만큼 **계획오더(PROPOSED)** 자동 생성 → 담당자가 **승인(convert, 창고·납기 지정)→생산지시(PLANNED) 전환** 또는 **기각(dismiss)**. 실무의 MRP "계획오더는 제안, 사람이 승인" 개념. 견적→수주 전환과 같은 패턴(별도 엔티티 + markConverted).
- **설계 결정**: ① 계획오더는 생산지시와 **별개 엔티티**(`pp.planning.PlannedOrder`, BaseEntity, status PROPOSED/CONVERTED/DISMISSED) ② 현재고=품목 전체 보유 합(`StockRepository.sumOnHandByItem`, 창고 무관) ③ **창고는 계획 단계 미정 → 전환 시 담당자 지정**(계획오더에 warehouse 없음) ④ 수주 확정과 같은 트랜잭션(BEFORE_COMMIT, 기존 패턴). convert 는 기존 `ProductionService.createDraft` 재사용(BOM 없으면 거기서 거부).
- **추가 파일**: `pp/planning/`(PlannedOrder·Status·Repository·Service·Controller·dto 2), `pp/integration/sd/SalesOrderConfirmedListener`, `sd/order/event/SalesOrderConfirmedEvent`. 변경: `SalesOrderService.confirm`(이벤트 발행 + `ApplicationEventPublisher` 주입), `StockRepository.sumOnHandByItem`, `TransactionNumberGenerator`(PLO 채번), `PpViewController`(/pp/planned-orders), `layout.html`(사이드바 "계획오더(MRP)" active='planned'). 화면 `templates/pp/planning/list.html`(목록+승인 모달[창고 선택]+기각). **Flyway V42**(planned_order).
- **검증(end-to-end, 컴파일 그린·앱 재기동·V42 적용)**: ① 노트북60 수주 확정 → `PLO-20260610-001` 부족7(=60-53) 자동생성 → 승인(본사창고) → 생산지시 `PO-20260610-001` 노트북7·BOM 5라인 전개·PLANNED, 계획오더 CONVERTED+생산지시번호 역기록 ② **재고 충분**(노트북5≤53) → 제안 0건(조용히 통과) ③ **기각**(모니터3, 재고0) → `PLO-20260610-002` 부족3 → dismiss → DISMISSED ④ 화면 `/pp/planned-orders` HTTP 200·스크립트 포함. ⚠️ 헤드리스 없어 모달 클릭 렌더는 hwlee님 육안 권장.
- ⚠️ **검증용 데이터**: 수주 SO-20260610-001(노트북60,CONFIRMED)·수주5(노트북5,CONFIRMED)·수주(모니터3,CONFIRMED), 계획오더 PLO-001(CONVERTED)·PLO-002(DISMISSED), 생산지시 PO-20260610-001(노트북7,PLANNED). 불필요시 삭제 가능. **전체 미커밋** — 커밋·푸시는 hwlee님이 직접.
- 📌 (선택) 학습 워크스루 문서 미작성 — 원하면 `doc/`에 "SD↔PP 연계(MRP)" 글 추가 가능.

#### 🔧 생산지시 취소 정합성 갭 2건 보강 (2026-06-10, hwlee님이 취소 눌러보다 발견)

- **발견**: 생산지시 취소 시 ① 그걸 만든 계획오더는 CONVERTED("처리됨")로 잠긴 채 거짓 정보 ② 수주는 출하 대기인데 채울 생산이 죽은 걸 모름 ③ MES 전송된 생산지시도 ERP만 취소되면 MES와 불일치. "출하 시점 재고부족으로 막히긴 하나(데이터 손상X) 미리 경고 없음(가시성 문제)" → hwlee님 지적 타당.
- **질문1 답(확인)**: 종료(CANCELLED) 생산지시 상태 변경 불가 = **정상 설계**(terminal 상태, 잘못 취소 시 새 생산지시가 정석). 단 계획오더 되살리기로 "다시 만들기" 경로를 열어줌.
- **개선A — 되살리기**(견적 revertConversion 패턴): 생산지시 취소 → `ProductionOrderCancelledEvent`(pp.order.event) 발행 → `pp/planning/ProductionOrderCancelledListener`(BEFORE_COMMIT) → `PlannedOrderService.revertByProductionNumber` → 계획오더 `revertConversion()`(CONVERTED→PROPOSED, 전환번호 클리어). `PlannedOrderRepository.findByConvertedProductionNumber`. **순환의존(order↔planning)은 이벤트로 끊음**(planning→order는 convert에서 직접 호출, 역방향은 이벤트).
- **개선B — MES 가드**(SD "출하 실적 있으면 수주 취소 거부" 사상): `ProductionOrder.cancel()` 에 `mesWorkOrderNo != null` 이면 취소 거부 가드 추가.
- **검증(end-to-end)**: ① 노트북70 수주 확정→계획오더 PLO-20260610-003 부족14→승인(생산지시)→**생산지시 취소→계획오더 PROPOSED+전환번호 null 복원** ✓ ② 복원 계획오더 재승인→생산지시→release→dispatch(MES WO-20260610-003 생성)→**취소 시도 거부**(메시지 정확) ✓. 컴파일 그린·앱 재기동.
- ✅ **기존 깨진 데이터 정리됨**(2026-06-10): 개선 *전* 취소됐던 PLO-20260610-001(거짓 CONVERTED, 연결 PO-20260610-001은 CANCELLED)을 DB UPDATE 로 **PROPOSED 복원**(converted_production_number=NULL) — 새 revertConversion 로직과 동일 결과. PROPOSED 목록 노출 확인(부족7, 출처 SO-20260610-001). 검증 추가데이터: 수주(노트북70)·PLO-003(CONVERTED)·생산지시 1건 CANCELLED+1건 RELEASED(MES전송 WO-20260610-003). **미커밋**.

### ➕ 인앱 알림 + 여신 상향 요청/승인 (2026-06-10, hwlee님 요청 "알림+딥링크+재무 한도상향, 포트폴리오 디테일")

- **A. 알림(Notification) 코어** `com.hwlee.erp.notification`: `Notification`(수신자 username·type·title·message·**linkUrl 딥링크**·is_read, BaseEntity) + `NotificationType`(PRODUCTION_CANCELLED/CREDIT_REQUEST_SUBMITTED/APPROVED/REJECTED) + `NotificationService`(**notifyUser**[1명]/**notifyRole**[역할 전체 fanout, 수신자별 1행]·list·unreadCount·markRead[본인만]·markAllRead) + `NotificationController`(Principal 기반, `/api/notifications` GET·unread-count·{id}/read·read-all). **Flyway V43**. `AppUserRepository.findByRoleCode`(역할별 사용자, enabled만) 추가.
  - **🔔 벨 UI**(`layout.html` 상단바 전역): 안읽은 수 badge·**30초 폴링**·드롭다운 최근8건(type별 아이콘/색)·클릭 시 읽음처리+**딥링크 이동**·"모두 읽음". 인박스 화면 `/notifications`(`notification/list.html`, 전체/안읽음 필터·페이징). layout `<style>`+`<script>` 공통 주입(모든 화면 적용).
- **B. 생산취소 → 영업 알림**: `PlannedOrderService.revertByProductionNumber`(생산취소 복원 시)에서 출처 수주의 **salesperson(Employee.email) 있으면 그 사람, 없으면 SALES 역할 전체**에게 PRODUCTION_CANCELLED 알림 + 딥링크=해당 수주(`/sd/sales-orders/{id}`). SalesOrderRepository·NotificationService 주입.
- **C. 여신 상향 요청/승인** `com.hwlee.erp.fi.credit`: `CreditLimitRequest`(number CLR·customer·currentLimit 스냅샷·requestedLimit·reason·status PENDING/APPROVED/REJECTED·decidedBy/At/Note) + 상태전이(submit[요청>현재 검증]/approve/reject) + `CreditLimitRequestService`(create→**notifyRole FINANCE**, approve→`Customer.changeCreditLimit`[신규 메서드, 한도만 변경·감사로그]+**notifyUser 요청자**, reject→notifyUser) + Controller(조회 SALES+FINANCE, 요청 SALES, 승인/거부 FINANCE). **Flyway V44**·채번 CLR. 화면 `fi/credit/list.html`(목록+영업 요청 모달[고객선택→현재한도 표시]+재무 승인/거부 모달, **역할 플래그는 sec:authorize 요소 유무로 JS 판별**). 사이드바: 영업 그룹 "여신 상향 요청"·회계 그룹 "여신 승인"(active='credit', 같은 화면).
- **검증(end-to-end, 컴파일 그린·앱 재기동·V43/V44 적용, 역할별 계정)**: ① **여신**: kim(SALES) 요청 CLR-20260610-001(소한도사 50만→5천만)→lee(FINANCE) 알림 CREDIT_REQUEST_SUBMITTED(안읽음1)→lee 승인→**고객 한도 50만→5천만 반영**→kim 알림 CREDIT_REQUEST_APPROVED ✓ ② **생산취소**: PLO-20260610-001 승인→PO-20260610-004→취소→계획오더 복원+kim 알림 PRODUCTION_CANCELLED(메시지 정확, 딥링크 `/sd/sales-orders/4`) ✓ ③ 화면 `/notifications`·`/fi/credit-limit-requests` HTTP 200. ⚠️ 헤드리스 없어 벨 드롭다운·모달 클릭 렌더는 hwlee님 육안 권장.
- ⚠️ **검증 데이터**: 알림 다수(kim 2·lee 1), 여신요청 CLR-001(APPROVED, 소한도사 한도 5천만으로 영구 변경됨), 생산지시 PO-004(CANCELLED). **전체 미커밋**.

#### ➕ 포트폴리오 디테일 3종 (2026-06-10, 순수 프론트)

- **① 수주↔여신 연결**: 수주 폼/상세(`sd/order/form.html`·`detail.html`)에서 신용한도 **초과 표시 옆에 "여신 상향 요청" 버튼**(딥링크 `/fi/credit-limit-requests?customerId={선택고객}`). 클릭→여신 화면이 customerId 파람 받아 **요청 모달 자동 오픈+고객 선택+현재한도 표시**(`fi/credit/list.html` DOMContentLoaded에서 `URLSearchParams` 처리, loadCustomers await 후).
- **② 알림 안읽음 시각화**: 드롭다운(layout `.notif-item.unread .ni-title::after` 파란 점·읽은 건 흐리게)·인박스(`notification/list.html` 안읽음 행 제목 `fw-bold`+앞 파란 점).
- **③ 여신 결재 이력 타임라인**: 여신 목록 행 **번호 클릭→상세 모달**(`#detailModal`)에 세로 타임라인(`.cl-timeline`: 요청제출[요청자·시각] → 승인/거부[결정자·시각·메모] or 검토대기). 데이터는 기존 응답 필드 재사용(추가 API 없음).
- **검증**: 앱 재기동 후 마크업/딥링크 포함 확인 — 수주 폼·상세 "여신 상향 요청" 버튼 포함, 여신 `?customerId=3` 200+openDetail/cl-timeline/customerId 처리 포함, 인박스 fw-bold 포함, 3화면 HTTP 200. ⚠️ 버튼 클릭→모달 자동오픈·타임라인·점 렌더는 헤드리스 없어 육안 권장. **미커밋**.

### ➕ 고객 마스터 등록 화면 + 권한 분리 (2026-06-10, hwlee님 요청 "기본정보는 영업, 한도는 재무")

- **배경**: 기존엔 고객 생성/수정/삭제가 **ADMIN only + 화면 없음**(API만) — 영업이 신규 고객 등록 불가 = 영업 흐름 불완전. 실무처럼 기본정보=영업, 신용한도=재무로 분리.
- **권한 분리(코드 레벨 강제)**: `CustomerController` 생성/수정 = **SALES+ADMIN**(삭제는 ADMIN 유지). **신용한도를 양쪽 DTO(`CustomerCreateRequest`/`CustomerUpdateRequest`)에서 완전 제거** → 영업이 API로도 한도 못 넣음. 신규 고객은 `CustomerService.create`가 **한도 0(현금거래) 강제**. `Customer.update(...creditLimit...)` → **`updateBasicInfo(name,address,paymentTerms)`** 로 교체(한도·사업자번호 불변). 한도 변경은 **여신 요청/승인(`changeCreditLimit`)으로만**.
- **화면 3종** `master/customer/`(list·detail·form) + `CustomerViewController`(`/master/customers[/new|/{id}|/{id}/edit]`, SALES/ADMIN) + 사이드바 **영업 그룹 "고객"**(active='customer'). 상세에 한도 표시+"재무 권한" 안내+**여신 상향 요청 버튼**(딥링크), 폼에 "신규 한도0·상향은 여신" 안내·한도 입력란 없음·사업자번호 수정 시 잠금·삭제는 ADMIN만. PaymentTerms=NET30/NET60/COD/PREPAID.
- **테스트 수정**(DTO 시그니처 변경 여파, 서브에이전트): SdMm·PartialDelivery·SalesOrderCrud·CustomerCrud·FiAccounting·AuditLog 6개 테스트에서 creditLimit 인자 제거 + 한도 의존 시나리오는 생성 후 `changeCreditLimit`로 보존. `compileTestJava` BUILD SUCCESSFUL.
- **검증(end-to-end, 앱 재기동)**: ① kim(SALES) 고객 등록 CUST-2026-0004 **한도0** ② 한도 억지 주입(99000000)→**무시(0)** ③ 영업 수정 시 한도 주입(50000000)→**한도 0 유지**(기본정보만 변경) ④ 한도 변경=여신 요청만(CLR-002 0→3천만 PENDING) ⑤ **park(권한없음)→403** ⑥ 고객 화면 4개 HTTP 200·NET30 매핑·사이드바 메뉴 반영. ⚠️ 클릭 렌더는 육안 권장. **미커밋**.
- ⚠️ 검증데이터: 고객 신규상사(개명)(CUST-2026-0004, 한도0)·꼼수상사 등 + CLR-20260610-002(PENDING).

### ➕ 여신 요청 중복 방지 + 실무 UI (2026-06-10, hwlee님 요청 "중복 신청 막고 검토 중이면 버튼 대신 안내")

- **서버 이중 방어**: `CreditLimitRequestService.create`가 같은 고객에 PENDING 요청이 이미 있으면 거부(IllegalState "이미 검토 중인 여신 상향 요청이 있습니다…"). `Repository.existsByCustomerIdAndStatus`/`findFirstByCustomerIdAndStatusOrderByIdDesc`. 조회 API `GET /api/credit-limit-requests/pending?customerId=`(있으면 200+본문, 없으면 204).
- **실무 UI(3곳)**: ① **고객 상세**: 검토 중이면 "여신 상향 요청" 버튼을 **"여신 검토 중 · CLR-xxx" 배지**(노랑, 요청 목록 링크)로 대체 ② **여신 화면 딥링크 진입**(?customerId=): 검토 중이면 모달 대신 flash 안내 + 해당 요청 상세 모달 ③ **여신 요청 모달**: 고객 선택 시 PENDING 있으면 인라인 경고 + **제출 버튼 비활성화**.
- **검증(end-to-end)**: 우진테크 중복 요청 시도→거부 / `/pending` 200(있음)·204(없음) 분기 / 재무 결정 후 다시 요청 가능 / 1고객=1 PENDING(1차 CLR-004 PENDING, 2차 거부) / 고객상세·여신화면 마크업·HTTP 200. 기존 기능 추가 전 쌓였던 우진테크 중복 PENDING 2건은 reject로 정리(현재 우진테크 CLR-004 PENDING 1건 — 화면 "검토 중" 배지 데모용). ⚠️ 클릭 렌더 육안 권장. **미커밋**.

### 🔧 학습 중 발견한 SD 갭 2건 수정 (2026-06-09, hwlee님이 화면 돌려보다 발견)

- **① 견적 1회 소비**: 한 견적으로 수주 만들면 그 견적 재사용 불가(분할 수주는 견적 없이). `QuotationStatus.CONVERTED` 추가, `Quotation.markConverted()`(ACCEPTED→CONVERTED, 중복 시 거부)/`revertConversion()`(수주 취소 시 복원). `SalesOrderService.create`가 견적 있으면 markConverted 호출, `cancel`이 revertConversion. 화면: 견적 list/detail STATUS에 CONVERTED('수주전환') 추가 → form의 ACCEPTED-only 드롭다운에서 소비된 견적 자동 제외. (enum 문자열 저장이라 마이그레이션 불필요.)
- **② 신용한도 수주 화면 노출**: `CreditStatusResponse`(limit/used/remaining) + `SalesOrderService.creditStatus` + `GET /api/sales-orders/credit-status?customerId=`. 수주 detail(확정 전 DRAFT면 "확정 시 남은 한도" 예고+초과 경고)·form(고객/견적 선택 시 표시, 라인 합계 반영 "이 주문 포함 시 남은 한도").
- **검증**: `compileJava` 그린. ⚠️ 앱 재기동+강력새로고침으로 실제 렌더는 hwlee님 육안 확인 필요(헤드리스 없음). **미커밋**.

### 📚 학습 페이즈 진행 (2026-06-09 시작)

- **운영 방식**(메모리 [[feedback-dev-first-study-later]]): 기존 doc/ → **`doc_bak/`** 백업, 새 **`doc/`** 에 작성. **채팅(터미널) + 문서(.md) 둘 다** 작성. 주제마다 **① 도메인(업무) 설명 → ② 구현(개발) 설명** 2단계. ②는 코드 조각을 글에 인용해 자립적으로.
- **완료한 글**:
  - `doc/00-전체-조감도.md` — 전체 숲(ERP 5모듈·OTC/제조 흐름·ERP↔MES 동기REST/비동기Kafka). 끝에 01~16 주제 목록표.
  - `doc/04-모듈연계/1-도메인.md` — 한 거래가 여러 모듈 자동 갱신(출하→재고·매출원가 등), 원자성+느슨한결합.
  - `doc/04-모듈연계/2-구현.md` — Spring Events: `DeliveryShippedEvent` 발행 → MM/FI `@TransactionalEventListener(BEFORE_COMMIT)` 구독, `@Order(10/20)` 순서, REQUIRED 합류로 롤백.
  - `doc/05-FI-회계/1-도메인.md` — 복식부기(차변=대변): 회계등식(자산=부채+자본+수익·비용), 차/대 정상방향 5계정, 황금률, 실제 분개 3예(발행/출하/입고, 실제 계정코드 1200·4100·2200·5100·1400·2100), 자동분개 개념, 전표 작성→전기(POSTED).
  - `doc/05-FI-회계/2-구현.md` — JournalEntry(헤더)+JournalLine(한 라인 한 방향, 생성자+DB CHECK)·`post()`가 차대검증(compareTo, UnbalancedJournalException)·`AutoJournalService` 8개 사건별 메서드(addDebit/addCredit)·04 리스너(`fi/integration/sd` BEFORE_COMMIT→createSalesEntry, 롤백)·매출원가 @Order(10→20)로 출고단가 전달·급여분개가 수학적으로 균형(net=gross−tax−ins).
  - `doc/01-마스터데이터/1-도메인.md` — 마스터(명사) vs 트랜잭션(동사), 단일 정의·모든 거래가 참조, 우리 마스터 6종(고객·거래처·품목·창고·부서·직원, master 패키지), 고객/거래처=돈 방향(매출1200/매입2100), 공통 성질(식별코드·거래 분기 속성[품목유형·신용한도]·삭제 대신 비활성).
  - `doc/01-마스터데이터/2-구현.md` — BaseEntityWithCode(code unique·updatable=false + status MasterStatus + deletedAt)·CodeGenerator(REQUIRES_NEW+SELECT FOR UPDATE, 마스터 연4자리/트랜잭션 일3자리)·Customer(creditLimit→02 수주, businessNo 수정불가)·Item(itemType→05 분기)·static create 팩토리 검증·CRUD(중복 이중방지·DTO·Specification 페이징)·**Soft Delete**(@SQLDelete→deleted_at UPDATE + @SQLRestriction 조회 자동제외) vs status 구분·Auditable.
  - `doc/02-SD-영업/1-도메인.md` — OTC(견적Quotation→수주SalesOrder→출하Delivery→세금계산서Invoice→입금), 4단계로 쪼개는 이유(되돌릴 수 없는 선), 각 상태머신(견적 DRAFT→SENT→ACCEPTED/EXPIRED, 수주 DRAFT→CONFIRMED→SHIPPING↔SHIPPED→INVOICING↔INVOICED→CLOSED, 출하/계산서 DRAFT→SHIPPED/ISSUED), **신용한도**(수주 확정 시점 미수금+주문액>creditLimit 거부), SD가 04·05의 출발점.
  - `doc/02-SD-영업/2-구현.md` — 상태 전이 메서드 = 입구 가드(if status!=… throw, 엔티티 메서드로만 변경)·**견적 1회 소비**(CONVERTED + markConverted/revertConversion, create↔cancel 대칭)·**수주 라인이 진실의 원천**(shippedQty/invoicedQty 누적→recomputeStatus 헤더 재도출, 부분출하·취소에도 불일치 불가)·**CreditLimitChecker**(외부조회[다른 수주 합계] 필요→엔티티 아닌 서비스 컴포넌트, confirm 한곳·excludeOrderId 자기제외, race는 03 보류)·creditStatus(화면 동일산식 예고)·**이벤트 발행**(ship→DeliveryShippedEvent[MM 재고차감], issue[VAT10%]→InvoiceIssuedEvent[FI 매출분개], BEFORE_COMMIT라 실패시 원천 롤백).
- **▶ 다음 후보**: **03 MM ①도메인 → ②구현**(DeliveryShippedEvent 수신 재고차감, 동시성/락) → 06~16.
- ⚠️ (정정 2026-06-09) 01·02·03(마스터/SD/MM)도 **순서대로 학습**하기로 함. 04·05 먼저 한 뒤 01부터 채우는 중.
- 참고: 예전 학습 문서·통합 아키텍처(`doc/10`,`doc/11` 등)는 **`doc_bak/`** 으로 이동됨.

### Phase 16 구현 요약 (2026-06-09) — 통합·운영

- **정합성 검증**(`pp.integration.mes`): `MesClient.fetchWorkOrders`(GET, CB) + `ReconciliationService`(dispatched 생산지시 ↔ MES 작업지시 대조: MISSING_IN_MES / QTY_MISMATCH) + `GET /api/integration/reconciliation`(@PreAuthorize PRODUCTION/ADMIN). `ProductionOrderRepository.findByMesWorkOrderNoIsNotNull`.
- **통합 문서**: `doc/11-phase-16-통합-운영/1-통합-아키텍처와-E2E.md`(최종 아키텍처 다이어그램·E2E 8단계·운영 3대장치[회복성/정합성/추적]). 루트 `README.md` 전면 갱신(두 시스템·실행법·포트·완료 현황).
- **검증(E2E)**: 정합성 정상 시 `consistent:true`(2건), ERP 수량 변조 주입 시 `QTY_MISMATCH`(ERP=4/MES=3) 탐지→원복. (Phase 12~15 단계별 흐름은 각 Phase에서 이미 E2E 검증.)
- ⚠️ 백그라운드 기동: ERP(8080)·MES(8082)·docker. **전체 미커밋** — 커밋·푸시는 hwlee님이 직접.

### Phase 15 구현 요약 (2026-06-09) — 품질 + 설비

- **품질**(`com.hwlee.mes.quality`): `DefectReason` 마스터(V5, DEF-01~03 시드) + `QualityInspection`(검사/합격/불량/사유/판정 PASS·FAIL). `QualityService.inspect`: 불량 시 사유 필수 + **Outbox 에 QUALITY_DEFECT 이벤트 적재**. API `GET /api/defect-reasons`, `POST|GET /api/work-orders/{id}/inspections`.
- **불량 통보(Kafka 2번째 흐름)**: `integration.event.QualityDefectEvent`. **OutboxPublisher 를 eventType→토픽 매핑으로 일반화**(PRODUCTION_PERFORMANCE→mes.production.performance, QUALITY_DEFECT→**mes.quality.defect**). ERP `pp.integration.mes`: `QualityDefectListener`(@KafkaListener) → `quality_defect_log`(V41, event_id UNIQUE 멱등) **기록만**(재고/회계 영향 없음 — 품질 상세는 MES 소유, ERP엔 요약만).
- **설비 상태**(`master.equipment`): `EquipmentStatus`(RUNNING/IDLE/DOWN/MAINTENANCE) + Equipment.status + `EquipmentStatusLog`(구간 이력, V6). `EquipmentStatusService.changeStatus`(진행구간 닫고 새 구간) + `utilization`(RUNNING 시간/전체 = 가동률, OEE 가용성 미니). API `POST /api/equipments/{id}/status`, `GET /api/equipments/{id}/utilization`.
- **계약**: `contracts/events/mes-quality-defect.json`.
- **검증(E2E)**: 품질검사(검사3·합격2·불량1·DEF-02)→FAIL→Outbox QI-1→Kafka→ERP `quality_defect_log` 기록 확인. 설비1 RUNNING(3s)→IDLE(2s)→RUNNING → 가동률 **60.0%**(running3/total5) 정확.
- ⚠️ **검증 데이터**: MES QI-1(불량1)·설비1 상태이력3. ERP quality_defect_log QI-1.
- ⚠️ 백그라운드 기동: ERP(8080)·MES(8082)·docker. 미커밋.

### Phase 14 구현 요약 (2026-06-09) — Outbox + Kafka ⭐

- **MES(Producer)**: **Transactional Outbox** — `integration.outbox`(OutboxEvent/Status/Repository, V4 outbox_event 테이블). `PerformanceService.report()`가 실적 저장과 **같은 트랜잭션**에서 이벤트 JSON을 outbox에 적재(원자성). `integration.event.ProductionPerformanceEvent`(eventId=workOrderNo#seq, erpOrderNo, goodQty/defectQty, consumptions[], reportedAt). `OutboxPublisher`(@Scheduled fixedDelay 2s, @EnableScheduling): PENDING 폴링→`KafkaTemplate.send(topic).get()`(브로커 ACK 확인)→SENT. 토픽 `mes.production.performance`.
- **ERP(Consumer)**: `pp.integration.mes` — `ProductionPerformanceListener`(@KafkaListener, group erp, ObjectMapper 역직렬화)→`ProductionPerformanceHandler`(@Transactional). **멱등**: `processed_event`(V40, event_id UNIQUE) existsByEventId→중복 skip. 처리: `findByNumber`로 PO 조회→부품 `stock.issue`(비관락)+StockMovement(PRODUCTION_OUT)+원가합→완제품 `stock.receive`+StockMovement(PRODUCTION_IN)+`AutoJournalService.createProductionEntry`(차)제품/대)원재료). Phase 8 생산완료 로직 재사용.
- **계약**: `contracts/events/mes-production-performance.json`(JSON Schema).
- **검증(E2E)**: MES WO-002 start→실적(양품3)→outbox PENDING→Publisher 발행(SENT)→ERP 수신. ERP 노트북 50→**53**(+3), 부품 BOM 비례 차감(메모리 -6 등), 생산분개 **차)1400 / 대)1410 = 2,040,000**(자재원가 합 정확), processed_event 1건. **멱등**: outbox를 PENDING으로 되돌려 재발행→ERP "이미 처리됨" skip→재고·전표 불변.
- ⚠️ **검증 데이터**: MES WO-20260609-002(IN_PROGRESS, 실적1·outbox1 SENT). ERP 노트북+3·부품 일부 차감·PROD 전표 JE-20260609-001·processed_event 1.
- ⚠️ 백그라운드 기동: ERP(8080)·MES(8082)·docker. 미커밋.

### Phase 13 화면 추가 (2026-06-09) — MES 첫 Thymeleaf 화면

- MES에 **thymeleaf 의존성** + 단건 조회 `GET /api/work-orders/{id}`(WorkOrderService.findById).
- **화면 토대**: `templates/fragments/layout.html`(MES 셸 — 데코레이터 document(title,content), 상단바, 인증 없음), `static/css/mes.css`, `static/js/mes.js`(MES.api/num/esc/badge/statusColor/flash).
- **화면 2종** + `web/MesViewController`(`/`→`/work-orders`, `/work-orders`, `/work-orders/{id}`):
  - `workorder/list.html`: 작업지시 목록(진행률 프로그레스바·상태 뱃지).
  - `workorder/detail.html`: 정보 + **상태별 액션**(RECEIVED=설비·작업자 선택+시작 / IN_PROGRESS=일시정지·완료+실적등록폼 / PAUSED=재개) + 실적 이력·자재투입 표. JS는 `th:inline`로 id 주입, /api/* 호출.
- **검증**: MES 재기동, `/`·`/work-orders`·`/work-orders/{id}` HTTP 200·페이지 스크립트 fragment 내 포함·브랜드 노출. 새 작업지시 WO-20260609-002(RECEIVED) 생성해 목록 표시 확인.
- ⚠️ 헤드리스 브라우저 없어 실제 클릭 렌더는 사용자 육안 권장(API 자체는 Phase 13 백엔드에서 검증됨).

### Phase 13 구현 요약 (2026-06-09) — MES 백엔드

- **작업지시 상태전이 확장**(`workorder`): WorkOrderStatus에 PAUSED 추가(RECEIVED→IN_PROGRESS↔PAUSED→COMPLETED). WorkOrder에 배정설비/작업자(ManyToOne)·producedQty·defectQty·startedAt·finishedAt + 도메인 메서드 start(설비·작업자 배정)/pause/resume/addProduction(누적)/complete.
- **생산 실적**(`performance`): `ProductionResult`(부분 실적 — 작업지시당 여러 건, seq·양품·불량·reportedAt·note) + `MaterialConsumption`(자재 투입). `PerformanceService`: report() 시 **BOM 단위소요(라인소요÷지시수량) × 양품수량**으로 자재 투입 자동 기록 + producedQty 누적. `PerformanceController`(`POST /api/work-orders/{id}/start|pause|resume|complete`, `POST|GET /{id}/results`).
- **Flyway V3**(mes_db): work_order 실행 컬럼 추가 + production_result·material_consumption.
- **검증(end-to-end)**: WO-20260609-001 start(EQ-001/OP-001)→실적1차(양품3)→2차(양품2·불량1)→complete. producedQty=5·defectQty=1, 자재투입 BOM 비례 정확(메모리 양품당 2개 등), 상태전이 정상.
- **미구현**: 실적 입력 **화면**(MES는 아직 화면 0개 — Thymeleaf 의존성·layout 토대부터 필요). STUDY-PLAN의 "현장 작업자 시뮬레이션 화면". ERP로의 실적 전송은 Phase 14(Kafka).
- ⚠️ **검증용 데이터**: MES WO-20260609-001(COMPLETED, 실적2·자재투입10행). ERP PO-20260609-001(RELEASED). 백그라운드 기동: ERP(8080)·MES(8082)·docker.

### Phase 12 구현 요약 (2026-06-09)

- **MES 도메인**(`com.hwlee.mes`): 공통 `BaseEntity`(+`JpaAuditingConfig`, created/updated_at). 마스터 `master.equipment`(Equipment)·`master.operator`(Operator) + 조회 API(`/api/equipments`,`/api/operators`). `workorder`: WorkOrder+WorkOrderLine(상태 RECEIVED→IN_PROGRESS→COMPLETED→CANCELLED, Phase 12는 RECEIVED), **멱등 수신**(`erp_order_no` UNIQUE → 기존 있으면 그대로 반환). 수신 API `POST /api/work-orders`(**201=신규, 200=멱등 중복**)·`GET /api/work-orders`. work_order_no = PO번호 → WO번호 치환. **Flyway V2**(equipment/operator/work_order/work_order_line + 설비2·작업자2 시드).
- **ERP 연계**(`pp.integration.mes`): `MesClient`(Spring **RestClient**, connect 2s/read 3s 타임아웃, **@Retry(max 3)** + **@CircuitBreaker** name=mes, 실패 시 `MesUnavailableException`→503). `MesDispatchService.dispatch(id)`: RELEASED/COMPLETED 생산지시를 MES로 전송(BOM 라인 포함), 응답의 WO번호를 `ProductionOrder.markDispatched`로 저장. `POST /api/production-orders/{id}/dispatch`(ProductionController). ProductionOrder에 `mes_work_order_no`/`mes_dispatched_at` 컬럼(**V39**). application.yml에 `mes.base-url`(8082)·resilience4j(retry·circuitbreaker mes) 설정.
- **계약**: `contracts/openapi/erp-to-mes-workorder.yaml`(OpenAPI 3, 멱등 201/200 명세).
- **검증(end-to-end)**: 양앱 컴파일·기동(MES V2/ERP V39 적용). MES 마스터 조회 OK. 생산지시 생성(노트북5)→release→**dispatch**: ERP `PO-20260609-001`→MES `WO-20260609-001`(RECEIVED), BOM 5라인 정확 전개. **멱등성**: dispatch 3회 호출에도 MES work_order 1건·라인 5건. **Circuit Breaker**: MES 강제종료 시 dispatch 503 빠른실패(0.01~0.03s), MES 복구 후 dispatch 200(회로 닫힘).
- ⚠️ **검증용 데이터**: ERP 생산지시 `PO-20260609-001`(RELEASED, MES전송됨), MES 작업지시 `WO-20260609-001`. 부품 재고는 미차감(완료 안 함). 불필요하면 삭제 가능.
- ⚠️ **포트**: MES는 **8082**(8081은 사용자 ddakplay RN Metro 점유).
- **미구현(선택)**: PP 생산지시 상세 화면에 "MES 전송" 버튼/상태 표시(현재 REST로만). MES 화면 일체 없음(Phase 13 실적 화면에서).
- ⚠️ 현재 백그라운드 기동: ERP(8080), MES(8082), docker 인프라.

### Phase 11 구현 요약 (2026-06-09) — Part 2 시작

- **새 프로젝트 `hwlee-mes`** (모노레포 디렉토리, 독립 Gradle 프로젝트, `rootProject.name=mes`). gradle wrapper는 hwlee-erp에서 복사(8.14.4). Spring Boot 3.5.14 / Java 21. 패키지 `com.hwlee.mes`.
  - 의존성: web, data-jpa, validation, actuator, spring-kafka, micrometer-tracing-bridge-brave + zipkin-reporter-brave, flyway, mysql, springdoc, lombok.
  - `MesApplication` + `application.yml`(**포트 8082** — ⚠️ 8081은 사용자 다른 프로젝트 ddakplay RN Metro가 점유 → 8082로 변경), mes_db(3308), kafka(9092), zipkin(9411), tracing sampling 1.0. `PingController`(GET /api/ping). Flyway `V1__init.sql`(베이스라인, MES 도메인 테이블은 Phase 12+).
- **docker-compose 확장**: 기존 `mysql`(erp_db 3307)에 더해 **`mes-mysql`(mes_db 3308)**, **`zookeeper`(2181)**, **`kafka`(confluentinc cp-kafka 7.6.1, 9092)**, **`zipkin`(openzipkin/zipkin:3, 9411)** 추가. 볼륨 분리.
- **ERP 연계 의존성/설정**: hwlee-erp build에 actuator·spring-kafka·resilience4j-spring-boot3·micrometer-tracing-brave·zipkin-reporter 추가. application.yml에 kafka(group erp)·management.tracing/zipkin 추가. `SecurityConfig` PUBLIC_PATHS에 `/actuator/health`,`/actuator/info` 공개. ⚠️ **실제 RestClient bean·Circuit Breaker 인스턴스·Kafka 토픽/리스너는 미구현(Phase 12·14에서 실호출과 함께)** — Phase 11은 의존성/통로 토대까지.
- **`contracts/`** 골격: `openapi/`, `events/`(.gitkeep) + `contracts/README.md`(계약 관리 규칙). 실제 스펙은 Phase 12·14·15.
- **검증(end-to-end)**: docker 5개 컨테이너 healthy. 두 앱 컴파일 그린·기동. MES `/api/ping` UP·`/actuator/health` UP(mes_db 연결)·flyway V1 적용. ERP 재기동 후 `/actuator/health` 인증없이 200 UP. Zipkin UP.
- ⚠️ 현재 백그라운드 기동 상태: ERP(8080), MES(8082). docker compose 인프라 가동 중.

### Phase 10 구현 요약 (2026-06-09)

- **리포트 3종**(JPQL/자바 집계, QueryDSL 미도입 — 기존 Specification 일관성·집계 단순성 이유). 패키지 `com.hwlee.erp.report`(+dto, +web).
  - **매출 리포트**(`GET /api/reports/sales?from&to&unit=DAY|MONTH`): 기간 ISSUED 인보이스를 **서비스에서 자바 그룹핑**(일=yyyy-MM-dd, 월=yyyy-MM) + 합계행. ⚠️ 최초 `function('date_format',...)` JPQL이 Hibernate 생성자 매핑 실패("Missing constructor") → 자바 그룹핑으로 전환.
  - **재고 현황 리포트**(`GET /api/reports/inventory?itemId&warehouseId`): 보유>0 (품목,창고)별 평가액(`StockRepository.inventoryReport` JPQL 생성자표현식) + 총평가액.
  - **손익계산서 미니**(`GET /api/reports/income-statement?from&to`): POSTED 전표의 수익/비용 계정 집계(`JournalEntryRepository.incomeStatementSums`) → 매출/매출원가/매출총이익/판관비/영업이익/당기순이익 + 계정 명세. `AccountAmount.normalAmount`로 부호 정리(수익=대−차, 비용=차−대).
  - 컨트롤러 `ReportController`(@PreAuthorize FINANCE/ADMIN).
- **화면 3종**(`templates/report/sales|inventory|income-statement.html`) + `ReportViewController`(/reports/*) + **사이드바 "리포트" 그룹**(layout.html, FINANCE/ADMIN). **대시보드 통계 카드 실연동**(dashboard.html: 이번달매출/당기순이익/재고평가액/재고품목수 — 리포트 API 호출, 권한 없으면 조용히 '—' 유지).
- 조회 메서드 보강: `InvoiceRepository.findIssuedBetween`.
- **검증(end-to-end)**: 컴파일 그린·앱 기동. 매출 2026-05 월별=2건·13,200,000 / 재고 6품목·365,000,000 / 손익(검증 전표 2건 생성 후)=매출100만·매출원가60만·당기순이익40만·명세 정확. 4개 화면(+대시보드) HTTP 200·페이지 스크립트 포함·사이드바 리포트 메뉴 노출.
- ⚠️ **검증용 테스트 데이터 추가**: 손익 검증 위해 수동 전표 2건(`[검증]매출 인식` 차)1200/대)4100 100만, `[검증]매출원가` 차)5100/대)1400 60만, 2026-06-01, POSTED) 생성됨. 불필요하면 삭제 가능.
- ⚠️ 앱은 현재 백그라운드 기동 상태(8080, MySQL 3307).

### Phase 9 구현 요약 (2026-06-09)

- **Spring Batch 5 정식 도입**(`spring-boot-starter-batch`). 메타테이블은 자동초기화 끄고(`spring.batch.jdbc.initialize-schema=never`, `spring.batch.job.enabled=false`) **Flyway V37**로 직접 생성(BATCH_* 9개). `@EnableScheduling`(메인 클래스).
- **배치 결과 테이블 V38**: `daily_sales_closing`(일자 UNIQUE), `inventory_valuation`(평가일+품목+창고 UNIQUE), `ar_aging`(노령화일+고객 UNIQUE). 엔티티/리포지토리 `com.hwlee.erp.batch.closing`.
- **잡 2개** `com.hwlee.erp.batch.job`:
  - **일일 매출 마감**(`dailySalesClosingJob`): 단일 Tasklet. 기준일 ISSUED 인보이스 합산 → 1행 저장. 삭제후재삽입으로 멱등.
  - **월말 결산**(`monthEndClosingJob`): 3스텝 = purge(Tasklet, 두 스냅샷 삭제) → 재고평가(**chunk** JpaPagingItemReader<Stock>+processor+JpaItemWriter, 보유0 스킵) → 채권노령화(Tasklet, **FIFO** 입금 차감 후 경과일 버킷팅 `ArAgingBuckets`).
- **멱등/재실행**: JobParameters = 기준일(식별) + `run.id`=epochMillis(매 실행 고유 → "이미 완료" 예외 없이 같은 날짜 재집계). 결과 멱등성은 스냅샷 "삭제 후 재삽입"으로 보장.
- **트리거** `com.hwlee.erp.batch.run`: `ClosingBatchService`(JobLauncher 동기) + REST `ClosingBatchController`(`POST /api/batch/daily-sales-closing?date=`, `/month-end-closing?date=`, `@PreAuthorize FINANCE/ADMIN`, date 생략 시 어제/전월말일) + `ClosingBatchScheduler`(`@Scheduled` 매일 02:30 일일, 매월1일 03:00 월말).
- 조회 메서드 보강: `InvoiceRepository.findIssuedByInvoiceDate/findIssuedUpToWithCustomer`, `PaymentRepository.findPostedReceiptsUpTo`.
- **검증(end-to-end, 컴파일 그린·앱 기동)**: V37/V38 적용 성공, 테이블 12개 생성. admin 로그인 후 배치 실행 둘 다 `COMPLETED`. ① 일일마감 2026-05-27 = 2건·13,200,000원(created_by=admin@hyunwoo.com) ② 재고평가 2026-05-31 = 6행·합계 365,000,000 ③ 채권노령화 = 고객1 0~30일 버킷 13,200,000(입금0·경과4일). **멱등 재실행** 후에도 행수 동일(1/6/1, 중복 없음), BATCH_JOB_INSTANCE 4건.
- ⚠️ **검증용 테스트 데이터 생성됨**: daily_sales_closing 1·inventory_valuation 6·ar_aging 1행 + BATCH_* 메타. 불필요하면 삭제 가능.
- **미구현(선택)**: 배치 결과 조회 REST/화면, 사이드바 "마감(배치)" 그룹. (현재는 DB/실행 응답으로만 확인.)
- ⚠️ 앱 검증 위해 기존 실행 앱을 종료 후 재기동했음(현재 백그라운드 기동 상태, MySQL 3307).

### (보류된 학습) Phase 8 PP — 구현 완료·검증

- **Phase**: 8 — PP(생산) BOM/생산지시. **단계 5(구현) 완료·검증.** 단계 6(워크스루)·7(시연)은 학습 페이즈로 보류.
- **단계**: 1~5 완료. 문서 `doc/09-phase-8-PP-생산/`(1-도메인-브리핑, 2-설계-제안).
  - **단계 4 승인 결과**: ①FI 전표 = **현실형 간이 포함**(원재료/제품 계정 분리) ②화면 = **포함**.
  - **단계 5 구현 요약**:
    - 마스터: `Item.itemType`(FINISHED/COMPONENT, 기본 FINISHED) + `ItemType` enum + `ItemCategory.PART`. ItemResponse에 itemType 노출.
    - PP 도메인: `pp/bom`(Bom+CRUD), `pp/order`(ProductionOrder↔Line, 상태머신 PLANNED→RELEASED→COMPLETED→CANCELLED, ProductionService). 생성 시 BOM×수량 전개(MRP), 완료 시 **부품 stock.issue + 완제품 stock.receive 직접 오케스트레이션**(GoodsReceipt/GoodsIssue 헤더 미사용 — vendor 강제·매입전표 오트리거 회피). 완제품 단가=투입 부품 실제원가 합÷수량(원가보존). `MovementReason.PRODUCTION_IN/OUT`, refType="PROD"(ref_type VARCHAR(10) 제약).
    - **회계(현실형 간이)**: `재고자산(1400)`→의미 '제품', **원재료(1410)·자본(3000)·이익잉여금(3100)** 신설. `AutoJournalService.createPurchaseEntry`가 itemType별 분기(부품→원재료, 완제품→제품), 신규 `createProductionEntry`(차)제품/대)원재료=직접재료비). COGS/매출 리스너 무변경. `JournalSource.PROD`. **재공품(WIP)·노무비·간접비는 범위 밖**(완제품원가=직접재료비만).
    - 인가: `PRODUCTION` 역할 신설, 생산팀(park@hyunwoo.com) 매핑 = **Phase 6 park 복선 회수**. ItemController/WarehouseController 읽기에 PRODUCTION 추가.
    - 화면: `pp/web/PpViewController` + 4화면(BOM 관리, 생산지시 목록/상세/생성) + 사이드바 **생산(PP) 그룹**(생산지시/BOM).
    - 마이그레이션 **V33**(스키마: item.item_type/bom/production_order/_line)·**V34**(계정)·**V35**(부품5+노트북BOM+기초재고500+개시분개 차)원재료/대)이익잉여금 3.15억)·**V36**(PRODUCTION 역할+park). ⚠️ V33 최초 `ALTER ... AFTER ... COMMENT` 순서 오류로 1회 실패 → COMMENT를 AFTER 앞으로 수정 + flyway_schema_history 실패행 삭제 후 재적용.
    - **검증(end-to-end, 컴파일 그린)**: park(PRODUCTION) 로그인→PP 4화면 200. 노트북×10 생산지시 생성(MRP: 메모리20 등)→착수→완료: 부품 정확 차감(메모리480 등)·완제품+10·생산전표 `JE-...005 차)제품6,800,000/대)원재료6,800,000`·출고단가 기록. 가용성 API 정상. 통합테스트 `ProductionScenarioTest`(2케이스, Docker 필요라 이 PC 미실행·컴파일 그린).
    - ⚠️ 검증용 테스트 데이터: 생산지시 PO-20260604-001(완료)+1건(계획), 부품 재고 일부 소모.
- **(이전) 화면 트랙 ✅ 전체 완료**: 프론트엔드 백필 SD·MM·FI·HR. (남은 다듬기 선택: 대시보드 통계 연동.)
- **⚠️ 리브랜딩(2026-06-04)**: 회사명 현우→**hyunwoo**, 이메일 도메인 **@hyunwoo.com**. **로그인 계정이 바뀜** — 시연 계정: `admin@hyunwoo.com` / `kim@hyunwoo.com`(SALES) / `lee@hyunwoo.com`(FINANCE) / `park@hyunwoo.com`(역할없음) / `jung@hyunwoo.com`(HR), 비번 공통 `pass1234`. 브랜드 표기 `HYUNWOO ERP`. 적용 방식 = 전진 마이그레이션 **V32**(기존 V8/V28/V31 시드는 Flyway 체크섬 보존 위해 미수정; V32가 employee/app_user/department/item 일괄 UPDATE). 테스트 5종 이메일 도메인도 동기화. (구 이메일 `@hwlee-erp.example`은 더 이상 로그인 불가.)
- **마지막 갱신**: 2026-06-04 (🛑 **오늘 세션 종료**). 오늘 한 일: ① 전 화면 공통 JS 누락 버그 수정 ② FI(회계) 화면 9종 ③ HR(인사/급여) 화면 5종 ④ 리브랜딩(현우→hyunwoo, @hyunwoo.com, V32) ⑤ **Phase 8(PP 생산) 단계 1~5 완료**(BOM·생산지시·MRP·현실형 회계·화면, V33~V36, end-to-end 검증).
- **▶ 다음 세션 시작점**: **Phase 8 단계 6(코드 워크스루) / 7(시연·회고)** 부터. (원하면 건너뛰고 Phase 9 배치로 가도 됨.) 앱 재기동 필요 시 `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun`(MySQL 3307). 로그인 `admin@hyunwoo.com`/`park@hyunwoo.com` 등 `pass1234`. **커밋·푸시는 hwlee님이 직접** — 오늘 변경분(미커밋) 푸시해야 다른 PC에서 이어감.

---

## ✅ 해결됨 — 전 화면 페이지 `<script>`가 렌더에서 누락 (2026-06-04 수정·검증 완료)

- **수정 내역**(2026-06-04): SD 12 + MM 11 = **23개 템플릿** 전부에서 `<script>` 바로 앞의 조기 종료 `</div>` 한 줄을 제거 → 스크립트가 `th:fragment="content"` 안에 포함됨. div 균형 23/23 정상.
- **렌더 검증 완료**(HTTP 200 너머): 앱 재기동(local, MySQL 3307) 후 admin 로그인하여 `/sd/quotations`·`/mm/stocks`·`/mm/goods-receipts` 응답에 `<script>`(3개)·`function load`·`addEventListener`가 **실제로 포함**됨을 grep 확인(수정 전엔 0건). REST API(`/api/quotations` 2건, `/api/stocks` 2건, `/api/customers` 2건)도 200+데이터 응답 → 표 렌더 흐름 정상.
- ⚠️ **Thymeleaf 캐시 기본값(cache=true)** 이라, 템플릿을 고친 뒤엔 반드시 **앱 재기동** 후 브라우저 **강력 새로고침(⌘+Shift+R)** 으로 확인할 것.
- 아래는 당시 진단 기록(참고용 보존):
- **증상**(사용자가 브라우저에서 확인): 목록이 "불러오는 중…"에서 안 바뀜(=`load()` 미실행), 검색 누르면 선택한 날짜 등 입력값이 사라짐(=JS 핸들러 미부착 → 네이티브 폼 전송으로 페이지 새로고침). 입고 화면에서 첫 확인, **SD도 동일**.
- **근본 원인**(렌더된 실제 HTML로 확정): 각 템플릿의 페이지 `<script>`가 `th:fragment="content"` div **바깥(형제 위치)** 에 있음. 레이아웃이 `~{::content}`로 그 fragment만 가져오므로 **스크립트가 최종 페이지에서 통째로 누락**됨. `curl`로 `/sd/sales-orders` 받아 `grep "searchForm').addEventListener"` → **0건**(스크립트 부재) 확인.
- **범위**: 레이아웃 데코레이터를 쓰는 **모든 페이지** — SD 14 + MM 11 + 대시보드/관리자 등. **git HEAD부터 있던 선재(先在) 버그**(MM 신규 작성분이 SD 패턴을 그대로 복사해 전파). 기존/금번 "검증"이 **HTTP 200(껍데기)만 보고 실제 브라우저 렌더를 안 봐서** 못 잡음.
- **수정 방법**(기계적, div 균형 유지): 본문 끝의 fragment-닫는 `</div>` 를 `<script>…</script>` **뒤로** 이동 = 스크립트를 fragment 안에 포함.
  ```
  현재(버그):                         수정 후:
  <div th:fragment="content">         <div th:fragment="content">
    …본문…                              …본문…
  </div>   ← 조기 종료                  <script>…</script>
  <script>…</script>                  </div>   ← 여기서 종료
  </div>                              </body>
  ```
  - 패턴: 각 파일에서 `</nav>`(목록) 또는 본문 끝 `</div>` 직후·`<script` 직전에 있는 **단독 `</div>` 한 줄을 제거**하면, `</script>` 뒤의 기존 `</div>`가 fragment를 닫게 됨. (여는 div 1 = 닫는 div 1 제거라 균형 유지)
- **다음 세션 시작점**: ① 위 수정을 전 템플릿 일괄 적용 → ② 앱 기동(`SPRING_PROFILES_ACTIVE=local ./gradlew bootRun`, MySQL 3307) → ③ **실제 렌더 검증**: `curl`로 페이지 받아 페이지 스크립트(`searchForm`/`load`)가 응답에 포함되는지 grep + 가능하면 헤드리스 브라우저(없으면 사용자 육안)로 **표에 행이 칠해지는지**까지 확인(HTTP 200만으론 불충분 — 이번 교훈).
- **참고**: 디자인 변경(필터바/카드 폼/라벨/날짜)·MM 화면·상태머신 로직 자체는 정상. **오직 스크립트 위치만 문제**. 데이터 유무 오해도 이 버그 때문(재고 1·입고 1·창고 1·출고 0건은 실제 DB 사실이나, 화면엔 JS 미실행으로 아무것도 안 칠해졌던 것).
- **⭐ 프론트엔드(화면) 백필 진행 중** (사용자 요청, Phase 횡단 작업): "기능을 화면으로 보며 이해"하기 위해 모듈별 풀 CRUD 화면을 **SD→MM→FI→HR** 순으로 소급 구현 중.
  - 공통 토대: `templates/fragments/layout.html`(**데코레이터 셸** `document(title,active,content)` — 좌측 사이드바+상단바+콘텐츠 슬롯), `static/css/app.css`(ERP 어드민 테마), `static/js/erp.js`(REST 호출 래퍼·401 리다이렉트·금액/날짜 포맷·flash). 방식 = MVC 컨트롤러가 껍데기 HTML 서빙 + JS 가 기존 REST API 호출(JWT HttpOnly 쿠키 자동 인증). Bootstrap 5.3 + bootstrap-icons CDN.
  - **UI 셸 개편(포트폴리오 지향)**: 로그인→**대시보드**(`/`=dashboard.html, 통계 플레이스홀더+바로가기) 진입. 상단 네비→**좌측 사이드바**(역할별 메뉴 그룹, 아이콘). 로그인 화면 리디자인. 기존 menu.html 삭제. 전 화면(SD 12 + admin 2 + 대시보드)을 셸로 통일.
  - **버그 수정**: admin 사용자/역할 화면이 OSIV=false 환경에서 뷰 렌더 시 `LazyInitializationException` → `AppUserAdminService` 조회 메서드가 트랜잭션 안에서 연관(employee/roles/permissions) 초기화하도록 수정.
  - **SD(영업) 완료** ✅: `sd/web/SalesViewController` + 14개 화면(견적·수주 각 목록/상세/생성·수정, 출하·인보이스 각 목록/상세/생성). menu.html SD 카드→실제 화면 연결. OTC 전체 흐름을 로컬 앱(MySQL 3307)에서 end-to-end 검증: 견적→발송→승인→수주(견적링크)→확정(신용한도)→출하(재고차감)→인보이스(VAT10%·매출 자동분개)→수주 INVOICED. 14개 화면 전부 HTTP 200·Thymeleaf 오류 0.
  - **다음 세션 시작점(화면 트랙)**: **MM(자재) 화면**부터 이어서. 이미 만든 셸/패턴을 그대로 재사용한다:
    1. `mm/web/` 에 `@Controller`(@PreAuthorize `PURCHASING`/`ADMIN`) 만들고 `/mm/...` 라우트가 껍데기 템플릿 반환.
    2. 템플릿은 데코레이터 사용: `<html th:replace="~{fragments/layout :: document('제목','<active>', ~{::content})}">` + `<body><div th:fragment="content"> ... </div></body>`. 본문은 `ERP.api()`로 REST 호출(목록=Page 검색, 상세, 생성폼 동적 라인).
    3. `layout.html` 사이드바에 **MM 메뉴 그룹 추가**(재고/입고/출고/창고) + 각 `active` 키. (현재 사이드바엔 영업 SD·관리자만 있음.)
    4. MM REST API: `/api/stocks`(재고현황·이동), `/api/goods-receipts`(입고: vendorId·warehouseId·receiptDate·lines[itemId,quantity,unitCost], 생성후 `/{id}/post` 전기), `/api/goods-issues`(출고), `/api/warehouses`(창고 CRUD). 상태머신/DTO는 각 컨트롤러·dto 패키지 확인 후 진행.
    5. 다 만들면 앱 띄워(`SPRING_PROFILES_ACTIVE=local ./gradlew bootRun`, MySQL 3307 필요) 페이지 HTTP 200·렌더 검증.
  - **화면 백필 순서**: SD ✅ → MM ✅ → 공통 버그 수정 ✅ → FI ✅ → **HR ✅ (전체 완료)**.
  - **✅ HR(인사/급여) 화면 완료**(2026-06-04): `hr/web/HrViewController`(@PreAuthorize HR/ADMIN) + 5개 화면. 사이드바에 **인사(HR) 그룹**(직원/급여대장) 추가. ⚠️ HR API는 **전역 목록이 없어**(계약·근태는 employeeId로만, 급여대장은 /{id}로만) **직원 중심**으로 구성.
    - **백엔드 보강 2건**(화면 동작에 필수): ① `EmployeeController` 읽기 권한에 `HR` 역할 추가(`hasAnyRole('SALES','PURCHASING','FINANCE','HR','ADMIN')`) — HR 사용자가 직원 조회 가능하도록. ② `PayrollController`에 목록 엔드포인트 `GET /api/payroll-runs`(Page) + `PayrollService.search(Pageable)` 추가(기존엔 POST·GET/{id}·confirm·pay만 있었음).
    - **직원(Employee)**: 목록(`/api/employees` List → 클라이언트 필터) + 상세. 상세 한 화면에 **기본정보 + 급여계약(인라인 신규계약/종료) + 이번달 근태(인라인 등록)** 통합. 계약 시급은 서버가 기본급÷계약시간으로 자동 계산.
    - **급여대장(PayrollRun)**: 목록(새 list API)/생성(대상월 YYYY-MM `input[type=month]`만 → 자동 계산)/상세(명세서 표 + 상태머신 DRAFT→확정→지급). 확정=인건비 전표, 지급=지급 전표가 FI 이벤트로 자동 생성.
    - **검증(end-to-end 완료)**: HR 사용자(jung, role HR)로 로그인 → 5개 화면 200·스크립트 포함 + 직원 API 접근(권한 보강 동작) 확인. **급여 흐름 스모크**: `PR-202605-001` 계산(명세 4건, 총지급 1,380만) → 확정(인건비 전표 `JE-...003` 차 1,442만) → 지급(지급 전표 `JE-...004` 차 1,235만), PAYROLL 자동분개 2건 생성 확인. 근태 등록(연장 180분 자동계산)·계약 등록(시급 자동)·계약 종료 모두 201/200. ⚠️ 테스트 데이터 생성됨(급여대장1+전표2, 직원1 근태1건, 직원4 계약1건[종료됨]) — 불필요하면 삭제 가능.
  - **✅ FI(회계) 화면 완료**(2026-06-04): `fi/web/FinanceViewController`(@PreAuthorize FINANCE/ADMIN) + 9개 화면. 사이드바에 **회계(FI) 그룹**(전표/계정과목/입출금) 추가. MM 패턴(filter-bar·table-wrap·카드폼·line-table·form-actions) 그대로 복붙.
    - **계정과목(Account)**: 목록(클라이언트 필터 — `/api/accounts`는 페이징 없이 List 반환)/상세/생성·수정/삭제. 유형 5종(자산/부채/자본/수익/비용)·정상방향·전기가능 표시. 코드·유형은 생성 후 잠금.
    - **전표(JournalEntry)**: 목록(`/api/journal-entries` Page, 출처/상태/전표일 필터)/상세(분개라인+차대합계, POSTED→취소)/생성(수동 전표: 전표일·적요·라인[계정code·차변·대변], 실시간 대차균형 검증). **수동 전표는 생성 즉시 전기(POSTED)** — `createManual`이 내부에서 `post()` 호출. 수정 없음.
    - **입출금(Payment)**: 목록(`/api/payments` Page, 구분/거래일 필터)/상세(읽기전용)/생성(구분 RECEIPT↔DISBURSEMENT 토글로 고객/거래처 선택 전환, 금액·거래일·적요). 생성 즉시 전기, 수정·취소 없음.
    - **검증(end-to-end 완료)**: 앱 재기동 후 admin 로그인 → 9개 화면 HTTP 200·페이지 스크립트 포함 확인 + REST API(계정17·전표3·입출금0) 응답 확인 + **생성 스모크 테스트**: 수동전표 `JE-20260604-001`(차/대 5만 균형, POSTED), 입출금 `PAY-20260604-001`(입금 3만, 고객연결, 자동분개 동반) 둘 다 201 생성됨. ⚠️ 이 2건은 테스트 데이터 — 불필요하면 삭제 가능.
  - **MM(자재) 화면 작성됨**(2026-06-01, ⛔ 위 "미해결 버그"로 현재 화면 미동작 — 코드/구조는 완성): `mm/web/MaterialsViewController`(@PreAuthorize PURCHASING/ADMIN) + 11개 화면. 사이드바에 **자재(MM) 그룹**(재고/입고/출고/창고) 추가. 모두 직전 개선한 새 디자인(인라인 `filter-bar`·`table-wrap`·카드 폼·`line-table`·`form-actions`)으로 구축.
    - **재고(Stock)**: 목록(검색 품목/창고/재고>0) + 상세(재고현황+평가액 / `/api/stock-movements` 이동이력). 조회 전용.
    - **입고(GoodsReceipt)**: 목록/상세/생성·수정. 생성=vendor/warehouse/receiptDate+lines[itemId,qty,unitCost](품목 선택 시 표준원가 자동). 상세 액션: DRAFT→전기(post), POSTED→취소(cancel). 합계 자동계산.
    - **출고(GoodsIssue)**: 목록/상세/생성·수정. 생성=warehouse/issueDate/reason+lines[itemId,qty]. 상태머신 동일. `deliveryId` 있으면 "출하(SD) 자동생성" 표시하고 직접 액션 숨김.
    - **창고(Warehouse)**: 목록(이름/상태 검색)/상세/생성·수정/삭제. code(WH-XXX, 생성 후 잠금)/name/address. status는 DTO에 없어 미편집.
    - 상태 전이 규칙(엔티티 확인): 입고·출고 공통 DRAFT→POSTED(post)→CANCELLED(cancel). **DRAFT는 취소 불가**(cancel은 POSTED만). 화면 버튼도 이에 맞춤.
    - **검증(불충분 — 교훈)**(2026-06-01): `compileJava` 그린. MM 11개 라우트 HTTP 200·REST API 7종 200까지만 확인했고 **실제 브라우저 렌더를 안 봐서 위 공통 JS 누락 버그를 못 잡음**. 다음엔 200이 아니라 응답에 페이지 스크립트가 포함되는지 + 표가 칠해지는지까지 봐야 함.
    - **다음 세션 시작점(화면 트랙)**: **FI(회계) 화면**. `fi/web/`에 @Controller(@PreAuthorize FINANCE/ADMIN) + `/fi/...` 라우트. 사이드바에 **회계(FI) 그룹** 추가(전표/계정/입출금). REST: `/api/journal-entries`(전표), `/api/accounts`(계정과목), `/api/payments`(입출금). MM/SD 화면 코드 복붙 변형이 가장 빠름.
  - **참고(이미 검증된 토대)**: `static/js/erp.js`(api·flash·won·query·param), `static/css/app.css`(테마), `templates/fragments/layout.html`(셸), `templates/sd/**`(목록/상세/폼 예시 12종), `templates/dashboard.html`. SD 화면 코드를 복붙 변형하면 가장 빠름.
  - **Tabler 톤 통일 완료**(2026-05-31): `app.css` 전면 리튠(Inter 폰트·네이비 사이드바·Tabler 블루 #206bc4·크리스프 라운드·소프트 보더/섀도·소프트 뱃지), `erp.js` `ERP.badge` 소프트화. SD 14개(목록 4 `page-head`+`filter-bar`+`table-wrap`, 상세/폼 8 헤더 `page-head`) + `admin/users` 뱃지 + `login` 색/폰트 정렬. 기능 무변경.
  - **✅ 검색 필터바(`filter-bar`) 디자인 개선 완료**(2026-06-01): 사용자 선택 = **"단정한 인라인 바"**. `app.css` `.filter-bar` 를 Bootstrap `row/col` 그리드 → **flex 인라인 바**로 재작성. 공통 패턴: `<form class="filter-bar">` 안에 `.filter-field`(작은 상단 uppercase 라벨 + 컨트롤, 높이 34px 통일, `.ff-wide`/`.ff-narrow`/`.ff-range` 폭 변형), 날짜는 `.ff-range > .date-range`(date 2개를 `~` 로 묶음, `::-webkit-calendar-picker-indicator` 톤 정리), 액션은 `.filter-actions`(우측 정렬, 검색=파랑 채움+🔍, 초기화=아이콘 버튼 ↺). SD 4개 목록(견적/수주/출하/인보이스) 마크업 일괄 교체 + 초기화 버튼/핸들러 없던 3곳(order/delivery/invoice)에 추가. **MM 등 이후 목록은 이 패턴 그대로 복붙**하면 됨.
    - **생성/수정 폼도 동일 톤 적용**(2026-06-01): SD 4개 `form.html`을 **카드 섹션 패턴**으로 — 기본정보 필드를 `.card>.card-body`(`.form-card`)로, 라인 입력 테이블을 `.card>.card-header(섹션명)+.card-body` + `table.line-table`(리스트와 톤 통일, table-bordered 제거)로, 하단 버튼을 `.form-actions`(우측 정렬·상단 구분선, 취소 좌/주버튼 우)로. date 입력 캘린더 아이콘 폴리시는 `.filter-bar` 한정 → **전역**으로 확대. `app.css`에 `.card>.card-header`/`.line-table`/`.form-actions` 추가. JS·id·라인 동작 무변경.
    - **라벨 톤 통일**(2026-06-01): 사용자 피드백 — 목록 필터바(작은 대문자 회색 라벨)와 폼(일반 검정 라벨)이 이질적. 선택 = **"라벨만 통일, 입력은 넓게"**. `.filter-field>label` + `.app-content .form-label`을 **공통 타이포**(.7rem·600·uppercase·muted·자간 .04em)로 묶고, 폼 입력 컨트롤은 표준 크기 유지(입력 편의). 라벨 내 `(선택)` 부가표기는 대문자/강조 제외, 빨간 `*`는 유지. 로그인 화면은 `.app-content` 밖이라 영향 없음.
    - **날짜 입력 크기 통일**(2026-06-01): 사용자 피드백 — 목록(필터바)의 date 입력은 34px, 폼은 38px라 크기 다름. 선택 = **"날짜 입력만 양쪽 통일"**. `.app-content input[type=date].form-control { height:34px; font-size:.82rem }` 로 앱 전체 날짜 입력을 **콤팩트(34px) 기준**으로 통일(필터바 가로 한 줄을 균일하게 유지하려고 키우는 대신 줄이는 방향 선택). 트레이드오프: 폼에서 날짜 칸이 옆 select(38px)보다 약간 낮음. ⚠️ 로컬 앱 렌더 검증(HTTP 200)은 MySQL 3307 필요 — 다음 실행 세션에서 같이 확인 권장(순수 템플릿/CSS/JS 변경이라 빌드 영향 없음).
  - **남은 다듬기(여유될 때)**: 대시보드 통계 카드에 실제 수치 연동(현재 `—` 플레이스홀더).
- **단계 6~7 산출물**: `doc/08-phase-7-HR-인사급여/3-코드-워크스루/`(01~06) + `4-시연-가이드.md`. README 인덱스 갱신 완료.
- **단계 5 구현 요약** (Phase 7):
  - 새 패키지 `hr/contract`(EmploymentContract+Position/ContractStatus), `hr/attendance`(Attendance), `hr/payroll`(PayrollRun/Payslip+PayrollPolicy+상태머신+event)
  - FI 확장: `JournalSource.PAYROLL`, `SystemAccounts` 5상수(5200/5300/2400/2500/2600), `AutoJournalService.createPayrollEntry`+`createSalaryPaymentEntry`, `TransactionNumberGenerator.nextPayrollNumber`(월 키 PR-202605-001)
  - 의존 분리: HR은 FI 미참조. `PayrollConfirmedEvent`/`PayrollPaidEvent` 발행 → `fi/integration/hr/PayrollAccountingListener`(BEFORE_COMMIT)가 분개. 확정=인건비 전표, 지급=미지급급여→현금 (2단계 발생주의)
  - 급여계산: 기본급(만근)+연장수당(시급×1.5×연장h), 소득세6%/4대보험 직원4.5%+회사4.5%, net=gross−소득세−직원분. net 정의상 차대 항상 균형
  - 마이그레이션 V29(4테이블)·V30(계정5+HR역할/권한)·V31(인사팀 정인사 jung@+HR역할+4명 급여계약 시드, 근태는 API로)
  - 인가: 급여/계약/근태 컨트롤러 `@PreAuthorize(hasAnyRole('HR','ADMIN'))`
  - 테스트: `PayrollPolicyTest`(순수 단위, 3개 통과) + `PayrollAccountingIntegrationTest`(근태→계산→확정 전표 차대균형→지급 전표; Testcontainers라 Docker 필요)
  - 빌드: 메인/테스트 전체 컴파일 그린. 통합테스트는 Docker 미설치로 이 PC에서 미실행(기존 FI 통합테스트와 동일 제약)
- **설계 요지**: 4테이블(employment_contract/attendance/payroll_run/payslip), 급여계산(기본급 만근+연장1.5배, 세율 고정), 자동분개 5계정(5200 급여비용/5300 법정복리비/2400 예수금-소득세/2500 예수금-사회보험/2600 미지급급여), PayrollConfirmedEvent→fi/integration/hr/, 마이그레이션 V29~V31, HR 역할 추가
- **단계 2 확정 사항** (실무 지향, AI가 결정·사용자 승인):
  - ① 급여정보 = 별도 **EmploymentContract** 테이블(Employee와 분리, 발효일/연봉 이력)
  - ② 근태/급여 = **시간 기반**(기본급 + 연장수당 = 연장시간×시급×1.5). 근로기준법 풀스펙은 제외
  - ③ 회계 분개 연결 = **이벤트 발행**(PayrollConfirmedEvent, Phase 4 방식)
  - ④ 지급 = **미지급급여 거쳐 2단계**(발생주의: 확정=비용인식, 지급=현금유출)
  - ⑤ 추가 실무 디테일: 예수금을 **소득세분/4대보험분 분리**, **4대보험 회사부담분=법정복리비(회사 추가비용)**
  - ⑥ 세액은 **고정 비율 간이 계산**(예: 소득세 6%, 4대보험 9%) — 정확한 요율표 아님
  - 분개 예: 차)급여비용+법정복리비 / 대)예수금(소득세·4대보험 직원분·회사분)+미지급급여
- **Phase 6 비고**: 7단계 전부 완료(빌드 그린). 커밋은 사용자가 직접.
- **단계 2 확정 사항**: ①로그인주체=별도 AppUser(1:1) ②감사로그=직접 구현(@EntityListeners) ③역할=부서기반 시드+수동 N:M ④화면=로그인+메뉴+사용자/역할 CRUD 일부
- **단계 5 구현 요약**:
  - 새 패키지 `security/`(jwt·auth·user·config·support·admin·web), `audit/`
  - 마이그레이션 V26(인증 5테이블)·V27(audit_log)·V28(역할/권한/사용자 시드)
  - `AuditorAware` 교체(복선 회수): SD/MM/FI/master 코드 0줄 수정으로 created_by=실사용자
  - 인가: 클래스 단위 `@PreAuthorize`(URL prefix 없어 메서드 보안 채택), master는 조회=전역/변경=ADMIN
  - 감사로그: `@EntityListeners`(Customer만, 선택 B) + 콜백서 즉시 JSON 직렬화 → afterCommit 기록
  - 화면: 로그인/메뉴/admin(users,roles) Thymeleaf, JWT는 HttpOnly 쿠키+헤더 병행
  - 테스트: AuthControllerTest/AuthorizationTest/AuditorAwareTest/AuditLogTest + 기존 웹테스트 8개에 @WithMockUser(ADMIN)
  - 시연 계정(pass1234): kim=SALES, lee=FINANCE, park=역할없음(403 시연), admin=ADMIN

> 7단계 사이클: ① 도메인 브리핑 → ② 이해 확인 Q&A → ③ 모델링 제안 → ④ 사용자 승인 → ⑤ 코드 구현 → ⑥ 코드 워크스루 → ⑦ 시연+회고
> (문서 산출물은 `1-도메인-브리핑` / `2-설계-제안` / `3-코드-워크스루/` / `4-시연-가이드` 4개)

---

## Phase 상태표

| Phase | 주제 | 상태 |
| --- | --- | --- |
| 0 | 환경 구축 | ✅ 완료 |
| 1 | 마스터 데이터 | ✅ 완료 |
| 2 | SD(영업) / OTC | ✅ 완료 |
| 3 | MM(재고) / 동시성 | ✅ 완료 |
| 4 | SD↔MM 연계 (이벤트) ⭐ | ✅ 완료 |
| 5 | FI(회계) / 자동 분개 ⭐ | ✅ 완료 |
| 6 | 인증/인가 + 감사 로그 | ✅ 완료 |
| 7 | HR 간이 모듈 | ✅ 완료 |
| 8 | PP(생산) — BOM/생산지시 | ✅ 구현 완료·검증 (학습 보류) |
| 9 | 배치 처리 — 야간 마감 ⭐ | ✅ 구현 완료·검증 (학습 보류) |
| 10 | 리포트와 대시보드 | ✅ 구현 완료·검증 (← ERP 단일 시스템 학습 종료) |
| 11 | MES 프로젝트 셋업 + 연계 인프라 | ✅ 구현 완료·검증 (Part 2 시작) |
| 12 | MES 마스터 + 작업지시 수신 (ERP→MES) | ✅ 구현 완료·검증 |
| 13 | MES 생산 실적 등록 (+ 현장 화면) | ✅ 구현 완료·검증 |
| 14 | MES→ERP 실적 전송 (Kafka) ⭐ | ✅ 구현 완료·검증 (Outbox+멱등) |
| 15 | MES 품질 검사 + 설비 상태 | ✅ 구현 완료·검증 |
| 16 | 통합 시나리오 + 운영 관점 (← 전체 종료) | ✅ 구현 완료·검증 |

> 🎉 **Phase 0~16 전체 구현 완료** (2026-06-09).

> 상세 커리큘럼은 [`ERP-STUDY-PLAN.md`](./ERP-STUDY-PLAN.md), Phase별 산출물 인덱스는 [`doc/README.md`](./doc/README.md).

# 진행 현황판 (PROGRESS)

> **이 파일의 목적**: 여러 PC에서 ERP 학습을 이어갈 때, "어디까지 했고 다음에 뭘 할지"를 **한 화면**으로 복원하기 위한 단일 진행 현황판.
> 대화 기록은 PC 간 공유되지 않으므로, 진행 상태의 **유일한 진실(source of truth)**은 이 파일 + git 저장소다.

## 운영 규칙 (AI가 지킬 것)

1. **"다음 진도 실행" 요청 시** → 저장소 전체를 훑지 말고 **이 파일을 먼저 읽어** 현재 위치를 파악한 뒤, `git log -3` 으로만 가볍게 대조하고 바로 다음 단계로 진행한다.
2. **하나의 진행(7단계 사이클의 한 단계)이 끝날 때마다** → 아래 "현재 위치"와 "Phase 상태표"를 **갱신한다**. (커밋·푸시는 사용자가 직접 한다. 다른 PC에서 이어가려면 사용자가 작업 후 커밋+푸시해야 최신 상태가 공유된다.)
3. **전체 학습이 끝나면**(Phase 16 또는 사용자가 정한 종료 지점 완료) → 사용자에게 **"학습 진행이 모두 끝났으니 이 PROGRESS.md 파일을 삭제하시겠어요?"** 라고 안내한다.

---

## 현재 위치

- **Phase**: 7 — HR 간이 모듈 (인사/급여) ✅ **전체 완료** (7단계 사이클 끝)
- **단계**: 5~7 모두 완료 (구현 + 워크스루 6편 + 시연 가이드)
- **다음 할 일**: 현재 **두 트랙** 병행.
  - ⭐ **화면 트랙(활성)**: 프론트엔드 백필 — SD ✅ 완료, **다음은 MM(자재) 화면**. 아래 "프론트엔드(화면) 백필" 절의 "다음 세션 시작점" 참조.
  - **도메인 트랙**: Phase 8 — PP(생산) BOM/생산지시 (도메인 브리핑부터). `ERP-STUDY-PLAN.md` 참조.
- **마지막 갱신**: 2026-05-31
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
  - **화면 백필 순서**: SD ✅ → **MM(다음)** → FI(전표/계정/입출금) → HR(계약/근태/급여).
  - **참고(이미 검증된 토대)**: `static/js/erp.js`(api·flash·won·query·param), `static/css/app.css`(테마), `templates/fragments/layout.html`(셸), `templates/sd/**`(목록/상세/폼 예시 12종), `templates/dashboard.html`. SD 화면 코드를 복붙 변형하면 가장 빠름.
  - **Tabler 톤 통일 완료**(2026-05-31): `app.css` 전면 리튠(Inter 폰트·네이비 사이드바·Tabler 블루 #206bc4·크리스프 라운드·소프트 보더/섀도·소프트 뱃지), `erp.js` `ERP.badge` 소프트화. SD 14개(목록 4 `page-head`+`filter-bar`+`table-wrap`, 상세/폼 8 헤더 `page-head`) + `admin/users` 뱃지 + `login` 색/폰트 정렬. 기능 무변경.
  - **⭐ 다음 진도 UI-TODO — 검색 필터바(`filter-bar`) 디자인 개선**: 사용자 피드백 — 현재 검색 조건 영역이 "투박"함(맨 라벨+기본 select/date input 나열). 개선 방향(예시): ① 인풋을 더 콤팩트/정렬되게(라벨을 인풋 안 placeholder 또는 작은 상단 라벨+일관 높이), ② date input 커스텀 스타일(브라우저 기본 "연도.월.일" 못생김 → 아이콘+통일 보더), ③ 검색/초기화 버튼 정렬·아이콘, ④ 필터바를 카드 톤과 한 줄로 단정하게(구분선/여백), 가능하면 "필터 칩" 또는 접이식. **지금 적용하지 말고 다음 세션(MM 화면 작업 시 함께)에서 `filter-bar` 공통 스타일로 한 번에 개선** — SD/MM 전 목록에 일괄 반영되도록 `app.css` `.filter-bar` + 목록 마크업 패턴으로 처리. 참고 톤: Tabler의 필터/검색 폼.
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
| **8** | **PP(생산) — BOM/생산지시** | ⬜ **다음 차례** |
| 9 | 배치 처리 — 야간 마감 ⭐ | ⬜ 예정 |
| 10 | 리포트와 대시보드 | ⬜ 예정 (← ERP 단일 시스템 학습 종료) |
| 11 | MES 프로젝트 셋업 + 연계 인프라 | ⬜ 예정 |
| 12 | MES 마스터 + 작업지시 수신 (ERP→MES) | ⬜ 예정 |
| 13 | MES 생산 실적 등록 | ⬜ 예정 |
| 14 | MES→ERP 실적 전송 (Kafka) ⭐ | ⬜ 예정 |
| 15 | MES 품질 검사 + 설비 상태 | ⬜ 예정 |
| 16 | 통합 시나리오 + 운영 관점 (← 전체 종료) | ⬜ 예정 |

> 상세 커리큘럼은 [`ERP-STUDY-PLAN.md`](./ERP-STUDY-PLAN.md), Phase별 산출물 인덱스는 [`doc/README.md`](./doc/README.md).

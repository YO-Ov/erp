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

- **🎉 Phase 0~16 전체 구현 완료·검증.** 이후 hwlee님 요청으로 **실무형 기능을 점진적으로 확장 중**(아래 2026-06-10 항목들). 학습 문서(doc/)는 별도 트랙.

### ▶ 다음 작업 (2026-06-10 세션 종료 — 다음에 여기부터)

> **🅰 우선 후보 = 재고 가시성(영업)** — hwlee님과 논의했으나 **착수 전**(레벨 미결정).
> - **문제(갭)**: 영업(SALES)이 재고를 **조회조차 못 함**(`StockController`·`MaterialsViewController` 가 PURCHASING/ADMIN). 정작 영업이 출하하려면 재고를 알아야 하는데 못 봄.
> - **실무 핵심**: 영업이 봐야 할 건 "총 재고"가 아니라 **ATP(약속가능재고) = 현재고 − 미출하 확정수주(orderQty−shippedQty) + 입고·생산 예정**. 총 재고만 보면 이미 다른 수주에 묶인 물량을 중복 약속하는 사고.
> - **추천 = 레벨②**: 재고 조회 권한 SALES 추가(읽기) + **수주 화면에 ATP 인라인 표시**(신용한도 표시 옆). 데이터는 이미 다 있음(Stock.qtyOnHand, SalesOrderLine.orderQty/shippedQty). 계획오더(MRP)와 연결됨. (레벨① 단순 현재고만 / 레벨③ 영업용 재고 조회 화면 추가 — 선택)
> - **원칙**: 재고 = **조회는 넓게(영업 등), 변경(입고·출고)은 PURCHASING만**. (고객 마스터 "기본정보 영업/한도 재무" 와 같은 사상.)
>
> **🅱 보조 후보**: ① 여신 요청에 결재 이력은 했고, **거래처(Vendor) 마스터도 같은 단순화**(구매팀 등록 화면 없음) → 필요 시 고객과 같은 패턴으로. ② 학습 문서 `doc/03 MM` 부터(아직 01·02만 작성). ③ doc/에 오늘 추가분(MRP·알림·여신·고객권한) 워크스루 글.
>
> ⚠️ **오늘(2026-06-10) 작업분 전부 미커밋** — 다른 PC에서 이어가려면 **hwlee님이 직접 커밋+푸시** 해야 함. 양이 많음(아래 6개 기능).

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

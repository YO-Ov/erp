# 4/7. `@Order` — 한 사건, 두 리스너, 누가 먼저인가 ⭐

> Phase 4 에선 한 사건(`DeliveryShippedEvent`)을 한 리스너만 들었다. Phase 5 에선 **같은 사건을 두 모듈이 동시에 듣는다** — MM(재고 차감)과 FI(매출원가 분개). 그런데 회계의 매출원가는 **재고가 먼저 빠진 뒤** 에야 단가를 알 수 있다. 순서가 정합성을 좌우 — `@Order` 가 그 보장 도구.
>
> 설계 결정 #10: 재고 리스너 `@Order(10)` → 회계 리스너 `@Order(20)`. 작은 값이 먼저.

대상 파일:

```
hwlee-erp/src/main/java/com/hwlee/erp/
├─ mm/integration/sd/
│  └─ DeliveryEventListener.java          @Order(10) onShipped — 재고 차감
└─ fi/integration/sd/
   ├─ CogsAccountingListener.java         @Order(20) onShipped — 매출원가 분개
   └─ SalesAccountingListener.java        InvoiceIssuedEvent (단일 리스너, @Order 불필요)
└─ fi/integration/mm/
   └─ PurchaseAccountingListener.java     GoodsReceiptPostedEvent (단일 리스너)
```

---

## 🔥 문제 — 한 사건을 두 리스너가 들으면?

`DeliveryShippedEvent` (출하 확정) 가 발행되면 두 가지 일이 일어나야 한다:

1. **MM 재고 차감** — Phase 4 부터. `GoodsIssue` 생성 + `Stock.qty_on_hand` 감소 + `StockMovement(-)` 적재. 적재된 StockMovement 의 `unit_cost` 가 **출고 단가** (Stock.averageCost 의 직전 값).
2. **FI 매출원가 분개** — Phase 5 신규. `차) 매출원가 / 대) 재고자산` 전표. 금액은 **출고된 라인들의 단가 × 수량 합산**.

문제: 2번의 단가는 1번이 만든 `StockMovement.unit_cost` 에서 가져온다. **순서가 거꾸로면 매출원가가 0** 이다 (StockMovement 가 없음).

> 💡 비유 — "주방장이 요리를 내고 나서 영수증을 끊는다" 와 "영수증부터 끊고 요리를 낸다" 는 결과가 다르다. 회계의 매출원가는 항상 출고 단가가 정해진 뒤에 끊는 영수증.

---

## 🔥 Spring 의 기본 — 순서 미보장

`@TransactionalEventListener` 를 같은 이벤트 타입에 여러 개 붙여도, Spring 은 **호출 순서를 기본적으로 보장하지 않는다**. JVM/리플렉션이 메서드를 발견하는 순서에 의존 → 빌드에 따라 달라질 수 있음. 회계 정합성이 우연에 기대게 됨.

→ `@Order(int)` (또는 `Ordered` 인터페이스) 로 명시:

| `@Order` 값 | 우선순위 |
| --- | --- |
| 작은 값 (`@Order(10)`) | 먼저 실행 |
| 큰 값 (`@Order(20)`) | 나중에 실행 |
| 없음 | 기본값 `Ordered.LOWEST_PRECEDENCE` (Integer.MAX_VALUE) — 즉 가장 나중 |

---

## 🔥 재고 리스너 — `@Order(10)`

`mm/integration/sd/DeliveryEventListener.java`:

```java
@Slf4j
@Component
@RequiredArgsConstructor
class DeliveryEventListener {

    private final GoodsIssueService goodsIssueService;

    /**
     * @Order(10) 으로 회계 리스너(@Order(20), Phase 5) 보다 먼저 실행되도록 못박는다.
     * 회계의 매출원가 분개는 StockMovement 의 unit_cost 가 박혀 있어야 계산 가능 —
     * 즉 GoodsIssue.post 가 끝난 뒤에 회계가 동작해야 한다. 순서가 정합성을 좌우.
     */
    @Order(10)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onShipped(DeliveryShippedEvent event) {
        log.info("출하 확정 사건 수신: deliveryId={}, warehouseId={}, lines={}",
                event.deliveryId(), event.warehouseId(), event.lines().size());
        goodsIssueService.createAndPostFromDelivery(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onCancelled(DeliveryCancelledEvent event) { ... }
}
```

Phase 4 의 기존 리스너에 **`@Order(10)` 한 줄만 추가**. 동작은 그대로. 변경 영향이 단 한 줄.

---

## 🔥 회계 리스너 — `@Order(20)`

`fi/integration/sd/CogsAccountingListener.java`:

```java
@Slf4j
@Component
@RequiredArgsConstructor
class CogsAccountingListener {

    private final AutoJournalService autoJournal;

    @Order(20)   // 재고 리스너(10) 보다 나중. unit_cost 가 박힌 뒤에야 매출원가 계산 가능.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onShipped(DeliveryShippedEvent event) {
        log.info("출하 확정 사건 수신(매출원가용): deliveryId={}", event.deliveryId());
        autoJournal.createCogsEntry(event);
    }
}
```

Phase 4 와 같은 구조 (package-private 클래스, `BEFORE_COMMIT`, `@RequiredArgsConstructor`), 두 가지만 다름:

1. **`@Order(20)`** — 재고 리스너보다 나중.
2. **호출 대상이 `AutoJournalService.createCogsEntry`** — 회계 모듈의 자동 분개.

> 💡 두 리스너는 **다른 패키지** (`mm/...` 와 `fi/...`) 에 산다. 같은 이벤트를 듣는다는 사실은 코드만 보면 안 보인다 — IDE 의 "Find Usages" 가 유일한 단서. 그래서 `@Order` 값 옆 주석에 "재고 리스너(10) 보다 나중" 같은 cross-reference 메모가 필수.

---

## 🔥 한 트랜잭션, 두 리스너, 명시된 순서

전체 흐름을 펼쳐 보면:

```
출하 확정 (DeliveryService.create)
   │
   ├─ Delivery DRAFT → SHIPPED
   ├─ SO 라인 shipped_qty 누적
   ├─ publishEvent(DeliveryShippedEvent)
   │
   │   ───────── BEFORE_COMMIT 단계 시작 ─────────
   │
   ├─ @Order(10) DeliveryEventListener.onShipped
   │     └─ goodsIssueService.createAndPostFromDelivery
   │           ├─ GoodsIssue 생성·POSTED
   │           ├─ Stock.qty_on_hand 차감 + Stock.averageCost 유지
   │           └─ StockMovement(reason=GOODS_ISSUE, unit_cost=직전 평균, refType="GI", refId=giId) 적재
   │
   ├─ @Order(20) CogsAccountingListener.onShipped
   │     └─ autoJournal.createCogsEntry(event)
   │           ├─ goodsIssueRepository.findByDeliveryId(deliveryId) → gi
   │           ├─ stockMovementRepository.findByRefTypeAndRefIdAndReason("GI", gi.id, GOODS_ISSUE)
   │           ├─ Σ unit_cost × |qty_delta| = totalCost
   │           ├─ JournalEntry.draft(...).addDebit(COGS, totalCost).addCredit(INVENTORY, totalCost).post()
   │           └─ journalRepository.save(...)
   │
   │   ───────── BEFORE_COMMIT 단계 종료 ─────────
   │
   └─ COMMIT
```

전부 **한 트랜잭션 안**. 어디서든 예외가 나면 출하·SO 누적·재고·StockMovement·회계 전표 전부 롤백 — Phase 4 의 원자성 보장이 그대로 회계까지 확장.

---

## 🔥 `@Order` 가 없으면? — 테스트로 본 무서운 모양

`@Order(10)` 없이 두 리스너가 들으면, 회계 리스너가 먼저 돌 수도 있다. 그러면:

```java
GoodsIssue gi = goodsIssueRepository.findByDeliveryId(event.deliveryId())
        .orElseThrow(() -> new IllegalStateException(
                "GoodsIssue 가 없다 — 재고 리스너(@Order=10) 가 먼저 돌지 않았다. "
                        + "deliveryId=" + event.deliveryId()));
```

`createCogsEntry` 의 첫 줄 — **GoodsIssue 가 없으면 IllegalStateException** 으로 즉시 거부. 정합성이 깨진 분개를 만들지 않는다.

→ 즉, `@Order` 가 없어도 "조용히 잘못된 전표" 가 만들어지진 않는다. **시끄럽게 실패**. 회계는 "조용한 오답" 이 가장 위험 → "큰 소리로 실패" 가 정답.

> 💡 두 겹 방어: (1) `@Order` 가 정상 동작을 보장 (2) `orElseThrow` 가 비정상 상황을 즉시 폭로. 한쪽이 무너져도 다른 쪽이 잡아낸다.

---

## 🔥 매출원가 단가 — 옵션 A vs B vs C (설계 결정 #9)

설계 §5.3 에서 세 옵션을 비교했었다:

| 옵션 | 단가 가져오는 방법 | 트레이드오프 |
| --- | --- | --- |
| **A** | 회계 리스너가 `deliveryId → GoodsIssue → StockMovement` 조회 | ✅ `@Order` 학습 직결. FI 가 MM 원장을 읽음 |
| B | `GoodsIssue.post` 시 별도 `GoodsIssuePostedEvent` 발행 → 회계가 구독 | 이벤트 중첩(리스너 안에서 또 발행). 흐름 추적 ↑ |
| C | `DeliveryShippedEvent` 본문에 단가 미리 실음 | SD 가 원가를 알아야 함 — 책임 위반 |

**옵션 A 채택** 의 진짜 이유는 "회계가 MM 원장을 읽는 것이 회계 본질에 부합" — 회계는 원래 다른 모듈의 사실을 읽어와 정리하는 모듈이다. 다음 글(`05-매출원가-단가연결.md`) 에서 이 조회 흐름을 상세히 본다.

---

## 🔥 다른 두 리스너는 왜 `@Order` 가 없나

| 리스너 | 이벤트 | 같은 이벤트를 듣는 다른 리스너? | `@Order` |
| --- | --- | --- | --- |
| `SalesAccountingListener` | `InvoiceIssuedEvent` | 없음 | 불필요 |
| `PurchaseAccountingListener` | `GoodsReceiptPostedEvent` | 없음 | 불필요 |
| **`CogsAccountingListener`** | **`DeliveryShippedEvent`** | **있음 (`DeliveryEventListener`)** | **`@Order(20)`** ⭐ |

→ **`@Order` 는 같은 이벤트에 리스너가 둘 이상일 때만 의미가 있다**. 단일 리스너에 붙이면 추가 가치 없음.

지금은 그렇지만, 미래에 또 다른 모듈(예: 통계 모듈)이 `InvoiceIssuedEvent` 를 듣게 되는 순간 — 그 새 리스너와 `SalesAccountingListener` 사이의 순서 규약을 다시 짚어야 한다. 그래서 "지금 안 필요해도 미리 붙여 두자"는 충분히 합리적인 선택이기도 하지만, **YAGNI** 원칙에 따라 일단 빠뜨리고 필요해질 때 추가.

---

## 🔥 `@Order` 값의 관습 — 10 단위로

```java
@Order(10)  // MM 재고
@Order(20)  // FI 회계
```

값 사이에 9를 비워둔다. 미래에 두 리스너 사이에 끼워 넣을 일이 생기면 (`@Order(15)`) 다른 코드를 안 건드리고 추가 가능 — DB 마이그레이션 버전 번호와 같은 발상.

> 💡 Spring 의 표준 — `Ordered.HIGHEST_PRECEDENCE = Integer.MIN_VALUE`, `LOWEST_PRECEDENCE = Integer.MAX_VALUE`. 보통은 0 ~ 100 사이의 작은 정수를 쓰고, 인프라 측 리스너는 음수를 쓰는 관습.

---

## 🔥 테스트로 검증 — `FiAccountingIntegrationTest`

`출하_확정_시_매출원가_분개_자동_생성`:

```java
// 6대 출하 — 매출원가 = 6 × 800,000 = 4,800,000
DeliveryResponse dlv = deliveryService.create(new DeliveryCreateRequest(...));

Long giId = goodsIssueIdOfDelivery(dlv.id());
List<JournalEntry> entries = journalRepository.findBySourceTypeAndSourceIdWithLines(JournalSource.GI, giId);

JournalEntry je = entries.get(0);
assertThat(je.getTotalDebit())
        .as("재고 리스너(@Order=10) 가 unit_cost 를 박은 뒤 회계 리스너(@Order=20) 가 합산")
        .isEqualByComparingTo("4800000");
assertThat(debitOf(je, SystemAccounts.COGS)).isEqualByComparingTo("4800000");
assertThat(creditOf(je, SystemAccounts.INVENTORY)).isEqualByComparingTo("4800000");
```

매출원가가 정확히 `qty × 입고단가` 로 나와야 통과. `@Order` 가 빠지면 (`createCogsEntry` 의 `orElseThrow` 가 발동) 트랜잭션 자체가 롤백돼 출하가 실패 → 테스트가 다른 모양으로 실패 → 즉시 발견.

---

## 정리

| 학습 포인트 | 코드 위치 |
| --- | --- |
| Spring 의 리스너 순서는 기본 미보장 | — (관습) |
| `@Order(10)` 재고 → `@Order(20)` 회계 | `DeliveryEventListener` / `CogsAccountingListener` |
| 작은 값이 먼저 (Phase Method Reference: `Ordered`) | — |
| 순서가 정합성을 좌우하는 케이스 ⭐ | 매출원가는 unit_cost 필요 |
| `@Order` 없을 때의 방어선 — `orElseThrow` | `createCogsEntry` 첫 줄 |
| `@Order` 는 같은 이벤트에 리스너가 둘 이상일 때만 의미 | 다른 두 리스너는 미부착 |
| `@Order` 값은 10 단위로 (확장성) | — |
| 통합 테스트가 순서 보장을 검증 | `FiAccountingIntegrationTest` |

다음 글(`05-매출원가-단가연결.md`) 에서는 회계 리스너가 **어떻게 MM 원장(StockMovement)을 따라가 매출원가 단가를 구하는지** 를 따라간다.

# 2/6. @TransactionalEventListener — phase 옵션과 한 트랜잭션의 원자성

> "출하 한 건" 이라는 **하나의 비즈니스 사건** 이 Delivery·SO 라인·GoodsIssue·Stock·StockMovement 라는 여러 테이블을 건드린다. 이 모두가 **한 트랜잭션** 으로 묶여 전부 박히거나 전부 사라져야 한다.
> 설계서 §3(리스너 배치)의 핵심 결정 #1 "통합 패턴 = `@TransactionalEventListener(BEFORE_COMMIT)`" 과 §5(트랜잭션 경계 + 전파)의 결정 #7 "전파 = `REQUIRED`" 의 실제 구현.

대상 파일:

```
hwlee-erp/src/main/java/com/hwlee/erp/
├─ mm/
│  ├─ integration/sd/
│  │  └─ DeliveryEventListener.java        (onShipped / onCancelled — BEFORE_COMMIT)
│  └─ goodsissue/
│     └─ GoodsIssueService.java            (createAndPostFromDelivery / cancelByDeliveryId / postStock)
└─ sd/delivery/
   └─ DeliveryService.java                 (publishEvent — 출하 트랜잭션 안에서)
```

---

## 🔥 한 비즈니스 사건 = 한 트랜잭션

출하 확정 버튼 한 번에 일어나는 일을 펼쳐 보면 5개 테이블이 움직인다:

| 테이블 | 변화 | 누가 |
| --- | --- | --- |
| `delivery` | DRAFT → SHIPPED | `DeliveryService.create` |
| `sales_order_line` | `shipped_qty` 누적 | `order.recordShipment(...)` |
| `goods_issue` (+ `goods_issue_line`) | 자동 GI 생성 → POSTED | `GoodsIssueService.createAndPostFromDelivery` |
| `stock` | `qty_on_hand` 차감 | `Stock.issue(qty)` |
| `stock_movement` | (-) 원장 행 적재 | `StockMovement.of(...)` |

이 5개가 **전부 성공하거나 전부 실패해야** 한다. 출하는 됐는데 재고가 안 빠지면 → 다음 출하의 가용 검증이 거짓말을 하게 되고, 장부가 실물과 어긋난다. **부분 상태(partial state)** 가 ERP 에서 가장 무서운 사고다.

→ Phase 4 의 학습 핵심은 "두 모듈(SD·MM)이 직접 호출 없이 이벤트로 느슨하게 연결되면서도, 동시에 **한 트랜잭션의 원자성**을 어떻게 지키는가" 다. 그 답이 `@TransactionalEventListener(BEFORE_COMMIT)` + `REQUIRED` 조합이다.

> 💡 "느슨한 결합(이벤트)" 과 "강한 정합성(한 트랜잭션)" 은 보통 상충하는 가치다. Spring 의 `@TransactionalEventListener` 가 이 둘을 동시에 잡는다 — 발행자는 누가 듣는지 모르지만(느슨), 리스너는 발행자와 같은 트랜잭션에서 돈다(강함).

---

## 🔥 `DeliveryEventListener` — 단 두 메서드

```java
@Slf4j
@Component
@RequiredArgsConstructor
class DeliveryEventListener {

    private final GoodsIssueService goodsIssueService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onShipped(DeliveryShippedEvent event) {
        log.info("출하 확정 사건 수신: deliveryId={}, warehouseId={}, lines={}",
                event.deliveryId(), event.warehouseId(), event.lines().size());
        goodsIssueService.createAndPostFromDelivery(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onCancelled(DeliveryCancelledEvent event) {
        log.info("출하 취소 사건 수신: deliveryId={}", event.deliveryId());
        goodsIssueService.cancelByDeliveryId(event.deliveryId());
    }
}
```

세 가지를 짚는다:

1. **클래스가 `package-private`** (`class`, `public` 없음). 외부에서 이 리스너를 직접 부를 일이 없다 — Spring 이 이벤트로만 깨운다. 노출을 최소화.
2. **리스너 메서드 자체엔 `@Transactional` 이 없다.** 트랜잭션은 호출하는 `GoodsIssueService` 메서드가 가진다(뒤에서 설명).
3. **로그 한 줄** — 통합 흐름은 직접 호출이 아니라 "이벤트가 어디서 어디로 흘렀나" 가 눈에 안 보인다. 그래서 수신 시점 로그가 시연/디버깅의 생명줄이다.

---

## 🔥 `phase` 의 4가지 옵션 — 왜 `BEFORE_COMMIT` 인가

`@TransactionalEventListener` 는 "리스너를 발행자 트랜잭션의 **어느 시점**에 실행할지" 를 `phase` 로 고른다.

| phase | 실행 시점 | 발행자 트랜잭션과의 관계 | 채택 |
| --- | --- | --- | --- |
| **`BEFORE_COMMIT`** | commit **직전** | **같은 트랜잭션** 안에서 실행 | ✅ |
| `AFTER_COMMIT` (기본값) | commit **후** | 이미 끝난 트랜잭션 (사실상 별도) | ❌ |
| `AFTER_ROLLBACK` | rollback 후 | 끝난 트랜잭션 | (안 씀) |
| `AFTER_COMPLETION` | commit/rollback 후 | 끝난 트랜잭션 | (안 씀) |

`AFTER_*` 계열은 전부 "트랜잭션이 이미 결판난 뒤" 다. 그래서 그 안에서 무슨 일이 일어나도 **발행자 트랜잭션을 되돌릴 수 없다.**

### `AFTER_COMMIT`(기본값) 으로 했을 때의 사고

```
@Transactional   DeliveryService.create
   ├─ delivery.ship()            → SHIPPED
   ├─ order.recordShipment(...)  → shipped_qty 누적
   └─ publishEvent(...)
COMMIT  ←━━━━━━━━━━━━━━━━━━━━━━━ 출하·SO 라인이 DB 에 영구 박힘
   │
   ▼  (커밋이 끝난 뒤에야 리스너 실행)
onShipped(event)   @AfterCommit
   └─ createAndPostFromDelivery
        └─ stock.issue(7)  → 재고 5뿐 → InsufficientStockException
                              ↑ 이미 늦었다. 출하는 커밋됨.
```

결과: **출하는 SHIPPED 로 박혔는데 재고는 안 빠진 부분 상태.** 다음 출하가 "재고 5개 있네" 하고 또 보낸다. 장부와 실물이 영영 어긋난다. 설계서 §3.3 이 짚은 바로 그 사고.

> ⚠️ `@TransactionalEventListener` 의 기본 phase 는 `AFTER_COMMIT` 이다. `phase =` 를 안 쓰면 이 사고가 기본값이다. Phase 4 에서 `phase = TransactionPhase.BEFORE_COMMIT` 를 **명시적으로** 박은 이유.

`BEFORE_COMMIT` 만이 "commit 직전, 같은 트랜잭션 안" 이라 리스너 실패 시 출하까지 통째로 롤백할 수 있다.

---

## 🔥 전파 `REQUIRED` — 리스너엔 `@Transactional` 이 없는데 어떻게 같은 트랜잭션인가

리스너 `onShipped` 엔 `@Transactional` 이 없다. 그런데도 "같은 트랜잭션 안" 이라는 게 어떻게 성립하나? 답은 **호출 대상** 에 있다:

```java
// GoodsIssueService — 클래스 기본은 readOnly, 쓰기 메서드만 @Transactional
@Transactional   // ← 전파 옵션 미지정 = REQUIRED (기본)
public GoodsIssueResponse createAndPostFromDelivery(DeliveryShippedEvent event) {
    ...
}
```

`@Transactional` 의 전파(propagation) 기본값은 `REQUIRED`. 의미는:

> "트랜잭션이 **이미 있으면 그것에 참여**하고, 없으면 새로 만든다."

`onShipped` 는 `DeliveryService.create` 의 트랜잭션(BEFORE_COMMIT 시점이므로 아직 살아있음) 안에서 실행된다. 그 안에서 `createAndPostFromDelivery(@Transactional REQUIRED)` 를 부르면 → **새 트랜잭션을 만들지 않고 기존 출하 트랜잭션에 합류**한다.

### `REQUIRED` vs `REQUIRES_NEW` (설계서 §5.2)

| 전파 옵션 | 출하 트랜잭션 안에서 호출되면 | 결과 |
| --- | --- | --- |
| **`REQUIRED`** (채택) | 기존 출하 트랜잭션에 **참여** | 재고 차감 실패 → 출하까지 한 번에 롤백 ✅ |
| `REQUIRES_NEW` | **별도 새 트랜잭션** 시작 (출하 트랜잭션 일시 정지) | 재고 차감 트랜잭션만 독립 커밋/롤백 → 출하와 따로 놂 → **부분 상태 사고** ❌ |

`REQUIRES_NEW` 였다면 재고 차감이 **자기만의 트랜잭션** 으로 먼저 커밋되거나 롤백된다. 출하 트랜잭션이 나중에 다른 이유로 롤백돼도 재고는 이미 빠진 채 남는다. → `BEFORE_COMMIT` 의 의도(원자성)가 정확히 깨진다.

```
[REQUIRED — 채택]                      [REQUIRES_NEW — 사고]
출하 TX ───────────────┐               출하 TX ──┐         ┌──── 출하 TX 재개
  └ 재고 TX (같은 경계) │                  (정지) │  재고 TX │
                       │                         └─[독립 COMMIT]┘
  하나로 COMMIT/ROLLBACK                  재고는 따로 커밋됨 → 출하 롤백돼도 재고는 빠짐
```

> 💡 그래서 리스너에 굳이 `@Transactional` 을 붙이지 않았다. 붙이면 오히려 "이 리스너가 독립 트랜잭션 같다" 는 오해를 부른다. 트랜잭션의 주인은 발행자(DeliveryService)이고, 서비스 메서드는 거기에 `REQUIRED` 로 얹힐 뿐 — 이 의도를 코드 모양으로 드러낸 것.

---

## 🔥 예외 전파 = 전체 롤백 → 409 (설계서 §5.3)

`BEFORE_COMMIT` + `REQUIRED` 조합의 진짜 이득은 **예외 처리를 따로 안 짜도 된다** 는 점이다.

```java
@TransactionalEventListener(phase = BEFORE_COMMIT)
void onShipped(DeliveryShippedEvent event) {
    goodsIssueService.createAndPostFromDelivery(event);
    //                       ↑ 여기서 InsufficientStockException / EntityNotFoundException 발생 가능
}
```

가용 재고가 부족하면 `Stock.issue(qty)` 가 `InsufficientStockException` 을 던진다. 이 예외가 commit **이전**에 터지므로:

```
createAndPostFromDelivery 안에서 InsufficientStockException
        │
        ▼
Spring 이 출하 트랜잭션을 rollbackOnly 로 마킹
        │
        ▼
commit 시도 → rollback 으로 전환
   delivery · sales_order_line · goods_issue · stock · stock_movement  전부 취소
        │
        ▼
예외가 DeliveryService.create 호출자(컨트롤러) 로 그대로 전파
        │
        ▼
GlobalExceptionHandler 가 잡아 409 INSUFFICIENT_STOCK 응답
```

즉 Phase 3 에서 이미 만든 `InsufficientStockException → 409` 매핑이 **SD 출하 경로에서 그대로 재사용**된다. SD 측은 재고 예외의 존재조차 몰라도 된다. 예외는 호출 스택을 타고 자연스럽게 위로 올라가 클라이언트에 닿는다.

`EntityNotFoundException`(창고/품목/재고 없음)도 같은 길. 어떤 예외든 commit 전이라 "전체 롤백" 이 공짜로 따라온다.

---

## 🔥 `createAndPostFromDelivery` 내부 — 그리고 `postStock` 의 DRY

리스너가 부르는 서비스 메서드의 속:

```java
@Transactional
public GoodsIssueResponse createAndPostFromDelivery(DeliveryShippedEvent event) {
    Warehouse warehouse = warehouseRepository.findById(event.warehouseId())
            .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: id=" + event.warehouseId()));

    String number = numberGenerator.nextGoodsIssueNumber(event.shippedDate());
    GoodsIssue gi = GoodsIssue.draftForDelivery(
            number, warehouse, event.shippedDate(), event.deliveryId());   // ← delivery_id 박힘

    for (DeliveryShippedEvent.Line line : event.lines()) {
        Item item = itemRepository.findById(line.itemId())
                .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + line.itemId()));
        gi.addLine(item, line.quantity());
    }

    repository.save(gi);   // id 채번 — StockMovement.refId 에 박기 위해 post 전에 저장
    postStock(gi);         // ← 비관 락 차감 + 원장 (공통 메서드)
    return mapper.toResponse(gi);
}
```

순서: **창고 조회 → GI 채번 → `GoodsIssue.draftForDelivery(...)` → 라인 추가 → `save` → `postStock(gi)`**.

`save` 를 `postStock` **전에** 부르는 이유 — `postStock` 안에서 `StockMovement.of(..., "GI", gi.getId(), ...)` 로 GI 의 id 를 원장에 박는다. id 는 INSERT 가 돼야 채번되므로 미리 저장.

### `post(id)` 와 `createAndPostFromDelivery` 가 공유하는 `postStock`

직접 출고와 출하 연계는 입구만 다르고 **재고 차감 로직은 똑같다.** 이걸 private 메서드로 빼서 둘이 재사용한다:

```java
@Transactional
public GoodsIssueResponse post(Long id) {       // 직접 출고
    GoodsIssue gi = getOrThrow(id);
    postStock(gi);                               // ← 공통
    return mapper.toResponse(gi);
}

// ↑ post(id) 와 createAndPostFromDelivery 가 함께 부르는 비관 락 차감 로직
private void postStock(GoodsIssue gi) {
    gi.post(LocalDateTime.now(clock));           // DRAFT → POSTED (자기 상태 검증)
    Warehouse warehouse = gi.getWarehouse();
    LocalDateTime now = LocalDateTime.now(clock);

    for (GoodsIssueLine line : gi.getLines()) {
        Item item = line.getItem();
        Stock stock = stockRepository
                .findForUpdate(item.getId(), warehouse.getId())   // ← 비관 락 (Phase 3)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found: item=" + item.getId() + ", warehouse=" + warehouse.getId()
                                + " — 출고 전에 입고가 있어야 합니다."));
        BigDecimal appliedCost = stock.issue(line.getQuantity());

        stockMovementRepository.save(StockMovement.of(
                item, warehouse, line.getQuantity().negate(), appliedCost,
                MovementReason.GOODS_ISSUE, "GI", gi.getId(), now));
    }
}
```

> 💡 **Phase 3 의 비관 락 차감 로직이 단 한 줄도 안 바뀌고 Phase 4 에서 재사용된다.** "출고가 사용자 버튼이든 출하 이벤트든, 재고를 빼는 본질은 같다" 는 도메인 진실을 `postStock` 한 메서드가 표현한다. 새 진입점(출하)이 생겨도 핵심 불변식(락·가용 검증·원장)은 한 곳에만 존재 — 버그가 갈라지지 않는다.

### `cancelByDeliveryId` — 역추적 + 방어적 무시

취소 경로도 같은 재사용 원칙:

```java
@Transactional
public void cancelByDeliveryId(Long deliveryId) {
    GoodsIssue gi = repository.findByDeliveryId(deliveryId).orElse(null);
    if (gi == null) {
        log.info("출하 id={} 에 연결된 GoodsIssue 없음 — 취소 건너뜀", deliveryId);
        return;                                  // ① Phase 4 적용 전 출하 — 무시
    }
    if (gi.getStatus() != GoodsIssueStatus.POSTED) {
        log.info("GoodsIssue id={} 가 POSTED 아님(현재 {}) — 취소 건너뜀", gi.getId(), gi.getStatus());
        return;                                  // ② 이미 취소됨 — 멱등성
    }
    cancel(gi.getId());   // 기존 cancel 재사용 — 비관 락 + 재고 복원 + ADJUSTMENT_PLUS
}
```

- `findByDeliveryId` — §4 에서 추가한 `delivery_id` FK 인덱스로 "이 출하의 GI" 를 빠르게 역추적.
- **방어적 무시 2가지** — (①) 연결된 GI 가 없거나(Phase 4 적용 이전 데이터), (②) 이미 POSTED 가 아니면(중복 취소 등) 예외 대신 조용히 건너뛴다. 같은 취소 이벤트가 두 번 와도 안전(멱등).
- 실제 복원은 Phase 3 의 기존 `cancel(id)` 를 그대로 호출 — 비관 락 + 재고 복원 + `ADJUSTMENT_PLUS` 원장 행. 또 한 번의 DRY.

---

## 🔥 발행부 — `DeliveryService.create` 가 사건을 알린다

```java
@Transactional
public DeliveryResponse create(DeliveryCreateRequest req) {
    ...
    delivery.ship();
    for (DeliveryLine dline : delivery.getLines()) {
        order.recordShipment(dline.getSalesOrderLine(), dline.getQuantity());
    }
    repository.save(delivery);

    // ⭐ Phase 4 — 출하 확정 사건 발행. MM 의 @TransactionalEventListener(BEFORE_COMMIT) 가
    // 같은 트랜잭션 안에서 GoodsIssue 자동 생성 + 재고 차감. 가용 부족이면 여기까지 전부 롤백.
    events.publishEvent(new DeliveryShippedEvent(
            delivery.getId(), delivery.getWarehouseId(), delivery.getShippedDate(),
            toEventLines(delivery)));

    return mapper.toResponse(delivery);
}
```

`events.publishEvent(...)` 는 `DeliveryService.create` 의 `@Transactional` 안에서 호출된다. `BEFORE_COMMIT` 리스너는 이 트랜잭션이 commit 되려는 직전에 깨어난다. **SD 는 누가 듣는지 모른다** — `ApplicationEventPublisher` 에 사건을 던질 뿐, `GoodsIssueService` 를 직접 import 하지 않는다(의존 방향은 다음 편 §3 에서).

취소도 대칭으로 `cancel(id)` 끝에서 `DeliveryCancelledEvent(delivery.getId())` 를 발행한다.

---

## 🔥 전체 흐름 — 출하 시퀀스 다이어그램

```
[Client]
POST /api/deliveries
   │
   ▼
[DeliveryService.create]  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  @Transactional 시작 (BEGIN)
   │
   ├─ delivery.ship()                         → DRAFT → SHIPPED
   ├─ order.recordShipment(...)               → SO 라인 shipped_qty
   ├─ repository.save(delivery)
   │
   ├─ events.publishEvent(DeliveryShippedEvent(...))      ← ⭐ 사건 발행
   │       │
   │       │   (commit 직전, 같은 트랜잭션 안에서)
   │       ▼
   │  [DeliveryEventListener.onShipped]  @TransactionalEventListener(BEFORE_COMMIT)
   │       │
   │       ▼
   │  [GoodsIssueService.createAndPostFromDelivery]  @Transactional(REQUIRED → 위에 참여)
   │       │
   │       ├─ warehouseRepository.findById(event.warehouseId())
   │       ├─ GoodsIssue.draftForDelivery(번호, warehouse, shippedDate, deliveryId)
   │       ├─ for each line: gi.addLine(item, qty)
   │       ├─ repository.save(gi)              ← id 채번
   │       │
   │       └─ postStock(gi)                    ← post(id) 와 공유하는 공통 메서드
   │              ├─ gi.post(now)              → DRAFT → POSTED
   │              └─ for each line:
   │                   ├─ stockRepo.findForUpdate(itemId, warehouseId)   ← 비관 락 (SELECT … FOR UPDATE)
   │                   ├─ appliedCost = stock.issue(qty)                 ← 가용 검증 + 차감
   │                   │      ⚠ 부족하면 InsufficientStockException
   │                   └─ stockMovementRepo.save(StockMovement.of(…, qty.negate(), GOODS_ISSUE, "GI", gi.id, now))
   │
   └─ return mapper.toResponse(delivery)
        ↓
   @Transactional 종료 (COMMIT)  ━━━━━━━━━━━━━━━━━━━━━━  5개 테이블 변화가 단 한 번에 박힘
        ↓
   201 Created + DeliveryResponse


[가용 부족 실패 흐름]
   stock.issue(qty) → InsufficientStockException
        ↓
   @Transactional ROLLBACK  ━━━━━━━━━━━━━━━━━━━━━━━━━━━  delivery · sales_order_line ·
                                                        goods_issue · stock · stock_movement
                                                        모두 시도 전 상태로 복귀
        ↓
   예외가 컨트롤러로 전파 → GlobalExceptionHandler
        ↓
   409 Conflict + { code: INSUFFICIENT_STOCK, available, requested, ... }
```

→ **commit 한 번 = 출하라는 한 사건의 모든 흔적.** 그 직전(`BEFORE_COMMIT`)에 끼어든 리스너가 같은 경계(`REQUIRED`) 안에서 일하므로, 어느 한 곳이 실패하면 다섯 테이블이 함께 사라진다. 이것이 "한 비즈니스 사건 = 한 트랜잭션 = 원자성" 의 코드 모양.

---

## 자기 점검

- [ ] `@TransactionalEventListener` 의 phase 4가지 중 `BEFORE_COMMIT` 만이 출하 원자성을 보장하는 이유는?
- [ ] phase 를 명시 안 하면 기본값이 무엇이고, 그때 어떤 부분 상태 사고가 나는가?
- [ ] 리스너 `onShipped` 엔 `@Transactional` 이 없는데 어떻게 출하와 "같은 트랜잭션" 인가?
- [ ] `REQUIRES_NEW` 로 했다면 무엇이 깨지는가? (한 줄)
- [ ] `InsufficientStockException` 이 별도 예외 처리 코드 없이도 409 로 전파되는 경로는?
- [ ] `post(id)` 와 `createAndPostFromDelivery` 가 `postStock` 을 공유하는 것이 DRY 측면에서 주는 이득은?

---

이전 편 → [01-도메인-이벤트.md](./01-도메인-이벤트.md)
다음 편 → [03-패키지-방향-의존성.md](./03-패키지-방향-의존성.md)

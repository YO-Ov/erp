# 2/7. Stock(캐시) vs StockMovement(원장) — "현재" 와 "역사" 의 분리

> 한 행짜리 "지금" 과 누적되는 "과거" 가 한 도메인에 공존하는 구조.
> 설계서 §0의 핵심 결정 #3, #4, #6, #7, #8 — Phase 3 의 데이터 모델 그 자체.

대상 파일:

```
hwlee-erp/src/main/java/com/hwlee/erp/mm/stock/
├─ Stock.java              ← 캐시 (현재 보유 + 평균 원가 + @Version)
├─ StockMovement.java      ← 원장 (append-only)
├─ MovementReason.java     ← 부호와 reason 의 묶음
├─ StockRepository.java
├─ StockMovementRepository.java
└─ ...
```

---

## 🔥 두 테이블의 책임 분리

### Stock — "지금 얼마 남았는가" 의 단일 진실

```
stock
+----+---------+--------------+-------------+-------------+---------+
| id | item_id | warehouse_id | qty_on_hand | average_cost| version |
+----+---------+--------------+-------------+-------------+---------+
| 1  | 1       | 1            | 13.0000     | 1100.00     | 5       |
+----+---------+--------------+-------------+-------------+---------+
UNIQUE (item_id, warehouse_id)
CHECK (qty_on_hand >= 0)
CHECK (average_cost >= 0)
```

(item × warehouse) 당 정확히 한 행. **현재 보유** 와 **이동평균 원가** 를 캐시. 화면이 "이 창고에 노트북 몇 대 남았어요?" 물으면 이 한 행이 즉시 답한다.

### StockMovement — "과거에 어떤 변화가 있었는가" 의 원장

```
stock_movement
+----+---------+--------------+-----------+-----------+---------------+----------+--------+---------------------+
| id | item_id | warehouse_id | qty_delta | unit_cost | reason        | ref_type | ref_id | moved_at            |
+----+---------+--------------+-----------+-----------+---------------+----------+--------+---------------------+
| 1  | 1       | 1            | +10       | 1000      | GOODS_RECEIPT | GR       | 1      | 2026-05-28 09:00:00 |
| 2  | 1       | 1            | +10       | 1200      | GOODS_RECEIPT | GR       | 2      | 2026-05-28 10:30:00 |
| 3  | 1       | 1            | -7        | 1100      | GOODS_ISSUE   | GI       | 1      | 2026-05-28 14:15:00 |
+----+---------+--------------+-----------+-----------+---------------+----------+--------+---------------------+
KEY (item_id, warehouse_id, moved_at)
KEY (ref_type, ref_id)
```

매 이동을 한 행씩 append. 영구 보존.

### 정합성 약속

```sql
-- (모든 (item, warehouse) 에 대해)
SUM(stock_movement.qty_delta) = stock.qty_on_hand
```

이게 깨지면 시스템 자체가 거짓말을 하고 있는 것. 통합 테스트 (`MmScenarioTest.원장_합계는_Stock_과_일치한다`) 가 매 시나리오 후 이 식을 검증한다.

→ **캐시(Stock) 는 빠른 조회를 위한 것일 뿐, 진실은 원장(Movement) 에 있다**. Phase 2 의 "SO 라인 `shippedQty` 는 캐시, `Delivery` 행이 진실" 과 같은 구조.

---

## 🔥 왜 한 테이블에 다 안 두나 — 책임 분리의 이유

생각의 1차 형태: "테이블 하나에 (item, warehouse) 당 한 행 두고, 변경할 때마다 그 행을 UPDATE". 그런데:

- "현재 7대 남았어요" 라는 답을 받기는 빠르지만,
- "지난 한 달 동안 이 상품이 이 창고에서 어떻게 움직였어요?" 는 답이 없다 (과거가 사라짐).
- "재고가 5대 부족한데, 누가 언제 가져갔어요?" 도 답이 없다.

→ 회계의 원장(ledger) 원리. **현재 잔액은 누적의 결과지, 그 자체가 진실이 아니다**. 매 거래를 행으로 쌓고, 잔액은 그 합. 우리 모델은 잔액을 캐시하지만 (성능), 원장이 정답.

### 캐시를 두는 이유

매번 SUM 하면 행 수가 많아질수록 느려진다. 1년에 10만 건 거래가 있는 상품의 잔액을 알려면 SUM 10만 행. 그래서 캐시 한 행을 같이 유지하고, **모든 변경은 캐시와 원장을 한 트랜잭션에서 같이** 갱신.

```java
// GoodsReceiptService.post 안에서 (한 트랜잭션)
stock.receive(qty, unitCost);                          // ← 캐시 갱신
stockMovementRepository.save(StockMovement.of(...));   // ← 원장 추가
```

캐시와 원장이 어긋날 가능성은 트랜잭션 격리 (둘이 같은 @Transactional 안) + DB CHECK 제약으로 차단.

---

## 🔥 `Stock.java` — 도메인 메서드만 4개

```java
public class Stock extends BaseEntity {
    @ManyToOne private Item item;
    @ManyToOne private Warehouse warehouse;
    private BigDecimal qtyOnHand = BigDecimal.ZERO;
    private BigDecimal averageCost = BigDecimal.ZERO;
    @Version private Long version;

    public static Stock empty(Item item, Warehouse warehouse) { ... }
    public void receive(BigDecimal qty, BigDecimal unitCost) { ... }
    public BigDecimal issue(BigDecimal qty) { ... }
    public BigDecimal cancelReceipt(BigDecimal qty) { ... }
    public void cancelIssue(BigDecimal qty) { ... }
}
```

외부에서 직접 `qty_on_hand` / `average_cost` 를 세팅할 길이 없다. 4개 도메인 메서드만이 변경을 책임진다.

- `empty(...)` — 첫 입고 시 행을 만드는 정적 팩토리. qty=0, avg=0.
- `receive(qty, unitCost)` — 입고. 가중평균 갱신 + 수량 증가. 자세한 공식은 05편.
- `issue(qty)` — 출고. 부족하면 `InsufficientStockException`. 적용 단가(=현재 평균) 반환.
- `cancelReceipt/cancelIssue` — 취소. 평균은 건드리지 않고 수량만 차감/복원.

### 도메인 메서드가 반환하는 BigDecimal 의 의미

```java
public BigDecimal issue(BigDecimal qty) {
    ...
    BigDecimal applied = this.averageCost;
    this.qtyOnHand = this.qtyOnHand.subtract(qty);
    return applied;
}
```

`issue` 가 적용 단가를 반환하는 이유 — 호출 측 `GoodsIssueService.post` 가 `StockMovement.unit_cost` 에 정확히 그 값을 박아야 하기 때문. **차감 직전의 평균** 이 출고의 원가가 된다 (Phase 5 매출원가의 기반).

```java
// GoodsIssueService.post 안에서
BigDecimal appliedCost = stock.issue(line.getQuantity());
stockMovementRepository.save(StockMovement.of(
        item, warehouse, line.getQuantity().negate(), appliedCost,
        MovementReason.GOODS_ISSUE, "GI", gi.getId(), now));
```

→ "Stock 이 결정 → Movement 가 기록" 의 한 줄짜리 협력.

---

## 🔥 `StockMovement.java` — setter 없음, 생성 후 불변

```java
@Entity
public class StockMovement extends BaseEntity {

    @ManyToOne @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    @ManyToOne @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @Column(name = "qty_delta", nullable = false, updatable = false, ...)
    private BigDecimal qtyDelta;

    @Column(name = "unit_cost", nullable = false, updatable = false, ...)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, updatable = false, length = 20)
    private MovementReason reason;

    @Column(name = "ref_type", updatable = false, length = 10)
    private String refType;

    @Column(name = "ref_id", updatable = false)
    private Long refId;

    @Column(name = "moved_at", nullable = false, updatable = false)
    private LocalDateTime movedAt;

    public static StockMovement of(...) { ... }   // 유일한 진입점
}
```

핵심 두 가지:

### 1. 모든 컬럼이 `updatable = false`

JPA Dirty Checking 이 이 엔티티의 어느 필드를 바꿔도 UPDATE SQL 이 안 생긴다. 우연한 setter 호출이나 영속성 컨텍스트의 동기화로 인한 수정도 차단.

> 💡 setter 가 없어도 `@MappedSuperclass` 의 audit 필드 (`@LastModifiedDate updatedAt`) 는 JPA Auditing 으로 갱신될 수 있다. 그건 의도된 행위 (수정자/시간 추적) 라서 `updatable=false` 를 적용하지 않음 — 단, **비즈니스 필드는 모두 잠금**.

### 2. 생성자 비공개, 정적 팩토리만 진입점

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA 용
// + 별도 public constructor 없음

public static StockMovement of(Item item, Warehouse warehouse, BigDecimal qtyDelta,
                               BigDecimal unitCost, MovementReason reason,
                               String refType, Long refId, LocalDateTime movedAt) {
    // 검증 + 생성
}
```

`new StockMovement()` 는 protected 라 같은 패키지에서만 호출 가능. 외부는 무조건 `of(...)` 를 통과 → 검증을 거치지 않은 행이 생기는 일이 없다.

---

## 🔥 부호와 reason 의 일치 검증

`MovementReason` enum 의 핵심:

```java
public enum MovementReason {
    GOODS_RECEIPT(true),         // qty_delta > 0
    GOODS_ISSUE(false),          // qty_delta < 0
    ADJUSTMENT_PLUS(true),
    ADJUSTMENT_MINUS(false),
    SCRAP(false);

    private final boolean positive;
    public boolean isPositive() { return positive; }
    public boolean isNegative() { return !positive; }
}
```

`StockMovement.of()` 의 검증:

```java
if (reason.isPositive() && qtyDelta.signum() < 0)
    throw new IllegalArgumentException("reason " + reason + " 은 양수 qtyDelta 여야 한다: " + qtyDelta);
if (reason.isNegative() && qtyDelta.signum() > 0)
    throw new IllegalArgumentException("reason " + reason + " 은 음수 qtyDelta 여야 한다: " + qtyDelta);
```

**왜 굳이 둘 다 들고 검증을 하나** — 코드 가독성 + 분석 편의:
- `qty_delta` 만 있으면 부호로 입출고를 추론은 가능하지만, "왜 빠졌는가" (출하? 폐기? 조정?) 가 안 보임.
- `reason` 만 있고 부호가 없으면 SUM 같은 집계 쿼리가 reason 별 부호 룰을 알아야 함.

→ **둘 다 들고 정합성을 코드가 강제** 하는 게 가장 안전. 새 reason 이 추가될 때 부호 룰만 정해주면 자동으로 검증이 따라온다.

> ⚠️ 부호 룰이 enum 에 박혀 있으면 미래에 "예외적 음수 입고 (예: 매입환출)" 같은 케이스가 생길 때 한계가 있다. 그땐 새 reason (`RECEIPT_RETURN`) 을 추가해 음수 방향으로 정의. enum 이 분류 체계를 강제하는 방향.

---

## 🔥 `ref_type / ref_id` — 약한 참조의 의도

```java
@Column(name = "ref_type", updatable = false, length = 10)
private String refType;   // "GR" / "GI" / "ADJ" / null

@Column(name = "ref_id", updatable = false)
private Long refId;       // GR 또는 GI 의 ID (nullable)
```

**FK 가 아니다**. `fk_stock_movement_gr` 같은 외래키 제약을 안 건다. 일부러.

### 왜 FK 가 아닌가

새 이동 유형이 추가될 때마다 FK 컬럼을 늘리지 않기 위함:
- Phase 3 현재: `GR` (GoodsReceipt), `GI` (GoodsIssue)
- Phase 4 추가 예정: `DLV` (Delivery 와 연계되는 자동 출고)
- Phase 8: `PROD` (생산 실적의 자재 소모)
- Phase 9: `ADJ` (실사 조정 배치)

이걸 모두 FK 컬럼으로 따로 만들면 `stock_movement` 에 nullable FK 가 5개, 10개 늘어난다. **다형성** 같은 관계라 약한 참조가 적절.

### 대가

- FK 가 없어서 GR 행이 삭제돼도 stock_movement 의 `ref_id` 가 그대로 남는 dangling reference 가능. **하지만** GR/GI 는 트랜잭션 데이터라 물리 삭제 자체가 금지 (Phase 2 의 정책). 실제로는 안 깨진다.
- DB 차원의 join 시 코드와 ID 둘 다 매칭해야 함:
  ```sql
  SELECT * FROM stock_movement sm
   INNER JOIN goods_receipt gr ON gr.id = sm.ref_id AND sm.ref_type = 'GR'
  ```

### 인덱스 `(ref_type, ref_id)`

```sql
KEY idx_stock_movement_ref (ref_type, ref_id)
```

"GR-42 가 만든 이동 행 모두 보여줘" 같은 역추적 쿼리가 빠르도록. 입고 취소 시 같은 GR id 로 음수 ADJUSTMENT 행을 찾아 정정하는 패턴도 가능.

---

## 🔥 인덱스 설계 — 두 가지 흔한 쿼리에 최적화

```sql
KEY idx_stock_movement_item_wh_time (item_id, warehouse_id, moved_at)
KEY idx_stock_movement_ref (ref_type, ref_id)
```

### 인덱스 1: `(item_id, warehouse_id, moved_at)`

가장 흔한 조회: "이 상품의 이 창고에서, 최근 한 달 동안 어떤 이동이 있었나?"

```sql
SELECT * FROM stock_movement
 WHERE item_id = ? AND warehouse_id = ? AND moved_at >= ?
 ORDER BY moved_at DESC
```

복합 인덱스의 컬럼 순서가 중요 — `item_id → warehouse_id → moved_at` 순서로 좁혀가는 게 자연스러움 (item 이 더 선택적).

### 인덱스 2: `(ref_type, ref_id)`

위 §ref_type 에서 다룬 역추적용. "GR-42 가 만든 행들" 조회.

---

## 🔥 정합성 검증 쿼리 — 시스템의 자기 검증

```sql
SELECT m.item_id, m.warehouse_id, SUM(m.qty_delta) AS ledger_qty, s.qty_on_hand
  FROM stock_movement m
  JOIN stock s ON s.item_id = m.item_id AND s.warehouse_id = m.warehouse_id
 GROUP BY m.item_id, m.warehouse_id, s.qty_on_hand
HAVING SUM(m.qty_delta) <> s.qty_on_hand
```

이 쿼리가 **0행** 이어야 한다. 행이 나오면:
- 코드 버그 (캐시/원장 동기화 깨짐) — 즉시 수정 대상.
- 사람 손으로 DB 직접 수정 (긴급 패치) — 다시 맞추는 보정 분개 필요.

→ Phase 9 (배치) 에서 야간 정합성 검증 배치로 격상시킬 수 있는 패턴. 지금은 통합 테스트가 매 시나리오 후 호출.

```java
// MmScenarioTest.원장_합계는_Stock_과_일치한다
BigDecimal ledgerSum = stockMovementRepository.findAll().stream()
        .filter(m -> m.getItem().getId().equals(ctx.itemId)
                && m.getWarehouse().getId().equals(ctx.warehouseId))
        .map(StockMovement::getQtyDelta)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

assertThat(stock.getQtyOnHand())
        .as("Stock.qty_on_hand == SUM(stock_movement.qty_delta)")
        .isEqualByComparingTo(ledgerSum);
```

→ 코드로 명세화된 시스템 약속.

---

## 🔥 조회 전용 API — 외부에서 직접 수정 불가

```
GET  /api/stocks                   목록
GET  /api/stocks/{id}              단건
GET  /api/stock-movements          이력 목록
GET  /api/stock-movements/{id}     이력 단건
```

POST/PUT/DELETE 가 **하나도 없다**. Stock 의 변경 길은 입고/출고/조정의 도메인 메서드뿐. 사람이 화면에서 직접 "현재 재고를 10대로 고쳐" 가 안 됨.

→ 캐시가 캐시인 이유. 외부에서 "현재 재고 = X" 라고 못 박으면 원장과 일치하지 않게 된다. **모든 변경은 이동 사건으로 표현** 해야 정합성이 유지.

> Phase 9 야간 실사 조정 배치가 도입될 때도 같은 원칙 — "실사로 7대였는데 시스템상 10대" 면 `ADJUSTMENT_MINUS` 이동 행으로 -3을 박는다. 캐시를 직접 7로 바꾸지 않는다.

---

## 🔥 Stock 의 부재 = 0 의 의미

```java
Stock stock = stockRepository
        .findByItemIdAndWarehouseId(item.getId(), warehouse.getId())
        .orElseGet(() -> stockRepository.save(Stock.empty(item, warehouse)));
```

(item, warehouse) 행이 **없으면 = qty 0** 으로 해석. `findByItem...` 이 `Optional.empty` 면 `Stock.empty(...)` 로 새 행 만들고 저장.

설계 선택:
- 매 (item, warehouse) 조합마다 시작부터 행을 INSERT 해 두는 방식 — 행 수 폭증 (item × warehouse 조합 모두).
- "행이 없으면 0" 으로 해석하고, 첫 입고 시 lazy 생성 — 행 수 절약 + 의미 동일.

→ 두 번째 채택. 빈 (item, warehouse) 의 의미가 무엇인지 코드 한 줄에 박혀 있다 (`Stock.empty(...)`).

---

## 🔥 한 트랜잭션 = 캐시 + 원장 동시 갱신

```java
// GoodsReceiptService.post 안에서 — 같은 @Transactional
Stock stock = stockRepository.findByItemIdAndWarehouseId(...)
        .orElseGet(() -> stockRepository.save(Stock.empty(...)));
stock.receive(qty, unitCost);                          // 캐시 변경
stockMovementRepository.save(StockMovement.of(...));   // 원장 추가
```

둘 다 같은 트랜잭션 안. 한쪽이 실패하면 둘 다 롤백. **정합성의 본질** 은 이 한 트랜잭션 약속.

만약 두 작업이 다른 트랜잭션이라면 — 캐시는 갱신됐는데 원장 적재 직전에 서버가 죽으면? 영구 미스매치. 같은 트랜잭션이라야 둘이 묶인 운명이 된다.

---

## 자기 점검

- [ ] Stock 과 StockMovement 의 책임 차이를 한 줄로 표현하면?
- [ ] `qty_on_hand` 캐시 없이 매번 SUM 으로 계산하는 설계는 왜 안 채택했나?
- [ ] `StockMovement` 모든 컬럼에 `updatable=false` 가 박힌 이유는?
- [ ] `ref_type/ref_id` 가 FK 가 아닌 이유 — 그 대가는?
- [ ] 캐시와 원장의 정합성 약속을 SQL 한 줄로 표현하면?
- [ ] Stock 의 변경 API 가 외부에 노출되지 않는 이유는?

---

이전 편 → [01-창고-마스터.md](./01-창고-마스터.md)
다음 편 → [03-비관적-락-출고.md](./03-비관적-락-출고.md)

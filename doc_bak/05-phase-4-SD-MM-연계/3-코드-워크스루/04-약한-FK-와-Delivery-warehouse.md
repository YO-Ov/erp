# 4/6. 약한 FK 와 모델 보강 — Long 식별자와 nullable FK

> 3편이 "왜 엔티티가 아니라 `Long` 이냐(모듈 의존 방향)" 를 다뤘다면, 이 편은 **그 `Long` 컬럼이 도메인 모델에서 어떻게 동작하나** 를 다룬다.
> 설계서 §4 "`GoodsIssue.delivery_id` — Delivery 참조의 방법" (세 옵션 비교) + §6.5 "`Delivery` 에 `warehouse_id` 추가" 의 실제 구현.

대상 파일:

```
hwlee-erp/src/main/java/com/hwlee/erp/
├─ mm/goodsissue/
│  ├─ GoodsIssue.java               (deliveryId 필드 + draftForDelivery 팩토리)
│  ├─ GoodsIssueRepository.java     (findByDeliveryId 역추적 쿼리)
│  └─ dto/
│     └─ GoodsIssueResponse.java    (deliveryId 노출)
└─ sd/delivery/
   ├─ Delivery.java                 (warehouseId 필드 + draft 시그니처 변경)
   └─ dto/
      └─ DeliveryCreateRequest.java (warehouseId 추가)
```

---

## 🔥 한 테이블, 두 출처 — `deliveryId` 가 nullable 인 이유

`GoodsIssue` 테이블에는 **성격이 다른 두 종류의 출고 행** 이 공존한다.

| 출처 | 생성 경로 | `delivery_id` | reason |
| --- | --- | --- | --- |
| 출하 연계 (자동) | 출하 확정 → MM 리스너가 생성 | 원천 Delivery id | `SHIPMENT` |
| 사용자 직접 등록 | `POST /api/goods-issues` (실사 조정/폐기 등) | `NULL` | 운영자가 선택 |

실제 컬럼 선언과 주석:

```java
/**
 * 이 출고가 비롯된 출하 ID (Phase 4). 출하 확정 시 자동 생성된 GI 만 채워지고,
 * 사용자가 직접 등록한 GI(실사 조정/폐기 등)는 {@code null} 이다.
 * DB 에는 {@code fk_goods_issue_delivery} FK 가 걸려 있지만, MM 이 SD 의 {@code Delivery}
 * 엔티티를 import 하지 않도록 Long 으로만 매핑한다(의존 방향 {@code MM → SD} 단방향).
 */
@Column(name = "delivery_id")
private Long deliveryId;
```

`nullable = false` 가 **없다**. 즉 nullable 이 기본. 이것이 설계서 §0 핵심 결정 #5, #6 의 결과 —

- **#5** `delivery_id` **nullable FK 컬럼 추가 (V21)**.
- **#6** 사용자 직접 만든 GI (`delivery_id = NULL`) 와 자동 GI 가 **한 테이블에 공존** → "nullable FK 의 효용. 두 출처를 단일 모델로".

> 💡 "출처가 다르면 테이블을 나눠야 하나?" 는 흔한 고민. 여기 답은 "아니오". **출고라는 사건의 본질은 같다** (창고에서 물건이 빠짐 → Stock 차감 → StockMovement(-)). 출처는 부가 정보일 뿐이라, `delivery_id` 라는 한 컬럼이 NULL 인지로 구분하면 충분하다. 차감/원장/취소 로직은 두 출처가 그대로 공유한다.

---

## 🔥 `draftForDelivery(...)` — 기존 `draft` 를 재사용하는 출하 전용 팩토리

수동 등록 GI 는 기존 `draft(...)` 로 만든다. 출하 연계 GI 만 새 팩토리 `draftForDelivery(...)` 로 만든다.

```java
public static GoodsIssue draft(String number, Warehouse warehouse, LocalDate issueDate, GoodsIssueReason reason) {
    if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
    if (warehouse == null) throw new IllegalArgumentException("warehouse 는 null 일 수 없다.");
    if (issueDate == null) throw new IllegalArgumentException("issueDate 는 null 일 수 없다.");
    if (reason == null) throw new IllegalArgumentException("reason 은 null 일 수 없다.");
    GoodsIssue gi = new GoodsIssue();
    gi.number = number;
    gi.warehouse = warehouse;
    gi.issueDate = issueDate;
    gi.reason = reason;
    return gi;
}

/**
 * 출하 연계 전용 팩토리 (Phase 4) — {@link #draft} 와 동일하되 원천 {@code deliveryId} 를 채운다.
 * reason 은 항상 {@link GoodsIssueReason#SHIPMENT}.
 */
public static GoodsIssue draftForDelivery(String number, Warehouse warehouse, LocalDate issueDate, Long deliveryId) {
    if (deliveryId == null) throw new IllegalArgumentException("deliveryId 는 null 일 수 없다.");
    GoodsIssue gi = draft(number, warehouse, issueDate, GoodsIssueReason.SHIPMENT);
    gi.deliveryId = deliveryId;
    return gi;
}
```

두 팩토리의 차이는 정확히 두 가지:

| | `draft` | `draftForDelivery` |
| --- | --- | --- |
| `reason` | 호출자가 인자로 지정 | **`SHIPMENT` 로 고정** (인자 없음) |
| `deliveryId` | 세팅 안 함 (NULL) | **인자로 받아 세팅** (NOT NULL 검증) |

설계 포인트 두 가지:

1. **재사용** — `draftForDelivery` 는 내부에서 `draft(...)` 를 그대로 호출한다 (`reason = SHIPMENT` 고정 전달). number/warehouse/issueDate 의 null 검증을 한 번만 작성. 출하 전용 팩토리는 그 위에 "`deliveryId` 받기 + null 검증" 만 얹는다. **공통 생성 규칙은 한 곳, 차이만 바깥에**.
2. **의도가 메서드명에 박힘** — 호출 측이 `draftForDelivery` 를 부르는 순간, 이 GI 가 출하에서 왔고 reason 은 `SHIPMENT` 이며 `deliveryId` 가 반드시 채워진다는 게 보장된다. "혹시 출하 GI 인데 reason 을 ADJUSTMENT 로 잘못 넣는" 실수 자체가 불가능.

> ⚠️ `deliveryId == null` 검증이 `draftForDelivery` 안에 있다. 출하 연계 GI 인데 `deliveryId` 가 NULL 이면 그건 "두 출처 구분" 이 깨진 상태 — 자동 GI 인데 NULL 이면 수동 GI 와 섞여버린다. 그래서 이 팩토리 경로에서는 NULL 을 막는다. 반대로 컬럼 자체는 nullable (수동 GI 를 위해).

---

## 🔥 `findByDeliveryId(Long)` — 출하 취소 시 역추적

출하를 취소하면 그 출하가 만든 GI 를 찾아 같이 취소해야 한다. 그 역추적이 이 한 줄이다.

```java
public interface GoodsIssueRepository
        extends JpaRepository<GoodsIssue, Long>, JpaSpecificationExecutor<GoodsIssue> {

    /** 출하 취소 시 원천 Delivery 로부터 자동 생성된 GI 를 역추적한다 (Phase 4). */
    Optional<GoodsIssue> findByDeliveryId(Long deliveryId);
}
```

- **Spring Data 파생 쿼리** — 메서드명 `findBy` + `DeliveryId` 컨벤션만으로 `SELECT ... WHERE delivery_id = ?` 가 자동 생성된다. JPQL 도, `@Query` 도 필요 없다. (3편의 `findForUpdate` 는 `@Lock` + JPQL 이 필요했지만, 여기는 단순 동등 비교라 파생만으로 충분.)
- **`Optional` 반환** — 한 Delivery 는 한 GI 와 1:1 (설계서 §4.2). 그래서 `List` 가 아니라 `Optional`. 없을 수도 있는 경우(이미 취소됨 등)를 `Optional` 로 표현.
- **인덱스가 받쳐줌** — V21 이 `idx_goods_issue_delivery` 인덱스를 같이 만든다(다음 편 V21/V22 참고). 역추적이 풀스캔이 아니라 인덱스 조회.

이 메서드의 존재 자체가 "강한 FK 채택(옵션 B)" 의 직접적 이유다. 취소 흐름이 빈번해서, **의도가 명확하고 빠른 역추적** 이 필요했기 때문.

---

## 🔥 세 옵션 비교 — 왜 강한 FK 였나 (설계서 §4.1)

`GoodsIssue` 가 Delivery 를 어떻게 가리킬지 세 가지를 비교했다.

| | 옵션 A — 약한 참조 | 옵션 B — 강한 FK ⭐ 채택 | 옵션 C — 매핑 테이블 |
| --- | --- | --- | --- |
| 형태 | `ref_type='DLV', ref_id=?` 두 컬럼, FK 없음 | `delivery_id BIGINT NULL` + FK | `delivery_goods_issue_link(delivery_id, goods_issue_id)` |
| 패턴 출처 | StockMovement 의 다형성 참조 | — | — |
| 역추적 | `WHERE ref_type='DLV' AND ref_id=?` (의도 흐려짐) | `WHERE delivery_id=?` (명확 + 인덱스) | JOIN 한 단계 |
| 정합성 | DB 가 보장 못 함 (dangling 가능) | **FK 가 강제** | FK 가 강제 |
| 확장성 | 새 출처 추가 시 컬럼 안 늘어남 | 출처 다양해지면 NULL FK 컬럼 늘어남 | 깨끗하지만 무거움 |

**옵션 B 채택 이유** (§4.2):

- **1:1 강한 관계** — Phase 4 의 SD↔MM 연계는 한 Delivery ↔ 한 GI. 다형성이 필요 없다.
- **취소 흐름이 빈번** — `findByDeliveryId` 역추적이 자주 일어남. FK 인덱스로 빠르게.
- **의도가 컬럼명에 박힘** — "이 GI 는 Delivery 에서 왔다" 가 `delivery_id` 한 컬럼으로 명확.

### StockMovement 의 약한 참조와는 성격이 다르다

설계서 §4.2 의 핵심 한 줄:

> 약한 참조 (StockMovement.ref_type) 와 강한 FK 는 **다른 성격**. StockMovement 는 다형성 (GR, GI, ADJ, 향후 더), GoodsIssue.delivery_id 는 단일 종속.

| | StockMovement | GoodsIssue.deliveryId |
| --- | --- | --- |
| 가리키는 대상 | **여러 종류** (GR/GI/ADJ, 향후 생산 등) | **한 종류** (Delivery 뿐) |
| 모델링 | `ref_type + ref_id` 다형성 약한 참조 | `delivery_id` 단일 강한 FK |
| 이유 | 원장이라 출처가 본질적으로 다양 | 1:1 종속이라 다형성 불필요 |

→ "StockMovement 가 약한 참조를 쓰니까 GI 도 똑같이" 가 **아니다**. 대상이 다형적이냐 단일 종속이냐에 따라 모델링이 갈린다. 같은 프로젝트 안에서도 두 패턴이 공존하는 게 정상.

> 💡 미래에 GI 의 출처가 더 다양해지면(예: Phase 8 생산 자재 소모) 그때 옵션 A(다형성 약한 참조)로 일반화를 검토한다(§4.1 단점). 지금 미리 일반화하지 않는 것 — **현재 요구가 1:1 이면 1:1 로 모델링** 하는 게 의도를 가장 잘 드러낸다.

---

## 🔥 `Delivery.warehouseId` — 모델 보강의 자리 (설계서 §6.5)

### 문제 — Phase 2 Delivery 엔티티엔 '출하지 창고' 가 없었다

출하는 어딘가의 창고에서 물건이 나가는 사건이다. 그런데 Phase 2 의 `Delivery` 모델에는 **어느 창고에서 나가는지** 정보가 없었다(§6.3, §6.5). 출하 자체는 SD 의 관심사라 창고를 안 잡았던 것.

이게 Phase 4 에서 문제가 된다 — MM 이 출고 GI 를 만들 때 **어느 창고의 Stock 을 차감할지** 를 특정할 수 없다. 차감 대상 창고를 모르면 출고가 성립 안 한다.

설계서 §6.4 의 미해결 주석이 바로 이 자리다:

```java
// ⭐ Phase 4 추가 — Delivery 가 어느 창고로 보낼지를 알아야 함
// → DeliveryCreateRequest 에 warehouseId 추가 또는
// → 어딘가에서 결정 (예: 항상 본사창고 default)
```

### 해결 — `warehouseId` (NOT NULL) 보강 (옵션 X)

설계서 §6.5 는 옵션 X(모델에 `warehouseId` 추가) vs 옵션 Y(`WH-HQ` 고정) 중 **X 를 채택**. 다중 창고가 ERP 의 정상 상태이고, Phase 3 시연에서 부산창고도 다뤘기 때문.

실제 필드:

```java
/**
 * 출하지 창고 ID (Phase 4). DB 에는 {@code fk_delivery_warehouse} FK 가 걸려 있지만,
 * SD 가 MM 의 {@code Warehouse} 엔티티를 import 하지 않도록 Long 으로만 매핑한다
 * (의존 방향 {@code MM → SD} 단방향 유지). 존재 검증은 출하 시점 MM 리스너가 수행.
 */
@Column(name = "warehouse_id", nullable = false)
private Long warehouseId;
```

`deliveryId` 와 대비된다 — 이쪽은 **`nullable = false`**. 출하지 창고가 없는 출하는 있을 수 없으니 NOT NULL 이 자연스럽다. (기존 행 backfill 은 다음 편 V22 에서.)

### `draft` 시그니처 변경

`warehouseId` 가 NOT NULL 이므로 생성 시점에 반드시 받아야 한다. 그래서 `Delivery.draft` 시그니처에 `warehouseId` 가 추가됐다:

```java
public static Delivery draft(String number, SalesOrder salesOrder, Long warehouseId, LocalDate shippedDate) {
    if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
    if (salesOrder == null) throw new IllegalArgumentException("salesOrder 는 null 일 수 없다.");
    if (warehouseId == null) throw new IllegalArgumentException("warehouseId 는 null 일 수 없다.");
    if (shippedDate == null) throw new IllegalArgumentException("shippedDate 는 null 일 수 없다.");
    Delivery d = new Delivery();
    d.number = number;
    d.salesOrder = salesOrder;
    d.warehouseId = warehouseId;
    d.shippedDate = shippedDate;
    return d;
}
```

그리고 입력 DTO `DeliveryCreateRequest` 에도 `warehouseId` 가 들어온다:

```java
public record DeliveryCreateRequest(
        @NotNull Long salesOrderId,
        @NotNull Long warehouseId,
        @NotNull LocalDate shippedDate,
        @NotEmpty @Valid List<DeliveryLineRequest> lines
) {}
```

`@NotNull` 로 컨트롤러 진입 단계에서 막고, 도메인 `draft` 가 한 번 더 막는다. 검증의 다층 — DTO 에서 형식/존재, 도메인에서 자기 불변식.

---

## 🔥 ⭐ 다시 강조 — 실제 구현은 엔티티가 아니라 `Long` + DB FK

이 편의 두 필드(`GoodsIssue.deliveryId`, `Delivery.warehouseId`)는 **둘 다 `Long` 타입** 이다. `Delivery` 객체도, `Warehouse` 객체도 아니다. 3편의 결론과 정확히 같은 이유 —

- `GoodsIssue.deliveryId` (MM) 가 `Delivery` (SD) 를 **객체로** 참조하면 MM → SD 코드 의존이 생긴다.
- `Delivery.warehouseId` (SD) 가 `Warehouse` (MM) 를 **객체로** 참조하면 SD → MM 코드 의존이 생겨, 이미 있는 MM → SD 와 합쳐 **양방향 순환** 이 된다.

그래서 둘 다 `Long` 컬럼 + DB FK 로만 구현했다. **DB 차원의 무결성은 FK 가 보장하되, Java 코드 차원의 모듈 의존은 끊는다**.

| | `GoodsIssue.warehouse` (Phase 3) | `GoodsIssue.deliveryId` (Phase 4) | `Delivery.warehouseId` (Phase 4) |
| --- | --- | --- | --- |
| Java 타입 | `Warehouse` (`@ManyToOne`) | `Long` | `Long` |
| 같은 모듈? | 둘 다 MM → 엔티티 OK | MM → SD (역방향) → Long | SD → MM (순환 위험) → Long |
| DB FK | 있음 | `fk_goods_issue_delivery` 있음 | `fk_delivery_warehouse` 있음 |

→ **같은 MM 안의 `Warehouse` 는 객체로 참조**(`@ManyToOne`), **모듈 경계를 넘는 참조만 `Long`**. 경계 안에서는 풍부한 객체 그래프를, 경계 밖에서는 식별자만.

### 그럼 존재 검증은 누가?

`Long` 만 들고 있으면 "그 창고/출하가 진짜 존재하는가" 를 JPA 가 로드 시점에 검증해주지 못한다. 그 검증은 **MM 리스너가 출하 시점에** 한다 — `Delivery.warehouseId` 를 받아 `warehouseRepository.findById(...)` 로 실제 `Warehouse` 를 조회(없으면 예외). 즉 컴파일 타임 타입 의존 없이, **런타임에 자기 모듈(MM) 안에서** 검증한다. (DB FK 도 마지막 보루로 정합성을 강제.)

> 💡 `findByDeliveryId` 도 같은 결. 취소 리스너가 `deliveryId(Long)` 만 받아 자기 모듈(MM)의 `GoodsIssueRepository` 로 GI 를 찾는다. 경계를 넘는 건 항상 `Long`, 그걸 객체로 푸는 건 받는 쪽 모듈의 책임.

---

## 🔥 `GoodsIssueResponse` — `deliveryId` 를 그대로 노출

```java
public record GoodsIssueResponse(
        Long id,
        String number,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        GoodsIssueStatus status,
        LocalDate issueDate,
        GoodsIssueReason reason,
        Long deliveryId,
        LocalDateTime postedAt,
        ...
) {}
```

`deliveryId` 가 응답에 그대로 노출된다. 클라이언트(또는 시연 확인)가 **이 GI 가 출하 연계인지(값 있음) 수동 등록인지(null)** 를 응답만 보고 판단할 수 있다.

- `warehouse` 는 객체 참조라 `warehouseId / warehouseCode / warehouseName` 3개로 풀어서 노출 (조회 편의).
- `deliveryId` 는 애초에 `Long` 이라 그대로 한 필드. 출하 상세가 필요하면 클라이언트가 `GET /api/deliveries/{deliveryId}` 로 따로 조회 — 모듈 경계가 API 응답에도 그대로 드러난다.

---

## 🔥 정리 — 두 필드, 한 원칙

| 필드 | 모듈 방향 | nullable | 의미 | 검증 위치 |
| --- | --- | --- | --- | --- |
| `GoodsIssue.deliveryId` | MM → SD | **nullable** | 출하 연계 GI 의 원천 (수동 GI 는 NULL) | `draftForDelivery` 에서 NOT NULL 강제 |
| `Delivery.warehouseId` | SD → MM | **NOT NULL** | 출하지 창고 (차감 대상) | `draft` + DTO + MM 리스너 findById |

같은 "경계를 넘는 참조 = `Long`" 원칙이지만, nullable 여부는 **도메인 의미** 가 결정한다 — 출처가 둘이라 NULL 을 허용해야 하면 nullable, 없으면 안 되는 정보면 NOT NULL.

---

## 자기 점검

- [ ] `GoodsIssue.deliveryId` 가 nullable 인데 `Delivery.warehouseId` 는 NOT NULL 인 이유는? (도메인 의미로 설명)
- [ ] `draftForDelivery` 가 기존 `draft` 와 다른 점 두 가지는? 왜 `draft` 를 안에서 재호출하는가?
- [ ] `findByDeliveryId` 가 `@Query` 없이 메서드명만으로 동작하는 이유는? 반환 타입이 `List` 가 아니라 `Optional` 인 이유는?
- [ ] 세 옵션(약한 참조/강한 FK/매핑 테이블) 중 B 를 고른 세 가지 이유는?
- [ ] StockMovement 의 `ref_type` 다형성 참조와 `GoodsIssue.deliveryId` 강한 FK 의 성격 차이는?
- [ ] 두 필드가 `Delivery`/`Warehouse` 객체가 아니라 `Long` 인 이유는? 그럼 "그 대상이 실재하는지" 는 누가 언제 검증하는가?

---

이전 편 → [03-패키지-방향-의존성.md](./03-패키지-방향-의존성.md)
다음 편 → [05-Flyway-V21-V22.md](./05-Flyway-V21-V22.md)

# 5/9. Item / Vendor 마스터 — Customer와 다른 점만

> 04편의 Customer 풀스택을 한 번 깊게 봤으니, 여기서는 **차이점에만 집중**한다.
> 같은 부분(`@SQLDelete`, `BaseEntityWithCode` 상속, `@Transactional(readOnly)`, MapStruct, ...)은 04편 참조.

대상 파일:

```
master/item/      → Item.java, ItemCategory, ItemUnit, Service/Controller/Specifications/Mapper/Repository/DTO 3종
master/vendor/    → Vendor.java, Service/Controller/Mapper/Repository/DTO 3종
```

---

## 🔥 Item 의 핵심 차이점

### 1. 사업자번호가 없다, 대신 카테고리/단위/원가/판매가

```java
@Entity
public class Item extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    private ItemCategory category;        // NOTEBOOK / MONITOR

    @Enumerated(EnumType.STRING)
    private ItemUnit unit;                // EA / BOX / KG

    @Column(name = "standard_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal standardCost;      // 원가

    @Column(name = "standard_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal standardPrice;     // 판매가
}
```

**Customer 와의 결정적 차이**: 외부 식별자가 없다. 사업자번호 같은 외부에서 부여한 UNIQUE 식별자가 없으니, `code` 가 유일한 비즈니스 식별자다.

→ 그래서 `ItemService.create(...)` 에 **중복 체크 분기가 없다**:

```java
@Transactional
public ItemResponse create(ItemCreateRequest req) {
    String code = codeGenerator.nextCode(CODE_PREFIX);   // 바로 코드 발급
    Item item = Item.create(...);
    return mapper.toResponse(repository.save(item));
}
```

Customer는 `existsByBusinessNo(...)` 가 먼저였는데, Item은 그게 필요 없다 — 코드 충돌은 02편의 `CodeGenerator` 가 동시성 안전하게 보장.

### 2. `standardCost` vs `standardPrice` 두 개의 돈 컬럼

ERP에서 매우 중요한 구분:

| 필드 | 의미 | 누가 보는가 |
|---|---|---|
| `standardCost` | 표준 원가 — 사들이거나 만드는 데 드는 비용 | 회계(FI), 생산(PP) |
| `standardPrice` | 표준 판매가 — 고객에게 파는 정가 | 영업(SD), 견적 |

둘 다 **표준**이라는 단어가 붙은 이유: 실제 거래에서는 거래처별/고객별 단가가 따로 잡혀서 다를 수 있다. 마스터에 박힌 값은 "기본값/참조값" 의 역할. Phase 2에서 수주를 만들 때 이 값이 어떻게 흘러가는지 보게 된다.

### 3. 도메인 검증의 "의도적 제약 없음"

```java
private static void validate(...) {
    ...
    if (standardPrice == null || standardPrice.signum() < 0) {
        throw new IllegalArgumentException("standardPrice 는 0 이상이어야 한다.");
    }
    // 비즈니스 규칙: standardPrice >= standardCost 는 Phase 5 에서 정책 결정.
    // 실무에서는 손해 판매도 가능하므로 일단 검증하지 않는다.
}
```

자연스러운 가정 — "판매가가 원가보다 커야 하지 않나?" 를 **굳이 검증하지 않는다**. 주석에 이유를 명시:

- 실무에서는 **재고 떨이, 손해 판매(loss leader)** 같은 합법적 케이스가 많다.
- 정책으로 강제하려면 별도 결재 흐름이 필요한데, Phase 5(FI) 이후가 더 자연스럽다.

→ "비즈니스 규칙을 너무 일찍 코드에 박지 않는다" 는 학습 포인트. 검증을 안 한 이유가 코드 안에 명시되어 있다는 것도 좋은 습관(생략된 검증은 종종 버그처럼 보이지만, 명시된 비-검증은 의도임을 알 수 있음).

### 4. Specifications 의 차이 — `categoryEquals`

```java
public static Specification<Item> categoryEquals(ItemCategory category) {
    if (category == null) return null;
    return (root, query, cb) -> cb.equal(root.get("category"), category);
}
```

Customer는 `businessNoEquals` 였고, Item은 `categoryEquals`. 도메인이 다르니 검색축이 다르다. 패턴은 04편과 동일.

### 5. `ItemController.search` — 패턴은 똑같다

```java
@GetMapping
public Page<ItemResponse> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) ItemCategory category,   // ← businessNo 대신
        @RequestParam(required = false) MasterStatus status,
        Pageable pageable
) {
    return service.search(
            where(nameContains(name)).and(categoryEquals(category)).and(statusEquals(status)),
            pageable
    );
}
```

쿼리 파라미터 하나만 다를 뿐, **Specification 합성 → 페이징** 의 골격은 똑같다. Phase 1의 4개 마스터(Department 제외) 모두 이 골격을 공유.

---

## 🔥 Vendor 의 핵심 차이점

### 1. Customer 의 거의 미러 이미지, 그러나 `creditLimit` 이 없다

```java
@Entity
public class Vendor extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "business_no", nullable = false, unique = true, length = 20)
    private String businessNo;

    @Column(name = "address", length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms", nullable = false, length = 30)
    private PaymentTerms paymentTerms;
}
```

Customer 와의 정확한 차이는 **`creditLimit` 가 없다**는 것. 왜?

```
[Customer]                          [Vendor]
우리가 외상으로 팔아준다       우리가 외상으로 사들여 온다
→ 받을 돈 = 매출채권              → 줄 돈 = 매입채무
→ 거래처(고객) 의 신용을         → 우리 자신의 신용을
  우리가 평가해야 함               거래처가 평가함
→ creditLimit 컬럼 필요          → creditLimit 컬럼 불필요
```

**신용한도는 채권 발생 방향에 따라 결정된다**. 우리가 돈을 못 받을 위험이 있는 쪽(Customer)만 신용한도를 둔다.

> 도메인 브리핑에서 이미 짚었던 SD↔MM 의 비대칭이 여기에 컬럼 차이로 나타난다.

### 2. `PaymentTerms` 를 `customer` 패키지에서 그대로 import

```java
import com.hwlee.erp.master.customer.PaymentTerms;
```

Customer 와 Vendor 는 같은 결제 조건 enum 을 공유한다. **공유 enum을 어디에 둘지** 가 살짝 고민거리:

- `common/` 에 둠 → 깔끔하지만 도메인 색깔이 옅어짐
- `customer/` 에 둠 (현재) → 처음 등장한 곳에 두고 다른 도메인이 import
- 별도 `master/shared/` 패키지 → 도메인 수가 늘어나면 정리

Phase 1 은 도메인 수가 적어서 "처음 등장한 곳에 두기" 로 시작. 만약 Phase 2~3 에서 다른 도메인이 또 이 enum 을 쓰기 시작하면 그때 `common/` 또는 `master/shared/` 로 옮기는 게 좋다. **YAGNI** (You Aren't Gonna Need It) 원칙대로 일단 가장 가까운 곳에 두는 셈.

### 3. `VendorController.search` — 작은 다른 점

```java
@GetMapping
public Page<VendorResponse> search(...) {
    Specification<Vendor> spec = Specification.allOf(
            nameContains(name),
            businessNoEquals(businessNo),
            statusEquals(status)
    );
    return service.search(spec, pageable);
}

private static Specification<Vendor> nameContains(String keyword) { ... }
private static Specification<Vendor> businessNoEquals(String businessNo) { ... }
private static Specification<Vendor> statusEquals(MasterStatus status) { ... }
```

두 가지 작은 차이가 있다:

1. **`Specification.allOf(...)`** 를 사용 (Spring Data 3.4+ 에서 추가됨). Customer 의 `where(...).and(...).and(...)` 와 의미는 같지만 조금 더 읽기 좋다. null 인 spec 은 자동으로 무시.
2. **Specification 빌더를 컨트롤러 안에 private static 으로 둠** — 별도 `VendorSpecifications.java` 파일을 만들지 않았다.

Customer 가 외부 파일에 두고 Vendor 가 컨트롤러 내부에 둔 건 살짝 일관성이 깨진 부분이다. 둘 다 같은 패턴으로 가는 게 깔끔하지만, 학습 단계에서 두 가지 표현 방식을 동시에 보여주는 의도이기도 함:

- **별도 파일** (Customer): 재사용 가능, 테스트 격리
- **컨트롤러 내부** (Vendor): 작은 마스터에서는 한 파일에 두는 게 응집도 ↑

> 실무에서는 한 쪽으로 통일하는 게 맞다. Phase 2에서 QueryDSL로 전환할 때 정리될 가능성이 높다.

### 4. `VendorService.create` — Customer 와 똑같이 중복 체크

```java
@Transactional
public VendorResponse create(VendorCreateRequest req) {
    if (repository.existsByBusinessNo(req.businessNo())) {
        throw new IllegalStateException("이미 등록된 사업자번호입니다: " + req.businessNo());
    }
    String code = codeGenerator.nextCode(CODE_PREFIX);
    Vendor vendor = Vendor.create(code, req.name(), req.businessNo(), req.address(), req.paymentTerms());
    return mapper.toResponse(repository.save(vendor));
}
```

`CODE_PREFIX = "VEND"` 만 다르고 패턴은 Customer 와 완전 동일.

---

## 🔥 3개 마스터의 코드 prefix 정리

| 도메인 | prefix | 코드 예시 |
|---|---|---|
| Customer | `CUST` | `CUST-2026-0001` |
| Item | `ITEM` | `ITEM-2026-0001` |
| Vendor | `VEND` | `VEND-2026-0001` |
| Employee (07편) | `EMP` | `EMP-2026-0001` |
| Department (06편) | (수동 입력) | `SALES` , `RND` 등 |

각 서비스의 `CODE_PREFIX` 상수가 `CodeGenerator.nextCode(...)` 에 들어가서 `code_sequence` 테이블의 (prefix, year) 행을 가른다. 즉:

```
code_sequence
+----+--------+------+-------------+
| id | prefix | year | next_number |
+----+--------+------+-------------+
| 1  | CUST   | 2026 | 43          |
| 2  | ITEM   | 2026 | 12          |
| 3  | VEND   | 2026 | 7           |
| 4  | EMP    | 2026 | 5           |
+----+--------+------+-------------+
```

각 prefix 마다 독립된 행이 있어, 동시성 락도 도메인별로 격리된다 — Customer 발급 중에 Item 발급은 대기하지 않음.

---

## 🔥 패턴 추출 — "복사 가능한 마스터 템플릿"

Customer / Item / Vendor 3개를 비교하면 패턴이 거의 동일하다. **새 마스터가 필요할 때 다음 골격을 따른다**:

```
master/<도메인>/
├─ <Entity>.java            — BaseEntityWithCode 상속, @SQLDelete, 정적 팩토리 + update + validate
├─ <Repository>.java        — JpaRepository + JpaSpecificationExecutor, findByCode, [existsByXxx]
├─ <Service>.java           — @Transactional(readOnly), CODE_PREFIX 상수, CRUD 6종
├─ <Controller>.java        — @RestController, 6개 엔드포인트, Pageable + Specification 합성
├─ <Specifications>.java    — final class, static 메서드, null=조건 없음
├─ <Mapper>.java            — MapStruct, toResponse 만
└─ dto/
    ├─ <Create>Request.java
    ├─ <Update>Request.java
    └─ <Response>.java
```

**도메인마다 달라지는 것**:
1. 필드 (사업자번호 / 카테고리 / 단위 / 원가 / 판매가 / 신용한도)
2. 검증 로직 (도메인 invariant)
3. 검색축 (`categoryEquals` vs `businessNoEquals`)
4. `CODE_PREFIX` 상수
5. 중복 체크 유무 (외부 식별자 있으면 `existsByXxx`)

→ 이 표를 외워두면 06편(Department) 와 07편(Employee) 가 "왜 다른가" 가 명확해진다.

---

## 자기 점검

- [ ] Vendor 에 `creditLimit` 가 없는 도메인 이유를 한 줄로?
- [ ] Item 의 `create` 에 중복 체크가 없는 이유?
- [ ] `standardCost` 와 `standardPrice` 가 둘 다 "표준" 이라는 단어를 달고 있는 이유?
- [ ] `Specification.allOf(...)` 와 `where(...).and(...)` 의 차이는?
- [ ] 새로운 마스터(예: Warehouse)를 추가한다면 위 템플릿에서 무엇이 달라지는가?

---

이전 편 → [04-Customer-마스터-풀스택.md](./04-Customer-마스터-풀스택.md)
다음 편 → [06-Department-자기참조-트리.md](./06-Department-자기참조-트리.md)

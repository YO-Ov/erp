# 4/9. Customer 마스터 — 풀스택 워크스루

> **Phase 1의 핵심 편**. Customer를 Entity ~ Controller 까지 한 번에 짚는다.
> Item / Vendor / Department / Employee 는 이 패턴을 변주하므로, 여기를 깊게 잡으면 나머지가 가벼워진다.

대상 파일 (10개):

```
hwlee-erp/src/main/java/com/hwlee/erp/master/customer/
├─ Customer.java                  (Entity)
├─ CustomerRepository.java        (Spring Data JPA)
├─ CustomerService.java           (트랜잭션 경계)
├─ CustomerController.java        (REST)
├─ CustomerMapper.java            (MapStruct)
├─ CustomerSpecifications.java    (동적 쿼리)
├─ PaymentTerms.java              (enum)
└─ dto/
    ├─ CustomerCreateRequest.java
    ├─ CustomerUpdateRequest.java
    └─ CustomerResponse.java
```

---

## 🔥 레이어 한 눈에

```
        ┌──────────────────────────────────────┐
HTTP →  │ CustomerController                   │ ← DTO 검증, URI 변환
        │   @RestController                    │
        └────────────────┬─────────────────────┘
                         │ DTO
                         ▼
        ┌──────────────────────────────────────┐
        │ CustomerService                      │ ← 트랜잭션 경계, 도메인 호출
        │   @Service @Transactional(readOnly)  │
        └────────┬─────────────────────────┬───┘
                 │                         │
                 ▼                         ▼
        ┌──────────────────┐    ┌───────────────────┐
        │ CustomerRepository│    │ CodeGenerator     │ ← 02편
        │  (JpaRepo +       │    │ (REQUIRES_NEW)    │
        │   Specification)  │    └───────────────────┘
        └────────┬─────────┘
                 │ JPA
                 ▼
        ┌──────────────────────────────────────┐
        │ Customer (Entity, BaseEntityWithCode 상속) │
        │   create()/update() 도메인 메서드        │
        └──────────────────────────────────────┘
                 │
                 ▼
              DB: customer 테이블
```

---

## 🔥 1. `Customer.java` — 도메인 엔티티

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "customer")
@SQLDelete(sql = "UPDATE customer SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Customer extends BaseEntityWithCode {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "business_no", nullable = false, unique = true, length = 20)
    private String businessNo;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms", nullable = false, length = 30)
    private PaymentTerms paymentTerms;
    ...
}
```

### 🔥 `@SQLDelete` + `@SQLRestriction` — Soft Delete 의 마법

```java
@SQLDelete(sql = "UPDATE customer SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
```

이 두 줄이 Soft Delete 의 전부다.

**`@SQLDelete`**: Hibernate 가 `customerRepository.delete(entity)` 를 받으면 보통 `DELETE FROM customer WHERE id = ?` 를 발사하는데, 이 어노테이션이 있으면 **그 SQL을 통째로 교체**한다.

```
service.delete(id)              [Spring Data]
  → repository.delete(customer) [Hibernate]
  → 실제 SQL: UPDATE customer SET deleted_at = NOW() WHERE id = ?
```

→ 서비스 코드는 `repository.delete(...)` 만 부르면 자동으로 Soft Delete 가 된다. `CustomerService.delete(...)` 한 줄짜리 메서드를 다시 보자:

```java
@Transactional
public void delete(Long id) {
    Customer customer = getOrThrow(id);
    repository.delete(customer); // @SQLDelete 에 의해 Soft Delete 로 동작
}
```

진짜로 한 줄이다. 도메인 규칙이 어노테이션에 흡수되어 비즈니스 코드가 단순해지는 좋은 예.

**`@SQLRestriction`**: 모든 SELECT 쿼리에 자동으로 `AND deleted_at IS NULL` 을 붙여준다.

```java
repository.findById(42)
  → 실제 SQL: SELECT ... FROM customer WHERE id = 42 AND deleted_at IS NULL
```

→ 삭제된 행은 어떤 조회에서도 안 나온다. 서비스 코드가 매번 `WHERE deleted_at IS NULL` 을 신경 쓰지 않아도 됨.

> ⚠️ 단점: 정말로 삭제된 행을 조회하고 싶을 때는 native query 를 써야 한다. Phase 1에서는 복구 API를 안 만들기로 했으므로 (설계서 §7) 이 단점이 문제가 되지 않는다.

### 🔥 정적 팩토리 `create(...)` — "어떻게 만드는지" 를 한 군데에 모은다

```java
public static Customer create(String code, String name, String businessNo, String address,
                              BigDecimal creditLimit, PaymentTerms paymentTerms) {
    validate(name, businessNo, creditLimit, paymentTerms);
    Customer c = new Customer();
    c.assignCode(code);
    c.name = name;
    ...
    return c;
}
```

`new Customer()` + setter 반복 대신 한 메서드로 생성을 통과시키면:

1. 검증을 강제할 수 있다 (`validate(...)` 호출).
2. 필수 필드를 빠뜨리는 실수를 줄인다.
3. **`assignCode(...)` 를 통과해야** code 가 박힌다 → 01편의 "코드 1회 할당" 규칙이 작동.
4. setter 가 없으니 외부에서 다르게 만들 방법이 없다.

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
```

기본 생성자는 JPA 리플렉션용으로만 열어두고 외부에서는 못 부르게 막는다 (02편 `CodeSequence` 와 동일 패턴). → **모든 생성은 `Customer.create(...)` 를 지나야 한다**.

### 🔥 `update(...)` — Setter 대신 의미 있는 메서드

```java
public void update(String name, String address, BigDecimal creditLimit, PaymentTerms paymentTerms) {
    if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("name 은 비어 있을 수 없다.");
    }
    ...
    this.name = name;
    this.address = address;
    this.creditLimit = creditLimit;
    this.paymentTerms = paymentTerms;
    // 비즈니스 규칙: business_no 는 수정 불가 (외부 식별자).
}
```

- **`businessNo` 가 인자에 없다** → 수정 메서드가 사업자번호를 받지 않으니 변경 자체가 불가능.
- **`code` 도 인자에 없다** → 01편의 `updatable = false` 와 짝.
- 검증을 한 곳에 모은다.

이게 "엔티티가 자기 상태를 책임진다" 의 실제 모습.

### `BigDecimal creditLimit` + `precision/scale`

```java
@Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
private BigDecimal creditLimit;
```

돈은 절대 `double` 로 다루지 않는다 — 부동소수점 오차 때문. `BigDecimal` 이 표준.

- `precision = 15` → 전체 15자리 (예: 9999999999999.99)
- `scale = 2` → 소수점 2자리 (원 단위까지)

ERP 학습에서 **돈 관련 컬럼은 무조건 `BigDecimal + precision/scale`** 이라는 습관을 들이는 게 좋다.

---

## 🔥 2. `PaymentTerms.java` — 도메인 enum

```java
public enum PaymentTerms {
    NET30,   // 거래일로부터 30일 안에 결제
    NET60,   // 60일
    COD,     // Cash On Delivery (현금 결제)
    PREPAID  // 선결제
}
```

ERP 약식 표기를 그대로 따른다. 굳이 한국어로 안 바꾼 이유 — **이 표기는 국제 표준에 가까워서** 회계 시스템, ERP 벤더 문서, 거래처 합의문에서 그대로 쓰인다. 도메인 언어를 유지하는 게 학습 가치가 더 크다.

`@Enumerated(EnumType.STRING)` (01편 참조) 로 DB에는 문자열로 박혀 컬럼만 봐도 의미를 안다.

---

## 🔥 3. `CustomerRepository.java` — Spring Data JPA

```java
public interface CustomerRepository
        extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByCode(String code);

    boolean existsByBusinessNo(String businessNo);

    Page<Customer> findAll(Specification<Customer> spec, Pageable pageable);
}
```

### 두 개의 인터페이스를 동시에 상속

- `JpaRepository<Customer, Long>` → save / findById / delete / findAll / count 등 표준 CRUD
- `JpaSpecificationExecutor<Customer>` → `findAll(Specification, Pageable)` 등 동적 쿼리

→ 정적 쿼리는 `JpaRepository` 의 메서드 이름 파싱, 동적 쿼리는 `Specification` 이라는 두 갈래.

### `findByCode` / `existsByBusinessNo` — 메서드 이름 파싱

Spring Data 가 메서드 이름을 보고 SQL을 자동 생성한다:

```
findByCode             → SELECT ... FROM customer WHERE code = ? AND deleted_at IS NULL
existsByBusinessNo     → SELECT 1 FROM customer WHERE business_no = ? AND deleted_at IS NULL LIMIT 1
```

`AND deleted_at IS NULL` 이 자동으로 붙는 이유는 `@SQLRestriction` 덕분.

### `existsByBusinessNo` 가 왜 `Optional<Customer>` 가 아닌 `boolean` 인가

```java
boolean existsByBusinessNo(String businessNo);
```

`CustomerService.create(...)` 에서 중복 체크용으로만 쓴다:

```java
if (repository.existsByBusinessNo(req.businessNo())) {
    throw new IllegalStateException("이미 등록된 사업자번호입니다: " + req.businessNo());
}
```

→ 엔티티가 필요 없으니 `Optional<Customer>` 가 아닌 `boolean` 이 맞다. SQL도 가벼움 (`SELECT 1 ... LIMIT 1`). 작은 디테일이지만 **필요한 만큼만 가져온다** 는 원칙.

---

## 🔥 4. `CustomerSpecifications.java` — 동적 조건 빌더

```java
public final class CustomerSpecifications {

    private CustomerSpecifications() {}

    public static Specification<Customer> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(root.get("name"), "%" + keyword + "%");
    }

    public static Specification<Customer> businessNoEquals(String businessNo) { ... }
    public static Specification<Customer> statusEquals(MasterStatus status) { ... }
}
```

### 핵심 규약 — null 반환

`keyword` 가 비어 있으면 `null` 을 돌려준다. 컨트롤러에서:

```java
where(nameContains(name))
    .and(businessNoEquals(businessNo))
    .and(statusEquals(status))
```

이런 식으로 체이닝할 때, **null 반환은 "조건 없음" 으로 해석**되어 SQL의 WHERE 절에서 자연스럽게 빠진다. 즉 `name=null, businessNo=null, status=null` 이면 전체 조회.

→ 컨트롤러에서 if 분기로 동적 쿼리를 짜는 대신, **Specification 합성**으로 깔끔하게 표현된다.

### 람다 안의 `root, query, cb`

```java
return (root, query, cb) -> cb.like(root.get("name"), "%" + keyword + "%");
```

- `root` → FROM 절의 엔티티 ("FROM customer")
- `query` → 전체 CriteriaQuery (정렬, distinct 등에 접근 가능)
- `cb` → CriteriaBuilder (조건/함수 생성기)

이게 곧 JPA Criteria API. 컴파일 타임에 타입 체크가 되지만 가독성이 떨어진다는 게 약점. **Phase 2 부터 QueryDSL** 로 전환하는 이유.

### `final class + private constructor`

```java
public final class CustomerSpecifications {
    private CustomerSpecifications() {}
    public static Specification<Customer> ...
}
```

**유틸리티 클래스 (static 메서드만 모음) 패턴**. `new` 못 하고 상속도 못 하는 닫힌 클래스. 도구 모음이라는 의도를 클래스 선언으로 못 박는다.

---

## 🔥 5. `CustomerService.java` — 트랜잭션 경계

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    static final String CODE_PREFIX = "CUST";

    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final CodeGenerator codeGenerator;
    ...
}
```

### 🔥 클래스에 `readOnly = true`, 쓰기 메서드만 `@Transactional` 으로 override

```java
@Transactional(readOnly = true)        // 클래스 기본값
public class CustomerService { ... }

@Transactional                          // 쓰기 메서드만 override
public CustomerResponse create(...) { ... }

@Transactional                          // override
public CustomerResponse update(...) { ... }

@Transactional                          // override
public void delete(...) { ... }
```

**왜 readOnly = true 가 기본인가**: 조회 메서드가 대다수다. `readOnly` 가 켜져 있으면:

1. Hibernate 가 1차 캐시의 **dirty checking 을 끄거나 줄인다** → 약간 빨라짐.
2. 일부 DB 드라이버는 readonly 트랜잭션을 read replica 로 라우팅할 수 있음 (운영 단계 이점).
3. 실수로 조회 메서드에서 데이터 수정하는 코드가 들어가도 INSERT/UPDATE 가 안 날아감 → 안전망.

→ **"기본은 안전한 쪽, 필요할 때만 풀어준다"** 는 패턴.

### 🔥 `create(...)` 전체 흐름

```java
@Transactional
public CustomerResponse create(CustomerCreateRequest req) {
    if (repository.existsByBusinessNo(req.businessNo())) {
        throw new IllegalStateException("이미 등록된 사업자번호입니다: " + req.businessNo());
    }
    String code = codeGenerator.nextCode(CODE_PREFIX);    // ← 02편의 REQUIRES_NEW
    Customer customer = Customer.create(
            code,
            req.name(), req.businessNo(), req.address(),
            req.creditLimit(), req.paymentTerms()
    );
    Customer saved = repository.save(customer);
    return mapper.toResponse(saved);
}
```

순서가 정해진 이유:

1. **사전 체크** → DB가 UNIQUE 위반으로 잡기 전에 친절한 메시지로 먼저 막는다.
   (Race condition 으로 DB에서 잡힐 가능성은 여전히 있음 — `GlobalExceptionHandler` 의 `DataIntegrityViolation` 핸들러가 안전망.)
2. **코드 발급** (02편 `REQUIRES_NEW`). 별도 트랜잭션이라 이 메서드가 실패해도 발급된 번호는 이미 커밋됨.
3. **엔티티 생성** — 도메인 규칙 검증은 `Customer.create(...)` 가 책임.
4. **저장** + **DTO 변환** → 응답.

레이어 책임 분담이 깔끔하다:
- 서비스: 트랜잭션 + 외부 협업 (`CodeGenerator`)
- 엔티티: 도메인 invariant
- 매퍼: DTO 변환

### `EntityNotFoundException` 사용

```java
private Customer getOrThrow(Long id) {
    return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: id=" + id));
}
```

JDK + jakarta 의 표준 예외다. 직접 만든 `CustomerNotFoundException` 이 아니라 표준을 쓰는 이유는 03편에서 짚었다 — `GlobalExceptionHandler` 가 이 예외를 404로 변환.

---

## 🔥 6. `CustomerController.java` — REST 엔드포인트

```java
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest req) {
        CustomerResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/customers/" + created.id())).body(created);
    }

    @GetMapping("/{id}")            public CustomerResponse findById(@PathVariable Long id) { ... }
    @GetMapping("/by-code/{code}")  public CustomerResponse findByCode(@PathVariable String code) { ... }

    @GetMapping
    public Page<CustomerResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) MasterStatus status,
            Pageable pageable
    ) { ... }

    @PutMapping("/{id}")    public CustomerResponse update(...) { ... }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(...) { ... }
}
```

### 6개 엔드포인트 — 설계서 §7.1 의 표준 패턴

| HTTP | 경로 | 역할 |
|---|---|---|
| POST | `/api/customers` | 생성 |
| GET | `/api/customers/{id}` | 단건 조회 (PK) |
| GET | `/api/customers/by-code/{code}` | 단건 조회 (비즈니스 코드) |
| GET | `/api/customers?name=&status=&...` | 목록 + 검색 + 페이징 |
| PUT | `/api/customers/{id}` | 수정 |
| DELETE | `/api/customers/{id}` | Soft Delete |

다른 마스터(Item/Vendor/Department/Employee)도 동일한 6개 패턴을 따른다.

### 🔥 `@Valid` — DTO 검증 트리거

```java
public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest req)
```

`@Valid` 가 붙은 DTO 필드의 `@NotBlank`, `@NotNull`, `@PositiveOrZero` 등이 작동한다. 위반 시 `MethodArgumentNotValidException` → 03편의 핸들러가 400 + fieldErrors 응답으로 변환.

### `ResponseEntity.created(URI...).body(...)` — 201 + Location 헤더

```java
return ResponseEntity.created(URI.create("/api/customers/" + created.id())).body(created);
```

REST 모범 사례 — 생성 응답은 `201 Created` + `Location: /api/customers/42` 헤더 + body. 클라이언트는 헤더만 보고 새 리소스의 URL을 안다.

다른 엔드포인트는 그냥 객체를 리턴하면 Spring 이 알아서 200 + body 로 처리해준다.

### `search(...)` 의 Specification 합성

```java
@GetMapping
public Page<CustomerResponse> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String businessNo,
        @RequestParam(required = false) MasterStatus status,
        Pageable pageable
) {
    return service.search(
            where(nameContains(name)).and(businessNoEquals(businessNo)).and(statusEquals(status)),
            pageable
    );
}
```

위에서 본 Specifications 가 합쳐지는 자리. 빈 키워드 → `null` 반환 → WHERE 절에서 빠짐. 컨트롤러는 if 분기 없이 깨끗하게 합성만 한다.

### `Pageable` 을 그냥 받는다

Spring 이 query string 의 `page`, `size`, `sort` 를 알아서 `Pageable` 로 묶어준다.

```
GET /api/customers?name=한빛&page=0&size=20&sort=createdAt,desc
```

→ Pageable 에 page=0, size=20, sort=createdAt desc 가 자동으로 들어옴.

---

## 🔥 7. `CustomerMapper.java` — MapStruct

```java
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer entity);
}
```

### 인터페이스인데 동작하는 이유

MapStruct 가 **컴파일 시점에 구현 클래스를 자동 생성**한다 (`CustomerMapperImpl`). build 폴더를 까보면 실제 생성된 코드를 볼 수 있는데 대략:

```java
public class CustomerMapperImpl implements CustomerMapper {
    @Override
    public CustomerResponse toResponse(Customer entity) {
        if (entity == null) return null;
        return new CustomerResponse(
                entity.getId(), entity.getCode(), entity.getName(),
                entity.getBusinessNo(), entity.getAddress(), entity.getCreditLimit(),
                entity.getPaymentTerms(), entity.getStatus(),
                entity.getCreatedAt(), entity.getCreatedBy(),
                entity.getUpdatedAt(), entity.getUpdatedBy()
        );
    }
}
```

→ **리플렉션이 아니라 진짜 코드**. 컴파일 타임 검증이 되니까 필드 이름이 안 맞으면 빌드가 깨진다(런타임 NPE가 아니라). `componentModel = "spring"` 은 생성된 클래스에 `@Component` 를 붙여서 Spring 빈으로 등록되게 해줌.

### 왜 ModelMapper / 수동 변환이 아닌가

- ModelMapper: 런타임 리플렉션 기반 → 느리고 타입 안전성 ↓
- 수동 변환: 정확하지만 필드 추가 시 빠뜨릴 위험 ↑
- **MapStruct**: 컴파일 타임 + 코드 생성 = 빠르고 안전

### `toEntity` 가 없는 이유

```java
// 없다 — toResponse 만 있음
```

Create/Update 요청은 **엔티티의 정적 팩토리 / 도메인 메서드를 직접 부르는 게 낫다**:

```java
Customer.create(code, req.name(), req.businessNo(), ...)   // 도메인 검증 통과
customer.update(req.name(), req.address(), ...)             // 비즈니스 규칙 적용
```

MapStruct로 자동 변환하면 검증을 우회하기 쉽다. → **DTO→엔티티는 의도적으로 수동**, **엔티티→DTO만 자동화**.

---

## 🔥 8. DTO 3종

### `CustomerCreateRequest`

```java
public record CustomerCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 20) String businessNo,
        @Size(max = 500) String address,
        @NotNull @PositiveOrZero BigDecimal creditLimit,
        @NotNull PaymentTerms paymentTerms
) {}
```

- `record` 라서 자동으로 불변 + getter (`name()`, `businessNo()` …) + equals/hashCode.
- `address` 만 `@NotBlank` 가 없음 → 주소는 옵션.

### `CustomerUpdateRequest` — `businessNo` 가 없다

```java
public record CustomerUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String address,
        @NotNull @PositiveOrZero BigDecimal creditLimit,
        @NotNull PaymentTerms paymentTerms
) {}
```

설계 정책이 **수정 불가 필드는 DTO에서 아예 제거** 라서 사업자번호 칸이 없다.

→ 클라이언트가 사업자번호를 바꾸려 해도 그 필드 자체가 없어서 보낼 곳이 없음. **API 모양으로 도메인 규칙을 강제**.

### `CustomerResponse` — 응답 12 필드

```java
public record CustomerResponse(
        Long id, String code,
        String name, String businessNo, String address,
        BigDecimal creditLimit, PaymentTerms paymentTerms,
        MasterStatus status,
        LocalDateTime createdAt, String createdBy,
        LocalDateTime updatedAt, String updatedBy
) {}
```

엔티티의 거의 모든 필드를 그대로. `deletedAt` 만 빠짐 — 어차피 살아있는 행만 조회되니까(`@SQLRestriction`) 의미가 없음.

응답 DTO를 둠으로써 **엔티티가 외부에 그대로 노출되는 걸 차단**한다. 예를 들어 비즈니스 사정으로 엔티티에 내부용 필드가 추가돼도 응답 DTO만 안 바꾸면 API 계약이 그대로 유지된다.

---

## 🔥 시나리오 — `POST /api/customers` 전체 흐름

```
[Client]
POST /api/customers
{
  "name": "한빛컴퍼니",
  "businessNo": "123-45-67890",
  "address": "서울 강남구",
  "creditLimit": 10000000,
  "paymentTerms": "NET30"
}
   │
   ▼
[CustomerController.create]
   1. @Valid → CustomerCreateRequest 검증 (위반 시 400)
   2. service.create(req) 호출
   │
   ▼
[CustomerService.create]              ← @Transactional (RW)
   3. existsByBusinessNo("123-45-67890") → false
   4. codeGenerator.nextCode("CUST")    ← REQUIRES_NEW
        → SELECT FOR UPDATE code_sequence
        → next_number=43 → 44 로 증가
        → 반환: "CUST-2026-0043"
   5. Customer.create("CUST-2026-0043", "한빛컴퍼니", ...)
        → assignCode("CUST-2026-0043")  (1회 할당 검사)
        → validate(...)                  (도메인 검증)
   6. repository.save(customer)
        → INSERT INTO customer ...
        → AuditingEntityListener 가 created_at/by, updated_at/by 자동 채움
   7. mapper.toResponse(saved)
   │
   ▼
[CustomerController]
   8. 201 Created + Location: /api/customers/{id} + body
   │
   ▼
[Client] CustomerResponse 받음
```

여기에 02편의 코드 생성, 03편의 Auditing + 예외 처리, 01편의 BaseEntity 상속이 **모두 한 요청 안에서 합쳐진다**. 1~3편이 4편으로 수렴하는 구조.

---

## 자기 점검

- [ ] `@SQLDelete` + `@SQLRestriction` 이 만들어내는 SQL 변환을 두 줄로 쓸 수 있는가?
- [ ] `@Transactional(readOnly = true)` 를 클래스에 기본값으로 두는 이유 3가지?
- [ ] `CustomerMapper` 에 `toEntity` 메서드가 없는 이유?
- [ ] `CustomerUpdateRequest` 에 `businessNo` 가 없는 이유? 이게 도메인 규칙을 어떻게 강제하는가?
- [ ] `Specification` 합성에서 null 반환이 의미하는 것은?

---

이전 편 → [03-Auditing-공통-인프라.md](./03-Auditing-공통-인프라.md)
다음 편 → [05-Item-Vendor.md](./05-Item-Vendor.md)

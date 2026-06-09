# 9/9. 테스트 — 동시성 + CRUD 통합 테스트

> Phase 1 의 마지막 편. 테스트가 단순히 "버그 잡기" 가 아니라 **시스템이 보장하는 업무 규칙의 살아있는 문서** 라는 관점에서 본다.

대상 파일:

```
hwlee-erp/src/test/java/com/hwlee/erp/
├─ common/code/CodeGeneratorConcurrencyTest.java     (동시성 1편)
└─ master/
    ├─ customer/CustomerCrudIntegrationTest.java      (5개 시나리오)
    ├─ item/ItemCrudIntegrationTest.java              (3개)
    ├─ vendor/VendorCrudIntegrationTest.java          (3개)
    ├─ department/DepartmentCrudIntegrationTest.java  (3개)
    └─ employee/EmployeeCrudIntegrationTest.java      (3개)
```

총 18개 테스트. 모두 `@SpringBootTest` + Testcontainers MySQL 환경에서 진짜 DB를 띄워서 검증.

---

## 🔥 테스트가 명세 (설계서 §10.3) — 메서드명을 보면 업무 규칙이 읽힌다

먼저 모든 테스트의 `@DisplayName` 만 모아 보자. 이게 바로 **Phase 1 의 명세서**:

```
[동시성]
- 코드 생성은 동시 요청에서도 중복되지 않는다

[Customer]
- 신규 고객을 생성하면 CUST-YYYY-NNNN 형식의 코드가 자동 발급된다
- 사업자번호가 중복되면 생성이 거부된다
- 삭제된 고객은 일반 조회에서 보이지 않는다
- 고객을 수정하면 updated_at 과 업데이트된 값이 반영된다
- 코드로 조회할 수 있다

[Item]
- 상품 생성시 ITEM-YYYY-NNNN 코드가 발급된다
- 표준 판매가가 표준 원가보다 낮아도 등록을 막지 않는다
- 삭제된 상품은 일반 조회에서 안 보인다

[Vendor]
- 거래처 생성시 VEND 코드가 발급된다
- 거래처 사업자번호 중복 등록은 거부된다
- 삭제된 거래처는 조회되지 않는다

[Department]
- 자식 부서는 부모 부서 코드로 연결된다
- 부서 코드 중복 등록은 거부된다
- 존재하지 않는 부모 코드로 생성하면 예외가 발생한다

[Employee]
- 직원 생성시 EMP 코드가 발급되고 부서가 연결된다
- 이메일 중복 등록은 거부된다
- 존재하지 않는 부서 코드는 거부된다
```

→ **테스트 이름만 읽어도 시스템이 보장하는 규칙이 한 눈에 들어온다**. 코드 변경 시 깨지는 테스트 = 변경하려는 업무 규칙.

---

## 🔥 1. `CodeGeneratorConcurrencyTest` — Phase 1의 가장 중요한 테스트

```java
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CodeGeneratorConcurrencyTest {

    private static final String PREFIX = "TEST";

    @Autowired CodeGenerator codeGenerator;
    @Autowired CodeSequenceRepository repository;
    @Autowired Clock clock;
    ...
}
```

### 왜 이게 중요한가

02편의 `CodeGenerator` 가 동시성 안전한지 **눈으로는 확인할 수 없다**. 코드를 읽고 "락이 걸려 있으니 안전할 거야" 라고 믿을 뿐. 동시성 버그는 **단일 스레드 테스트로는 절대 잡히지 않는다**.

→ 50개 스레드를 동시에 발사해서 **진짜로** 중복이 안 나는지를 검증해야 한다.

### `@BeforeEach` — 시퀀스 행 미리 만들기

```java
@BeforeEach
void seedSequenceRow() {
    int year = LocalDate.now(clock).getYear();
    repository.findAll().stream()
            .filter(s -> s.getPrefix().equals(PREFIX) && s.getYear() == year)
            .findFirst()
            .ifPresentOrElse(s -> {}, () -> repository.save(CodeSequence.initial(PREFIX, year)));
}
```

이 테스트는 **"행이 이미 있을 때 락이 동시성을 보장하는가"** 를 검증한다. 즉 02편의 두 단계 동시성 중 **1단계 (락)** 만 본다. 행이 없는 상태에서 동시 INSERT (2단계, catch 분기) 는 별도 테스트가 다룬다고 javadoc 에 명시:

> 운영에서 행 초기화는 부팅 시 한 번만 발생하는 이벤트이고, 그 충돌 처리는 별도 단위 테스트 (CodeGeneratorInitRaceTest) 에서 다룬다.

→ 한 테스트 = 한 시나리오. 동시성 검증을 여러 단계로 쪼개야 디버깅도 쉽다.

### `CountDownLatch` — 동시 발사 게이트

```java
CountDownLatch startGate = new CountDownLatch(1);

List<Future<String>> futures = IntStream.range(0, threads).mapToObj(i -> pool.submit(() -> {
    startGate.await();                          // ← 모든 스레드가 여기서 대기
    return codeGenerator.nextCode(PREFIX);
})).toList();

startGate.countDown();                          // ← 한 번에 풀어줌
```

스레드 풀에 작업 50개를 제출하면 자연스럽게 **시간차** 가 생긴다 (제출 시점이 다름). 락이 약하더라도 시간차로 인해 우연히 충돌이 안 날 수 있음.

`CountDownLatch(1)` 을 게이트로 써서 **모든 스레드를 같은 순간에 출발**시킨다. 이래야 진짜 동시 경합 상태를 만들 수 있다.

### `pool.awaitTermination(60, TimeUnit.SECONDS)` — 데드락 안전망

```java
boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);

assertThat(finished).as("모든 스레드가 60초 안에 끝나야 한다").isTrue();
```

만약 락 로직에 버그가 있어서 **데드락**이 걸리면, `awaitTermination` 이 60초 뒤 false 를 돌려준다. 그러면 테스트가 무한 hang 하지 않고 "60초 안에 안 끝났다" 라는 명확한 실패 메시지로 죽음.

→ **동시성 테스트는 반드시 타임아웃을 두어야 한다**. 안 그러면 CI가 영원히 멈춤.

### 핵심 검증 — Set 의 크기

```java
Set<String> issued = new HashSet<>();
for (Future<String> f : futures) {
    issued.add(f.get());
}
assertThat(issued)
        .as("발급된 코드는 모두 unique 해야 한다 (락이 없으면 여기서 깨진다)")
        .hasSize(threads);
```

50번 발사 → 50개의 결과 → **Set 에 넣었을 때 크기도 50** 이어야 함. 한 번이라도 중복 발급이 일어났다면 Set 의 size 가 50보다 작아진다.

`.as("...락이 없으면 여기서 깨진다")` — 실패 메시지에 "왜 깨졌는지" 를 미리 적어둔 게 좋은 습관. 미래에 이 테스트가 깨졌을 때 메시지만 보고 원인 후보를 떠올릴 수 있다.

---

## 🔥 2. `CustomerCrudIntegrationTest` — 풀스택 통합 테스트

```java
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CustomerCrudIntegrationTest {

    @Autowired CustomerService service;
    @Autowired CustomerRepository repository;
    ...
}
```

### `@SpringBootTest` + Testcontainers — "진짜" 환경

여기서 진짜로 일어나는 일:

1. Testcontainers 가 도커로 MySQL 컨테이너를 실제 띄움
2. Flyway 가 V1~V8 마이그레이션을 모두 적용
3. Spring Boot 가 풀 컨텍스트를 부팅 (Service, Repository, Controller 다)
4. `service.create(...)` 가 실제로 DB에 INSERT

→ Mock 없이 **운영과 거의 같은 환경**에서 검증. 학습 단계에서 매우 큰 가치가 있다 — "동작한다" 가 진짜인지 의심 없음.

대가: 첫 번째 테스트가 컨테이너 부팅 비용으로 5~10초 걸림. 하지만 학습 단계에선 이 비용이 받아들일 만하다.

### 🔥 코드 형식을 정규식으로 검증

```java
assertThat(created.code()).matches("CUST-\\d{4}-\\d{4}");
```

`"CUST-2026-0001"` 처럼 **하드코딩으로** `equals("CUST-2026-0001")` 비교하면 시드 데이터가 바뀌거나 연도가 바뀌면 테스트가 깨진다. 정규식 매칭으로 **형식의 약속**만 검증.

→ 테스트가 환경에 종속되지 않으면서도 의도한 규칙을 검증.

### 🔥 Auditing 검증 — `system` 이 자동으로 박혔는지

```java
assertThat(created.createdBy()).isEqualTo("system");
assertThat(created.updatedBy()).isEqualTo("system");
```

03편의 `JpaAuditingConfig` + `AuditorAware = "system"` 가 진짜로 작동하는지 확인. 코드 한 줄 안 부르고 자동으로 박혔는지를 본다.

### 🔥 사업자번호 중복 거부 시나리오

```java
@Test
@DisplayName("사업자번호가_중복되면_생성이_거부된다")
void 사업자번호가_중복되면_생성이_거부된다() {
    String dup = uniqueBusinessNo();
    service.create(new CustomerCreateRequest("회사A", dup, null,
            BigDecimal.ZERO, PaymentTerms.NET30));

    assertThatThrownBy(() ->
            service.create(new CustomerCreateRequest("회사B", dup, null,
                    BigDecimal.ZERO, PaymentTerms.NET30))
    ).isInstanceOf(IllegalStateException.class)
     .hasMessageContaining("이미 등록된 사업자번호");
}
```

같은 사업자번호로 두 번 등록을 시도. 두 번째에서 `IllegalStateException` 이 나는 것까지 확인.

`hasMessageContaining("이미 등록된 사업자번호")` — 예외 메시지에 의미 있는 단어가 포함되는지도 확인. 이게 03편 핸들러에서 409 응답으로 변환되는데, 그 위 단계까지는 통합 테스트가 안 검증한다 (web 계층 통합은 별도).

### 🔥 Soft Delete 검증 — `findById` 후 다시 못 찾는 것

```java
@Test
@DisplayName("삭제된_고객은_일반_조회에서_보이지_않는다")
void 삭제된_고객은_일반_조회에서_보이지_않는다() {
    CustomerResponse created = service.create(...);
    Long id = created.id();

    service.delete(id);

    assertThatThrownBy(() -> service.findById(id))
            .isInstanceOf(EntityNotFoundException.class);

    boolean stillVisible = service.search(null, PageRequest.of(0, 1000))
            .stream()
            .anyMatch(c -> c.id().equals(id));
    assertThat(stillVisible).isFalse();
}
```

이 한 테스트가 **`@SQLDelete` + `@SQLRestriction` 의 짝이 같이 작동하는지** 검증한다 (04편).

- `service.delete(id)` → `@SQLDelete` 가 UPDATE 로 변환했는가?
- `service.findById(id)` → `@SQLRestriction` 이 `deleted_at IS NULL` 필터를 자동으로 붙였는가?
- 페이징 목록에서도 빠졌는가?

세 단계가 모두 한 시나리오에 묶여 있다. **연관된 어노테이션들의 협동**을 한 번에 검증.

### 🔥 수정 시 사업자번호 불변 검증

```java
// business_no 는 수정 불가 정책 — Request DTO 에 필드 자체가 없음
assertThat(updated.businessNo()).isEqualTo(created.businessNo());
```

04편의 `CustomerUpdateRequest` 에 `businessNo` 필드가 없는 게 도메인 규칙이라고 짚었다. 이 테스트는 그 규칙을 명시적으로 확인 — "수정 후에도 사업자번호가 그대로다".

### 🔥 `uniqueBusinessNo()` 헬퍼 — 테스트 격리

```java
private static int counter = 1;

private static synchronized String uniqueBusinessNo() {
    return String.format("%03d-%02d-%05d", 100 + counter, counter % 100, counter++ + 10000);
}
```

여러 테스트가 같은 DB 컨테이너를 공유한다. 모든 테스트가 같은 사업자번호를 쓰면 두 번째 테스트부터 UNIQUE 위반으로 깨진다. → **각 테스트가 고유한 값을 받아쓰는 헬퍼**.

`synchronized` 로 카운터를 보호 (병렬 테스트에서도 안전).

> 더 멋진 방법은 `@DirtiesContext` 같은 격리 메커니즘인데, 학습 단계에서는 단순한 카운터로 충분.

---

## 🔥 3. `ItemCrudIntegrationTest` — 의도적 비검증 검증

```java
@Test
@DisplayName("표준_판매가가_표준_원가보다_낮아도_등록을_막지_않는다")
void 표준_판매가가_원가보다_낮아도_등록을_막지_않는다() {
    // Phase 1 정책: 손해 판매 가능성을 인정하고 검증하지 않는다.
    ItemResponse created = service.create(new ItemCreateRequest(
            "행사용 노트북", ItemCategory.NOTEBOOK, ItemUnit.EA,
            new BigDecimal("1000000.00"),    // cost
            new BigDecimal("900000.00")      // price (cost 보다 낮음)
    ));

    assertThat(created.standardPrice()).isEqualByComparingTo(new BigDecimal("900000.00"));
    assertThat(created.standardCost()).isEqualByComparingTo(new BigDecimal("1000000.00"));
}
```

특이한 테스트. **검증 안 하는 것을 검증** 한다. 05편의 Item.validate 에 "price >= cost 는 일부러 검증하지 않는다" 라고 적었던 부분.

→ 누가 미래에 "어 이거 버그 같은데?" 하면서 검증을 추가하면 이 테스트가 깨진다. **의도된 비검증**임을 코드 + 테스트 양쪽에서 명시하는 패턴.

### `isEqualByComparingTo` — BigDecimal 비교의 함정

```java
assertThat(created.standardPrice()).isEqualByComparingTo(new BigDecimal("900000.00"));
```

`isEqualTo` 가 아니라 `isEqualByComparingTo`. 왜?

```java
new BigDecimal("900000.00").equals(new BigDecimal("900000"))   // false! (scale 다름)
new BigDecimal("900000.00").compareTo(new BigDecimal("900000")) == 0  // true
```

`BigDecimal.equals` 는 **스케일까지 본다** — `900000.00` 과 `900000` 이 다르다. 의도하는 건 값만 같은지이므로 `compareTo` 기반의 `isEqualByComparingTo` 를 써야 함.

→ ERP 처럼 돈을 다루는 코드에선 **BigDecimal 비교는 무조건 `compareTo` / `isEqualByComparingTo`**.

---

## 🔥 4. `DepartmentCrudIntegrationTest` — 시드 데이터 활용

```java
@Test
@DisplayName("자식_부서는_부모_부서_코드로_연결된다")
void 자식_부서는_부모_부서_코드로_연결된다() {
    // V8 시드로 이미 DEPT-HQ 와 5개 하위 부서가 존재한다.
    DepartmentResponse sales = service.findByCode("DEPT-SALES");

    assertThat(sales.parentCode()).isEqualTo("DEPT-HQ");
    assertThat(sales.name()).isEqualTo("영업팀");
}
```

**V8 시드 데이터를 그대로 활용**한다. 따로 부서를 만들지 않고, 시드된 `DEPT-SALES` 가 `DEPT-HQ` 를 부모로 갖는지 확인.

→ 마이그레이션이 잘 적용됐는지 + 자기참조 매핑이 잘 풀렸는지를 한 번에 검증.

### 부모 부재 검증

```java
@Test
@DisplayName("존재하지_않는_부모_코드로_생성하면_예외가_발생한다")
void 존재하지_않는_부모_코드로_생성하면_예외() {
    assertThatThrownBy(() ->
            service.create(new DepartmentCreateRequest("DEPT-RND", "연구소", "DEPT-NOWHERE"))
    ).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
}
```

06편의 `resolveParent` 가 부모를 못 찾으면 `EntityNotFoundException` 을 던지는 동작 검증. 03편의 핸들러가 이걸 404로 변환할 텐데, 거기까진 통합 테스트의 책임 밖.

---

## 🔥 5. `EmployeeCrudIntegrationTest` — 시드 + 신규의 결합

```java
@Test
@DisplayName("직원_생성시_EMP_코드가_발급되고_부서가_연결된다")
void 직원_생성시_EMP_코드_발급_및_부서_연결() {
    EmployeeResponse e = service.create(new EmployeeCreateRequest(
            "신입사원-" + System.nanoTime(),
            "new" + System.nanoTime() + "@hwlee-erp.example",
            "DEPT-SALES",
            LocalDate.of(2026, 5, 20)
    ));

    assertThat(e.code()).matches("EMP-\\d{4}-\\d{4}");
    assertThat(e.departmentCode()).isEqualTo("DEPT-SALES");
    assertThat(e.departmentName()).isEqualTo("영업팀");
}
```

**시드된 `DEPT-SALES` 에 신규 직원을 추가**하는 통합 시나리오. 07편의 `departmentName` 비정규화 응답까지 검증한다 (`e.departmentName()`).

### `System.nanoTime()` 으로 unique 만들기

```java
"신입사원-" + System.nanoTime()
"new" + System.nanoTime() + "@hwlee-erp.example"
```

테스트 격리를 위해 매번 다른 값을 쓰는 또 다른 패턴. Customer/Vendor 는 `counter++` 헬퍼였고, Employee 는 `nanoTime` 을 직접 박음. 둘 다 일관성이 있으면 더 좋겠지만 효과는 같다.

---

## 🔥 6. `VendorCrudIntegrationTest` — Customer 와 거의 동일

```java
@Test
@DisplayName("거래처_생성시_VEND_코드가_발급된다")
void 거래처_생성시_VEND_코드가_발급된다() {
    VendorResponse v = service.create(new VendorCreateRequest(
            "삼우전자부품", uniqueBusinessNo(), "수원시", PaymentTerms.NET30));
    assertThat(v.code()).matches("VEND-\\d{4}-\\d{4}");
}
```

Customer 테스트의 거의 미러. 05편에서 정리한 것처럼 도메인 패턴이 유사하면 테스트도 유사하다.

### Vendor 의 `uniqueBusinessNo()` — Customer 와 충돌 회피

```java
private static synchronized String uniqueBusinessNo() {
    return String.format("%03d-%02d-%05d", 200 + counter, counter % 100, counter++ + 20000);
}
```

Customer 는 `100 + counter` 를, Vendor 는 `200 + counter` 를 쓴다. 두 도메인이 같은 DB의 다른 테이블이라 사실 중복돼도 문제 없긴 하지만, 시드 / 마이그레이션과 우연히 같은 값이 나오지 않게 의도적으로 분리.

---

## 🔥 테스트 분포 표 — 한 눈에

| 파일 | 테스트 수 | 핵심 검증 |
|---|---|---|
| `CodeGeneratorConcurrencyTest` | 1 | 50스레드 동시 발급 → 중복 0 |
| `CustomerCrudIntegrationTest` | 5 | 코드 발급 / 중복 거부 / Soft Delete / 수정 불변필드 / 코드 조회 |
| `ItemCrudIntegrationTest` | 3 | 코드 발급 / **의도된 비검증** (price<cost OK) / Soft Delete |
| `VendorCrudIntegrationTest` | 3 | 코드 발급 / 중복 거부 / Soft Delete |
| `DepartmentCrudIntegrationTest` | 3 | 자기참조 / 코드 중복 거부 / 부모 부재 |
| `EmployeeCrudIntegrationTest` | 3 | 코드 발급 + FK / 이메일 중복 거부 / 부서 부재 |

총 18개. 단위 테스트는 동시성 1개뿐이고 나머지는 모두 통합 — 이게 설계서 §10 정책 ("핵심 로직만 단위, 도메인 규칙은 통합") 의 실제 구현.

---

## 🔥 한 줄 정리 — 테스트가 알려주는 Phase 1의 약속

```
1. 코드는 동시 요청에서도 절대 중복되지 않는다 (CodeGenerator)
2. 사업자번호 / 이메일 / 부서코드 등 외부 식별자 중복은 거부된다
3. Soft Delete 된 행은 어떤 일반 조회에도 안 나타난다
4. created_by / updated_by 는 자동으로 "system" 으로 채워진다
5. 마스터별 코드 형식이 자동 발급된다 (CUST/ITEM/VEND/EMP-YYYY-NNNN)
6. 부서 / 직원의 부모/소속이 존재하지 않으면 거부된다
7. 사업자번호 같은 외부 식별자는 수정 불가
8. 의도적으로 검증하지 않는 규칙(price >= cost 등) 도 명시적
```

→ 이 8가지가 Phase 1 의 실제 산출물. 마이그레이션, 엔티티, 서비스, 컨트롤러 모두 이 8가지를 지키기 위한 인프라.

---

## 자기 점검

- [ ] `CountDownLatch(1)` 게이트가 없으면 동시성 테스트가 어떻게 신뢰성을 잃는가?
- [ ] 동시성 테스트에 `awaitTermination(60, SECONDS)` 가 반드시 필요한 이유?
- [ ] `isEqualTo` 대신 `isEqualByComparingTo` 를 BigDecimal 에 쓰는 이유?
- [ ] `assertThat(created.code()).matches("CUST-\\d{4}-\\d{4}")` 같은 정규식 매칭이 하드코딩 비교보다 유리한 점?
- [ ] "검증 안 하는 것을 검증" 하는 테스트(`표준_판매가가_원가보다_낮아도_등록`) 가 왜 가치가 있는가?

---

이전 편 → [08-Flyway-마이그레이션-V2-V8.md](./08-Flyway-마이그레이션-V2-V8.md)

🎉 **Phase 1 코드 워크스루 완료**. 다음은 7단계 시연 + 회고 (`4-시연-가이드.md`).

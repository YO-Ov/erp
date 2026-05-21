# 7/9. Employee 마스터 — Department FK 연결

> Phase 1 의 마지막 마스터. **다른 마스터(Department)를 외래키로 참조**하는 케이스. Phase 6에서 인증 사용자로 진화할 예비 자리이기도 함.

대상 파일:

```
master/employee/
├─ Employee.java                    (Department ManyToOne)
├─ EmployeeRepository.java
├─ EmployeeService.java             (DepartmentRepository 협력)
├─ EmployeeController.java
└─ dto/
    ├─ EmployeeCreateRequest.java
    ├─ EmployeeUpdateRequest.java
    └─ EmployeeResponse.java
```

---

## 🔥 다른 마스터들과의 비교 한 표

|  | Customer/Item/Vendor | Department | **Employee** |
|---|---|---|---|
| 코드 | 자동 (CUST/ITEM/VEND) | 수동 (DEPT-*) | **자동 (EMP)** |
| 부모 관계 | 없음 | 자기참조 | **다른 마스터(Department) 참조** |
| 목록 | 페이징 | 전체 List | **전체 List** (Phase 1 한정) |
| 외부 식별자 | 사업자번호 | 코드만 | **이메일** |

→ **자동 코드는 Customer/Item/Vendor 와 같고, 부모 참조는 Department 의 자기참조와 비슷, 목록은 Department와 같음**. 전편들의 패턴이 조합된 모습.

---

## 🔥 1. Department 를 ManyToOne 으로 참조

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "department_id", nullable = false)
private Department department;
```

### 06편의 자기참조와의 차이

| | Department (06편) | Employee (07편) |
|---|---|---|
| 가리키는 대상 | 같은 테이블 | 다른 테이블 |
| nullable | YES (루트 부서) | **NO** (직원은 무조건 부서 소속) |
| `optional` | (기본 true) | **false** |

`optional = false` 가 두 가지 효과:

1. **JPA 수준**: 해당 연관관계가 항상 존재한다고 보장 → `LEFT OUTER JOIN` 대신 `INNER JOIN` 으로 페치 가능.
2. **컬럼 수준**: `nullable = false` 와 짝을 이뤄 DB가 NOT NULL을 강제.

→ **"직원은 반드시 부서에 속한다"** 라는 도메인 규칙이 어노테이션 두 개로 코드에 박힘.

### `FetchType.LAZY` 는 여기서도 기본

```java
@ManyToOne(fetch = FetchType.LAZY, ...)
```

`ManyToOne` 의 기본 fetch 는 `EAGER` 다. 그냥 두면 직원을 조회할 때마다 부서가 항상 JOIN 되어 SELECT. 100명 조회 = 100명 + 부서 JOIN.

LAZY로 명시해야 필요할 때만 부서를 가져온다. 다만 `toResponse(...)` 에서 `e.getDepartment().getCode()` 를 항상 부르므로 결과적으로는 N+1 이 발생한다(06편의 끝에서 짚은 패턴).

→ Phase 2의 QueryDSL/JOIN FETCH 도입에서 진정한 해결이 일어남. 지금은 의식만 하고 넘김.

---

## 🔥 2. `email` 이 외부 식별자

```java
@Column(name = "email", nullable = false, unique = true, length = 200)
private String email;
```

Customer 의 `business_no` 위치에 Employee 는 `email` 이 있다. 이메일이 UNIQUE 라는 게 도메인 의미를 갖는다:

- ERP 사용자 식별자 — 사번이 아니라 이메일이 사람 단위 식별
- **Phase 6 에서 그대로 로그인 ID** 가 됨 (`AuditorAware` 가 이 값을 채우게 됨)

→ 그래서 Phase 1 에서부터 UNIQUE + 형식 검증을 둔다:

```java
public record EmployeeCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 200) String email,   // ← @Email 형식 검증
        @NotBlank String departmentCode,
        @NotNull LocalDate hireDate
) {}
```

`@Email` 어노테이션이 RFC 5322 패턴으로 형식을 검증한다. 이상한 문자열이 들어와도 컨트롤러에서 막힘.

### 이메일 변경은 허용

```java
public void update(String name, String email, Department department, LocalDate hireDate) {
    validate(name, email, department, hireDate);
    this.name = name;
    this.email = email;       // ← 수정 가능
    ...
}
```

`businessNo` 가 수정 불가였던 것과 다르게 이메일은 수정을 허용한다. 직원이 회사 도메인 이메일을 바꾸는 일은 빈번하기 때문(개명, 통합 도메인 변경 등). 단, UNIQUE 제약이 걸려 있어서 다른 사람의 이메일로는 못 바꿈.

### `hireDate` — `LocalDate` (`LocalDateTime` 아님)

```java
@Column(name = "hire_date", nullable = false)
private LocalDate hireDate;
```

입사일은 "날짜" 만 의미 있고 시각은 의미 없다 → `LocalDate` 가 자연스러운 타입. ERP에서 회계/HR/계약 날짜 같은 건 대부분 `LocalDate`.

→ `created_at`/`updated_at` 같은 시스템 시간만 `LocalDateTime`. **타입이 곧 의미** — 타입을 보고 어떤 종류의 시간인지 안다.

---

## 🔥 3. `EmployeeService.create` — 두 협력자

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    static final String CODE_PREFIX = "EMP";

    private final EmployeeRepository repository;
    private final DepartmentRepository departmentRepository;   // ← 다른 도메인의 Repository
    private final CodeGenerator codeGenerator;

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest req) {
        if (repository.existsByEmail(req.email())) {
            throw new IllegalStateException("이미 등록된 이메일입니다: " + req.email());
        }
        Department department = resolveDepartment(req.departmentCode());
        String code = codeGenerator.nextCode(CODE_PREFIX);
        Employee employee = Employee.create(code, req.name(), req.email(), department, req.hireDate());
        return toResponse(repository.save(employee));
    }
    ...
}
```

### 다른 도메인의 Repository를 의존

```java
private final DepartmentRepository departmentRepository;
```

Employee 가 Department 를 참조하므로, 서비스가 Department 의 Repository를 직접 의존한다. **느슨한 결합** 측면에서는 `DepartmentService` 를 의존하는 게 더 좋지만(서비스 → 서비스), Phase 1 에서는 **Department 의 조회 로직이 단순**하고 추가적인 도메인 규칙이 없어서 Repository 직접 호출이 간단해서 그렇게 했다.

→ 만약 Department 조회에 "휴면 부서는 신규 직원을 받지 않는다" 같은 규칙이 생기면, 그때 `DepartmentService.findActiveByCode(...)` 같은 메서드를 만들어 거기로 이전한다. Phase 1 의 단순함 우선.

### `resolveDepartment` — 06편과 같은 패턴

```java
private Department resolveDepartment(String code) {
    return departmentRepository.findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException("Department not found: code=" + code));
}
```

DTO에서 `departmentCode` (문자열) 로 받아서 엔티티로 변환. 06편의 `resolveParent` 와 동일한 패턴이다. **부모/연관 엔티티는 항상 비즈니스 코드로 받고 서비스가 엔티티로 변환** 한다는 Phase 1의 일관된 약속.

→ Employee 의 `departmentCode` 가 없는 부서를 가리키면 `EntityNotFoundException` → 03편의 핸들러가 404 응답.

### `create` 의 순서

```
1. existsByEmail   → 중복 체크 (친절한 에러 메시지)
2. resolveDepartment → 부서 존재 확인 (없으면 404)
3. nextCode("EMP") → 코드 발급 (REQUIRES_NEW)
4. Employee.create → 도메인 검증 + 객체 생성
5. repository.save → INSERT
```

순서가 미묘하게 중요하다:
- **검증을 먼저 (1, 2)** 코드 발급 전에. 잘못된 입력으로 코드를 발급해버리면 02편의 "번호 구멍" 이 늘어남.
- 4번 `Employee.create` 의 `validate` 가 마지막 안전망. 1~2에서 못 잡은 invariant를 잡는다.

---

## 🔥 4. 응답 DTO — `departmentName` 도 포함

```java
public record EmployeeResponse(
        Long id, String code,
        String name, String email,
        String departmentCode,
        String departmentName,         // ← 비정규화
        LocalDate hireDate,
        MasterStatus status,
        LocalDateTime createdAt, String createdBy,
        LocalDateTime updatedAt, String updatedBy
) {}
```

응답에는 부서의 `code` 뿐 아니라 `name` 까지 같이 보낸다. **클라이언트가 화면에 부서명을 보여주려고 매번 부서 조회 API를 또 부르지 않게** 하는 작은 배려.

서비스의 `toResponse(...)` 가 이걸 채움:

```java
private EmployeeResponse toResponse(Employee e) {
    Department d = e.getDepartment();
    return new EmployeeResponse(
            ...,
            d.getCode(),    // departmentCode
            d.getName(),    // departmentName
            ...
    );
}
```

### N+1 다시 한 번

`findAll()` 응답을 만들 때 직원마다 `e.getDepartment().getName()` 을 호출 → 직원이 100명이면 부서 SELECT가 100번. 06편에서 짚은 N+1이 그대로 재현됨.

해법은:
- `@EntityGraph(attributePaths = "department")` 를 `EmployeeRepository.findAll` 에 붙임
- DTO Projection (직원과 부서의 필요한 컬럼만 JOIN 쿼리로 한 번에 SELECT)

Phase 1 의 시드 데이터 규모(직원 ~5명)에서는 문제가 안 되지만, 실무 의식 차원에서 짚어둔다. Phase 2부터 본격 도입.

### MapStruct 없이 수동 변환 (Department와 같은 이유)

```java
e.getDepartment().getCode()
```

조건부 + 다른 엔티티 필드 접근이 들어가니 MapStruct 매핑 표현이 복잡해진다. 한 메서드 수동 변환이 더 명료.

---

## 🔥 5. Repository — `JpaSpecificationExecutor` 가 없다

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByCode(String code);
    boolean existsByEmail(String email);
}
```

Customer/Item/Vendor 와 다르게 `JpaSpecificationExecutor` 를 안 상속한다 — Phase 1 에서 동적 검색 화면이 없기 때문. 직원은 전체 조회만.

만약 Phase 6 에서 "이름으로 검색", "부서별 필터" 같은 요구가 생기면 그때 `JpaSpecificationExecutor` 추가하면 된다. **현재 요구만 만족시키는 코드** 라는 원칙.

---

## 🔥 6. Controller — 5종 (페이징 없음)

```java
POST   /api/employees
GET    /api/employees/{id}
GET    /api/employees/by-code/{code}
GET    /api/employees             ← Pageable 없음
PUT    /api/employees/{id}
DELETE /api/employees/{id}
```

설계서 §7.1 의 표준 6종 패턴은 유지하되, list 만 페이징 없음. Department 와 동일한 결정 — 회사 직원 규모가 보통 페이징이 필요할 정도는 아님 (수십~수백명).

→ 만약 직원이 1만 명 넘는 회사로 확장된다면 Phase 6/7 에서 페이징 추가하면 됨.

---

## 🔥 시드 데이터 — Phase 1 의 직원 트리

V8 마이그레이션에 들어가는 직원 예시(설계서 §9 의 시드 데이터):

```
직원                | 이메일                | 부서          | 입사일
-------------------|---------------------|--------------|------------
EMP-2026-0001 김본부 | hq@hwlee.com         | DEPT-HQ      | 2020-01-01
EMP-2026-0002 이영업 | sales1@hwlee.com     | DEPT-SALES-1 | 2022-03-15
EMP-2026-0003 박개발 | rnd1@hwlee.com       | DEPT-RND     | 2023-07-01
```

→ Employee 가 Department 의 행을 FK로 참조 → 마이그레이션은 무조건 **Department 먼저, Employee 나중**. 그래서 V6 → V7 순서가 강제된다.

---

## 🔥 4개 마스터의 의존 관계 정리

```
       Customer        Item        Vendor
        (독립)         (독립)       (독립)

       Department          ← 다른 마스터에 의존 없음
            │
            ▼ FK
       Employee            ← Department 에 의존
```

→ Phase 1 의 마이그레이션 순서는 이 의존성을 그대로 따라가야 한다 (08편에서 다룸).

---

## 🔥 Phase 6 의 진화 예고

설계서 §6.2 + 도메인 브리핑에서 짚은 부분. Phase 6 에서 Spring Security 가 들어오면:

```java
// Phase 1
@Bean
public AuditorAware<String> auditorProvider() {
    return () -> Optional.of("system");
}

// Phase 6
@Bean
public AuditorAware<String> auditorProvider() {
    return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName);   // ← 이게 Employee.email
}
```

이때 **Employee.email 이 곧 로그인 ID** 가 된다. 그래서 Phase 1 에서부터 이메일을 UNIQUE 로 박아두고 `@Email` 검증을 둔 것. **Phase 6에서 새로 만들 게 없도록** 미리 자리를 잡아둠.

---

## 자기 점검

- [ ] `optional = false` 와 `nullable = false` 가 각각 어느 레벨의 제약인가?
- [ ] Employee 가 Department의 Service가 아닌 Repository를 의존하는 이유? 어떤 상황에서 Service 의존으로 바꿔야 하는가?
- [ ] 응답 DTO에 `departmentName` 도 같이 담는 이유? 트레이드오프는?
- [ ] `hireDate` 가 `LocalDate` 인 이유?
- [ ] Phase 6 에서 어떤 한 줄만 바꿔도 직원이 시스템 사용자가 되는가?

---

이전 편 → [06-Department-자기참조-트리.md](./06-Department-자기참조-트리.md)
다음 편 → [08-Flyway-마이그레이션-V2-V8.md](./08-Flyway-마이그레이션-V2-V8.md)

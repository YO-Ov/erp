# 6/9. Department 마스터 — 자기참조 트리

> 4개 마스터 중 가장 결이 다른 도메인. **코드는 운영자가 직접 입력**하고, **자기 자신을 부모로 가질 수 있는 트리 구조**.

대상 파일:

```
master/department/
├─ Department.java                  (자기참조 ManyToOne)
├─ DepartmentRepository.java
├─ DepartmentService.java           (parent 해석 로직 + MapStruct 없음)
├─ DepartmentController.java        (페이징 없는 list)
└─ dto/
    ├─ DepartmentCreateRequest.java
    ├─ DepartmentUpdateRequest.java
    └─ DepartmentResponse.java
```

---

## 🔥 Customer/Item/Vendor 와의 4가지 결정적 차이

| | Customer/Item/Vendor | Department |
|---|---|---|
| 코드 발급 | `CodeGenerator` 자동 (CUST-2026-0001) | **운영자 수동 입력** (DEPT-SALES) |
| 부모 관계 | 없음 (평면) | **자기참조 ManyToOne** |
| 목록 | 페이징 (수천~수만 건) | **페이징 없음** (수십 건) |
| Mapper | MapStruct (`*Mapper`) | **서비스 내 수동 변환** |

각 차이가 왜 이런지를 짚는다.

---

## 🔥 1. 왜 코드를 수동 입력하는가

설계서 §3.4 에서 미리 짚었던 내용. ERP 의 다른 마스터/트랜잭션 코드는 일련번호가 자연스럽지만, 부서 코드는 다르다.

```
CUST-2026-0042  ← "42번째 등록된 고객" 이라는 사실은 별 의미 없음 (순번 그뿐)
ITEM-2026-0001  ← "이번 해 첫 번째 상품" 도 마찬가지

DEPT-SALES       ← "영업부" 임을 코드만 봐도 안다
DEPT-RND-AI      ← "연구개발본부 AI팀" — 계층/조직이 코드 형태에 박힘
```

→ **부서 코드는 의미를 담는 식별자**다. 자동 생성된 일련번호로는 표현 불가. 그래서 운영자가 직접 부여한다.

### DTO에서 정규식으로 형식 강제

```java
public record DepartmentCreateRequest(
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^DEPT-[A-Z0-9_-]+$",
                 message = "code 는 DEPT-XXX 형식이어야 한다")
        String code,
        @NotBlank @Size(max = 100) String name,
        String parentCode
) {}
```

`@Pattern` 어노테이션이 정규식 검증을 한다. 자유 입력이긴 하지만 **`DEPT-` 접두 + 대문자/숫자/언더스코어/하이픈** 으로만 허용해서 코드의 일관성을 강제한다. 실제 입력 예:

- ✅ `DEPT-SALES`, `DEPT-RND_AI`, `DEPT-FI-2`
- ❌ `dept-sales` (소문자), `SALES` (접두 없음), `DEPT-영업` (한글)

### `Department.create(...)` 에 `assignCode` 가 그대로 들어간다

```java
public static Department create(String code, String name, Department parent) {
    if (code == null || code.isBlank()) {
        throw new IllegalArgumentException("code 는 비어 있을 수 없다.");
    }
    if (name == null || name.isBlank()) { ... }
    Department d = new Department();
    d.assignCode(code);           // ← 1회 할당 규칙은 여전히 적용
    d.name = name;
    d.parent = parent;
    return d;
}
```

코드를 외부(`CodeGenerator`)에서 받든 운영자에게서 받든 **`BaseEntityWithCode.assignCode(...)` 를 통과해야 한다**. → 01편의 "코드는 한 번만 할당, 그 후 불변" 규칙이 어떤 입력 경로에서도 일관되게 적용된다.

---

## 🔥 2. 자기참조 ManyToOne

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id")
private Department parent;
```

### 같은 테이블의 다른 행을 FK로 가리킨다

DB 수준에서는 이런 모양:

```
department
+----+--------------+--------+-----------+
| id | code         | name   | parent_id |
+----+--------------+--------+-----------+
| 1  | DEPT-HQ      | 본사   | NULL      |   ← 최상위 (루트)
| 2  | DEPT-SALES   | 영업부  | 1         |   ← 본사 소속
| 3  | DEPT-SALES-1 | 영업1팀 | 2         |   ← 영업부 소속
| 4  | DEPT-SALES-2 | 영업2팀 | 2         |
| 5  | DEPT-RND     | 연구소  | 1         |
+----+--------------+--------+-----------+
```

`parent_id` 가 같은 `department` 테이블의 `id` 를 가리킨다. 트리 구조를 한 테이블로 표현하는 가장 단순한 방법 (Adjacency List Model).

### `FetchType.LAZY` 가 트리에서는 거의 필수

```java
@ManyToOne(fetch = FetchType.LAZY)
```

`EAGER` 였다면 부서를 로드할 때마다 부모 부서도 한 번에 SELECT. 부모의 부모도(같은 매핑이라면) 또. 트리가 깊으면 한 부서를 읽었을 뿐인데 N단 깊이의 SELECT가 줄줄이.

`LAZY` 로 두면 `department.getParent()` 를 **실제로 호출**할 때만 추가 쿼리가 나간다. 트리 구조에서 가장 안전한 기본값.

### `update(...)` 의 invariant 체크 — "자기 자신을 부모로?"

```java
public void update(String name, Department parent) {
    if (name == null || name.isBlank()) { ... }
    if (parent != null && parent.getId() != null && parent.getId().equals(this.getId())) {
        throw new IllegalArgumentException("자기 자신을 부모 부서로 지정할 수 없다.");
    }
    this.name = name;
    this.parent = parent;
}
```

자기참조 트리에서 가장 흔한 버그 — **자기 자신을 부모로 지정**해서 무한 루프 사이클을 만드는 것. 이걸 엔티티 안에서 막는다.

> ⚠️ 단, 이 체크는 1단계만 막는다. 더 깊은 사이클(A→B→C→A)은 못 잡는다. 운영에서 필요하면 별도 검사기를 두거나 DB 트리거를 쓴다. Phase 1 에서는 직접적인 자기참조만 막아도 충분.

### 의도적으로 `@OneToMany` 가 없다

```java
// 없다 — children 컬렉션
```

부모만 들고 있고 자식 컬렉션은 안 만든다. 이유:

1. **양방향이 되면 동기화 부담**이 생긴다 — `parent.addChild(this)` 같은 헬퍼 필요
2. **자식이 매우 많아질 수 있다** — `LAZY` 라도 `getChildren()` 한 번 호출하면 다 끌어옴
3. 자식 목록이 필요하면 **쿼리로 따로 조회**하면 된다 (`findByParent`)

→ "양방향 매핑은 정말 필요할 때만" 이라는 원칙. Phase 1 의 화면 요구사항이 단순해서 단방향으로 충분.

---

## 🔥 3. 페이징이 없는 목록 조회

### Controller

```java
@GetMapping
public List<DepartmentResponse> findAll() {
    // 부서는 보통 수십 개 정도라 페이징 없이 전체 반환.
    return service.findAll();
}
```

`Pageable` 도, `Specification` 도 없다. 그냥 List 전부.

### 도메인 특성 차이

| 마스터 | 일반적인 데이터 양 | 화면 요구 |
|---|---|---|
| Customer | 수천~수만 | 페이징 + 필터 검색 |
| Item | 수백~수천 | 페이징 + 카테고리 필터 |
| Vendor | 수백 | 페이징 (가능하지만 옵션) |
| **Department** | **수십** | **전체 트리** (페이징 안 함) |

부서가 1만 개가 넘어가는 회사는 드물다. 화면 요구도 다르다 — 부서는 보통 **드롭다운/트리 뷰**로 한 번에 보여줘야 하기 때문에 페이징이 오히려 방해된다.

→ 도메인의 실제 사용 패턴이 다르면 API 모양도 다르게 설계한다.

### `Specifications` 파일도 없다

Customer 는 `CustomerSpecifications.java` 가 있었고, Vendor 는 controller 안에 private static 으로 있었는데, **Department 는 아예 없다**. 검색 필터가 없으니까. 단순함이 도메인을 따라간다.

---

## 🔥 4. parent 해석 로직 — `parentCode` 로 받는 이유

```java
@Transactional
public DepartmentResponse create(DepartmentCreateRequest req) {
    if (repository.existsByCode(req.code())) {
        throw new IllegalStateException("이미 등록된 부서 코드입니다: " + req.code());
    }
    Department parent = resolveParent(req.parentCode());
    Department department = Department.create(req.code(), req.name(), parent);
    return toResponse(repository.save(department));
}

private Department resolveParent(String parentCode) {
    if (parentCode == null || parentCode.isBlank()) {
        return null;
    }
    return repository.findByCode(parentCode)
            .orElseThrow(() -> new EntityNotFoundException("Parent department not found: code=" + parentCode));
}
```

### DTO 가 `parentId` 가 아닌 `parentCode` 를 받는 이유

```java
public record DepartmentCreateRequest(
        ...
        String parentCode    // ← parentId 가 아니다
) {}
```

API 클라이언트가 부모를 지정할 때 **자동 증가된 PK(id)** 를 알 필요가 없다. 비즈니스 식별자인 `code` 가 더 자연스럽고 안전한 입력:

```
POST /api/departments
{ "code": "DEPT-SALES-1", "name": "영업1팀", "parentCode": "DEPT-SALES" }
```

`parentId: 5` 를 받으면 클라이언트가 ID 매핑을 신경 써야 하지만, `parentCode: "DEPT-SALES"` 는 의미가 명확. **외부 식별자(code)는 안정적, 내부 PK(id)는 노출 최소화**.

### `resolveParent` 가 변환 다리

서비스가 코드를 받아서 엔티티를 찾고, 그 엔티티를 도메인 메서드에 넘긴다. 도메인은 PK가 아닌 **부모 엔티티 자체** 를 받는다 → 도메인 계층이 외부 식별자 형식에 오염되지 않음.

### `null` 부모는 정상 (루트 부서)

```java
if (parentCode == null || parentCode.isBlank()) {
    return null;
}
```

부모가 없는 부서 = 루트 (예: `DEPT-HQ`). 그래서 `Department.parent` 컬럼은 nullable.

---

## 🔥 5. MapStruct 가 없다 — 직접 변환

```java
private DepartmentResponse toResponse(Department d) {
    return new DepartmentResponse(
            d.getId(),
            d.getCode(),
            d.getName(),
            d.getParent() != null ? d.getParent().getCode() : null,    // ← 이게 핵심
            d.getStatus(),
            d.getCreatedAt(),
            d.getCreatedBy(),
            d.getUpdatedAt(),
            d.getUpdatedBy()
    );
}
```

다른 마스터에는 `*Mapper.java` 가 있었는데 Department는 서비스 안에서 직접 변환한다. 왜?

**조건부 변환 필드** 가 있기 때문:

```java
d.getParent() != null ? d.getParent().getCode() : null
```

`parent` 가 null 일 수 있고, 응답에 표시할 건 `parent.id` 도 `parent.name` 도 아닌 **`parent.code`** 다. MapStruct로도 가능하지만 `@Mapping(target = "parentCode", expression = "...")` 같이 표현이 복잡해진다. **변환 로직 한 줄이면 수동이 더 깔끔**.

### N+1 위험 — LAZY + parent 접근

```java
d.getParent().getCode()
```

이 한 줄이 **추가 SELECT 한 번**을 발사한다 (LAZY 라서). 부서 100개를 `findAll()` 로 가져온 뒤 모두 `getParent().getCode()` 를 호출하면 → **1 + 100 회의 쿼리** (N+1 문제).

Phase 1 에서는 부서가 수십 개라 무시 가능한 수준이지만, 실무에서는 의식적으로 잡아야 한다. 해결 방법:

- `@EntityGraph(attributePaths = "parent")` 를 `findAll` 에 붙임
- `JOIN FETCH` 쿼리 작성
- 별도 DTO 프로젝션 쿼리

→ Phase 2의 QueryDSL 도입 + N+1 인식이 자연스럽게 이어진다. Phase 1 에서는 일단 단순함을 우선.

---

## 🔥 6. CRUD 5종 — 코드 조회 빼고

Customer 는 6종 엔드포인트(생성/PK조회/코드조회/목록/수정/삭제)였는데 Department 는 사실상 같은 6종. **목록 조회만 페이징 없음** 이라는 한 가지가 다를 뿐.

```java
POST   /api/departments
GET    /api/departments/{id}
GET    /api/departments/by-code/{code}
GET    /api/departments            ← Pageable 없음, List 반환
PUT    /api/departments/{id}
DELETE /api/departments/{id}
```

---

## 🔥 시드 데이터 (V8) — 5개 부서 트리

마이그레이션 V8 에서 시드되는 부서 트리(설계서 §9 참조):

```
DEPT-HQ (본사)
├─ DEPT-SALES (영업부)
│  ├─ DEPT-SALES-1 (영업1팀)
│  └─ DEPT-SALES-2 (영업2팀)
└─ DEPT-RND (연구소)
```

→ 07편의 Employee 가 이 부서에 소속된다. 즉 **Department 는 Employee 의 dependency**. 그래서 마이그레이션 순서도 V6 (Department) → V7 (Employee) 다.

---

## 자기 점검

- [ ] 부서 코드를 자동 생성하지 않는 이유는 도메인 관점에서 무엇인가?
- [ ] `@ManyToOne(fetch = FetchType.LAZY)` 가 자기참조에서 특히 중요한 이유?
- [ ] DTO 에서 `parentId` 가 아닌 `parentCode` 를 받는 이유?
- [ ] Department 에 MapStruct Mapper 가 없는 이유 (한 줄로)?
- [ ] `findAll()` 호출 시 발생할 수 있는 N+1 시나리오를 설명할 수 있는가?

---

이전 편 → [05-Item-Vendor.md](./05-Item-Vendor.md)
다음 편 → [07-Employee-Department-FK.md](./07-Employee-Department-FK.md)

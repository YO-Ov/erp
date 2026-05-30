# Phase 6 코드 워크스루 ⑤ — 감사 로그(층위 ②): @EntityListeners로 변경 이력을 쌓는다 ⭐

> 대상 파일
> - `audit/Auditable.java` / `AuditAction.java` / `AuditEvent.java`
> - `audit/AuditEntityListener.java` / `AuditBuffer.java` / `AuditLogWriter.java` / `AuditBufferConfigurer.java`
> - `audit/AuditLog.java` / `AuditLogRepository.java` / `AuditLogController.java`
> - `master/customer/Customer.java` (감사 대상 부착)
>
> 이 글의 목표: 변경 한 건마다 한 행을 쌓는 **append-only 감사 로그**를 직접 구현(확정 ②)한 흐름을 따라간다. Phase 3 `StockMovement`·Phase 5 `JournalEntry`와 같은 "이력 누적" 패턴의 세 번째 등장. 브리핑 §6-2·§6-3.

---

## 0. 한눈에 — "콜백에서 모았다가 커밋 후 기록"

```
[비즈니스 트랜잭션]
Customer.update(...)  →  Hibernate flush
   │
   ▼  @PostUpdate (세션 열림)
AuditEntityListener.onUpdate(customer)
   ├─ customer.auditSnapshot()  →  Map  →  ★즉시 JSON 직렬화★  (세션 살아있을 때)
   └─ AuditBuffer.add(AuditEvent(type, id, UPDATE, json))  →  ThreadLocal 에 적재
   │
   ▼  트랜잭션 COMMIT
afterCommit:  AuditLogWriter.write(events)   @Transactional(REQUIRES_NEW)
   └─ audit_log 에 INSERT (changed_by = SecurityAuditorAware)
```

두 개의 설계 결정이 이 그림에 녹아 있다:
1. **JSON 직렬화는 콜백(세션 열림) 시점에 즉시** — lazy 프록시 깨짐 방지.
2. **DB 기록은 커밋 직후 별도 트랜잭션** — flush 재진입 방지.

(이 둘은 구현 중 실제로 부딪힌 문제를 풀며 정해졌다 — §5 참고.)

---

## 1. 누가 감사 대상인가 — `Auditable` 인터페이스 (선택 B)

설계 확정 ②는 "전 엔티티"가 아니라 **핵심 엔티티만**(선택 B) 감사한다. 무엇을 감사할지 의식적으로 고르는 게 학습 포인트.

```java
public interface Auditable {
    Long getId();

    default String auditEntityType() {
        return ClassUtils.getUserClass(this).getSimpleName();   // Hibernate 프록시 벗긴 실제 클래스명
    }

    Map<String, Object> auditSnapshot();   // 무엇을 이력에 남길지 명시적으로 고른다
}
```

- **`auditSnapshot()`이 핵심 설계점.** 리플렉션으로 전 필드를 긁는 대신, 엔티티가 "감사에 남길 필드"를 직접 노출한다. "무엇을 추적하는가"가 코드에 명시적으로 드러난다.
- **`ClassUtils.getUserClass`**: Hibernate가 만든 프록시(`Customer$HibernateProxy$xxx`)가 아니라 원래 클래스명(`Customer`)을 얻는다 — `entity_type` 컬럼이 깔끔해진다.

`Customer`에 부착:

```java
@Entity
@SQLDelete(...) @SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditEntityListener.class)        // ← 감사 리스너 부착
public class Customer extends BaseEntityWithCode implements Auditable {

    @Override
    public Map<String, Object> auditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("code", getCode());
        snapshot.put("name", name);
        snapshot.put("businessNo", businessNo);
        snapshot.put("creditLimit", creditLimit);     // ← 시연 핵심: 신용한도 변경 추적
        snapshot.put("paymentTerms", paymentTerms);
        snapshot.put("status", getStatus());
        return snapshot;
    }
}
```

> 💡 `BaseEntity`에는 이미 `@EntityListeners(AuditingEntityListener.class)`(JPA Auditing, 층위 ①)가 붙어 있다. Customer에 추가로 `@EntityListeners(AuditEntityListener.class)`를 달면 **둘 다 동작**한다 — JPA가 두 리스너를 모두 호출. 층위 ①(변경자)과 층위 ②(이력)가 한 엔티티에 공존.

---

## 2. 변경을 가로채는 리스너 — `AuditEntityListener`

```java
public class AuditEntityListener {

    private static final ObjectMapper FALLBACK_MAPPER = new ObjectMapper();

    @PostPersist public void onInsert(Object e) { emit(e, AuditAction.INSERT); }
    @PostUpdate  public void onUpdate(Object e) { emit(e, AuditAction.UPDATE); }
    @PostRemove  public void onDelete(Object e) { emit(e, AuditAction.DELETE); }

    private void emit(Object entity, AuditAction action) {
        if (entity instanceof Auditable auditable) {
            String json = toJson(auditable.auditSnapshot());   // ★ 콜백 시점에 즉시 직렬화
            AuditBuffer.add(new AuditEvent(
                    auditable.auditEntityType(), auditable.getId(), action, json));
        }
    }
    // toJson(...): AuditBuffer.objectMapper() 사용, 없으면 FALLBACK_MAPPER
}
```

- **`@PostPersist/@PostUpdate/@PostRemove`**: JPA 생명주기 콜백. INSERT/UPDATE/DELETE가 flush될 때 호출된다.
- **JPA 리스너는 Spring 빈이 아니다.** JPA가 직접 `new` 한다 → `@Autowired`로 의존성을 못 받는다. 그래서 정적 진입점 `AuditBuffer`에 사건을 넘기고(§3), 협력 빈은 `AuditBufferConfigurer`가 정적으로 주입한다(§4).
- **`auditSnapshot()` → JSON 변환을 여기서(콜백 안에서) 즉시** 한다. 이게 §5에서 설명할 문제 해결의 핵심.

---

## 3. 트랜잭션 단위 버퍼 — `AuditBuffer`

```java
public final class AuditBuffer {
    private static final ThreadLocal<List<AuditEvent>> EVENTS = ThreadLocal.withInitial(ArrayList::new);
    private static AuditLogWriter writer;
    private static ObjectMapper objectMapper;

    static void add(AuditEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (writer != null) writer.write(List.of(event));    // 트랜잭션 밖이면 즉시 기록
            return;
        }
        List<AuditEvent> events = EVENTS.get();
        boolean first = events.isEmpty();
        events.add(event);
        if (first) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    if (writer != null) writer.write(new ArrayList<>(EVENTS.get()));
                }
                @Override public void afterCompletion(int status) {
                    EVENTS.remove();   // 커밋/롤백 무관 ThreadLocal 정리 (누수 방지)
                }
            });
        }
    }
}
```

- **`ThreadLocal`에 한 트랜잭션의 사건을 모은다.** 같은 요청에서 여러 엔티티가 바뀌면 다 모았다가 커밋 시 한꺼번에 기록.
- **`afterCommit`에서 기록** — 비즈니스 트랜잭션이 **성공적으로 커밋된 뒤**에만 감사가 남는다. 롤백되면? `afterCommit`이 안 불리고 `afterCompletion`에서 ThreadLocal만 정리 → **감사도 안 남는다**(정합성: 일어나지 않은 변경은 기록하지 않음).
- **`afterCompletion`에서 `EVENTS.remove()`**: ThreadLocal은 스레드 풀에서 재사용되므로 반드시 정리해야 다음 요청에 누수되지 않는다.

---

## 4. 기록기 — `AuditLogWriter`와 정적 브리지

```java
@Component
public class AuditLogWriter {
    private final AuditLogRepository repository;
    private final AuditorAware<String> auditorAware;   // ★ 층위①과 같은 출처
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(List<AuditEvent> events) {
        String changedBy = auditorAware.getCurrentAuditor().orElse("system");
        LocalDateTime now = LocalDateTime.now(clock);
        for (AuditEvent event : events) {
            repository.save(AuditLog.of(
                    event.entityType(), event.entityId(), event.action(),
                    changedBy, now, event.changesJson()));
        }
    }
}
```

- **`REQUIRES_NEW`**: 이미 커밋된 비즈니스 트랜잭션과 별개로 새 트랜잭션을 열어 INSERT. `afterCommit` 시점엔 원래 트랜잭션이 닫혀 있으므로 새 경계가 필요하다.
- **`changedBy`를 `AuditorAware`에서 가져온다** — 워크스루 ④의 `SecurityAuditorAware`와 **같은 출처**. 그래서 층위 ①(`created_by`)과 층위 ②(`audit_log.changed_by`)가 동일한 "누가"를 공유한다.
- `changesJson`은 이미 문자열이라 여기선 엔티티/프록시를 건드리지 않는다(안전).

정적 브리지 — JPA 리스너에 빈을 연결:

```java
@Component
@RequiredArgsConstructor
public class AuditBufferConfigurer {
    private final AuditLogWriter auditLogWriter;
    private final ObjectMapper objectMapper;
    @PostConstruct
    void wire() { AuditBuffer.configure(auditLogWriter, objectMapper); }   // 정적 필드에 주입
}
```

→ 기동 시 한 번, Spring 빈(`AuditLogWriter`, `ObjectMapper`)을 정적 `AuditBuffer`에 꽂아준다. "Spring 빈이 아닌 JPA 리스너"와 "Spring 빈"을 잇는 다리.

---

## 5. 구현 중 실제로 부딪힌 두 문제 ⭐

이 설계는 처음부터 이 모양이 아니었다. 두 번 깨지고 고쳤다 — 학습 가치가 큰 부분.

### 문제 1 — lazy 프록시 직렬화 실패

처음엔 `AuditEvent`에 `Map<String,Object> snapshot`을 담고 **커밋 후**(`AuditLogWriter`) JSON 직렬화했다. 그런데 커밋 후엔 영속성 컨텍스트(세션)가 닫혀 있어, snapshot 안에 lazy 연관(프록시)이 있으면 직렬화 시 `LazyInitializationException`이 터졌다.

→ **해결**: 직렬화를 **콜백 시점(`@PostUpdate`, 세션 열림)으로 앞당겼다.** `AuditEvent`는 이제 `Map`이 아니라 **이미 직렬화된 `changesJson` 문자열**을 담는다(`AuditEntityListener.toJson`). 커밋 후엔 문자열만 INSERT하므로 프록시를 건드릴 일이 없다.

### 문제 2 — flush 재진입

`@PostPersist`/`@PostUpdate`는 Hibernate가 **flush(=commit) 도중** 호출한다. 그 순간 같은 세션으로 `audit_log`를 INSERT하면 "flush 중 또 flush"라는 재진입 위험이 있다.

→ **해결**: 콜백에선 DB를 안 건드리고 `AuditBuffer`(ThreadLocal)에 **적재만** 한다. 실제 INSERT는 `afterCommit` + `REQUIRES_NEW`로 미룬다.

> ⚠️ 이 절충의 결과: 감사 기록은 **사후 기록**이다(비즈니스 커밋 직후 별도 트랜잭션). 만약 audit_log INSERT 자체가 실패하면 비즈니스 변경은 이미 커밋된 뒤다. 완벽한 원자성을 원하면 Envers나 같은-트랜잭션 기록이 필요하지만, "흐름을 눈으로 본다"는 학습 목적엔 이 단순·안정 조합이 맞다. 한계를 아는 것까지가 학습.

---

## 6. 저장 모델과 조회 — `AuditLog` / `AuditLogController`

```java
@Entity @Table(name = "audit_log")
public class AuditLog {                 // ← BaseEntity 상속 안 함 (자기 감사 방지)
    @Id @GeneratedValue(...) private Long id;
    private String entityType;          // "Customer"
    private Long entityId;
    @Enumerated(EnumType.STRING) private AuditAction action;   // INSERT/UPDATE/DELETE
    private String changedBy;           // 층위①과 같은 출처
    private LocalDateTime changedAt;
    private String changes;             // JSON 스냅샷 (LONGTEXT)
    // setter 없음 — append-only
}
```

- **`BaseEntity`를 상속하지 않는다.** 만약 상속하면 audit_log도 감사 대상이 되어 무한 루프 위험 + created_by 류 컬럼이 의미 없이 붙는다. 그래서 `changed_by`/`changed_at`만 직접 가진다(브리핑 §6-2).
- **setter 없음** = append-only. Phase 3 `StockMovement`, Phase 5 `JournalEntry`와 동일한 "한 번 쓰면 불변" 철학.

조회 API:

```java
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")    // 민감 정보 — 제한 열람
public class AuditLogController {
    @GetMapping
    public Page<AuditLogResponse> search(
            @RequestParam String entityType,
            @RequestParam(required = false) Long entityId,
            Pageable pageable) { ... }   // 최신순
}
```

→ "이 고객(#42)이 누가 언제 어떻게 바뀌었나"를 한 번에 조회. 계획서 성공 기준("변경 이력을 조회할 수 있는 API")을 충족하고, 인가도 ADMIN/FINANCE로 제한.

---

## 7. 테스트로 본 이력 — `AuditLogTest`

```java
@WithMockUser(username = "tester@hwlee-erp.example", roles = "ADMIN")
void customer_insert_and_update_are_audited_with_user() {
    Long id = txTemplate.execute(s -> customerRepository.save(Customer.create(...)).getId());  // INSERT
    txTemplate.executeWithoutResult(s -> {                                                      // UPDATE
        Customer c = customerRepository.findById(id).orElseThrow();
        c.update("감사테스트상사", "부산", new BigDecimal("5000000"), PaymentTerms.NET60);
    });

    var logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByChangedAtDesc("Customer", id, ...);
    assertThat(logs).anyMatch(l -> l.getAction() == AuditAction.INSERT);
    assertThat(logs).anyMatch(l -> l.getAction() == AuditAction.UPDATE);
    assertThat(logs).allMatch(l -> l.getChangedBy().equals("tester@hwlee-erp.example"));   // 층위②도 실사용자
    assertThat(logs).anyMatch(l -> l.getChanges().contains("5000000"));                     // 스냅샷 검증
}
```

- **`TransactionTemplate`으로 변경을 별도 커밋**시킨다 — 감사가 `afterCommit`에 일어나므로, 같은 트랜잭션 안에서는 아직 기록 전이라 별도 커밋이 필요. (테스트 설계가 구현 메커니즘을 그대로 반영)
- 검증: INSERT/UPDATE 둘 다 남고, `changed_by`가 실사용자, `changes` JSON에 바뀐 신용한도(5000000)가 담겼는지.

---

## 8. 자기 점검

- [ ] JSON 직렬화를 커밋 후가 아니라 콜백 시점에 하는 이유는? (힌트: lazy 프록시)
- [ ] 콜백에서 DB에 바로 INSERT하지 않고 버퍼에 모으는 이유는? (힌트: flush 재진입)
- [ ] 비즈니스 트랜잭션이 롤백되면 감사 로그는 어떻게 되는가?
- [ ] `audit_log`가 `BaseEntity`를 상속하지 않는 이유 두 가지는?
- [ ] 층위 ①과 층위 ②가 같은 "누가"를 공유하는 메커니즘은? (힌트: `AuditorAware`)
- [ ] 이 구현이 Envers 대비 갖는 한계(사후 기록)는 무엇인가?

---

## 9. 한 줄 요약

> 감사 로그(층위 ②)는 `@EntityListeners`로 변경을 가로채되, **콜백에서 즉시 JSON으로 고정**(프록시 회피)하고 **커밋 후 별도 트랜잭션으로 기록**(flush 재진입 회피)한다. `StockMovement`·`JournalEntry`와 같은 append-only 원장이며, `changed_by`는 층위 ①과 같은 출처를 공유한다.

# Phase 6 코드 워크스루 ④ — AuditorAware 교체: 한 빈으로 전 모듈의 변경자가 바뀐다 ⭐

> 대상 파일
> - `security/support/SecurityAuditorAware.java` (신규)
> - `common/audit/JpaAuditingConfig.java` (한 줄 교체)
>
> 이 글의 목표: Phase 1에서 심은 **`auditorProvider` 복선이 회수**되는 순간을 본다. 감사 "층위 ①"(변경자 추적). 브리핑 §6-1, 그리고 §0의 "복선 회수의 Phase"가 가장 선명하게 드러나는 곳.

---

## 0. 이 글이 특별한 이유

Phase 6에서 작성한 코드 중 **가장 적은 줄로 가장 넓은 효과**를 내는 부분이다. 새 클래스 하나(약 15줄) + 기존 설정 한 줄 교체로, **SD/MM/FI/master 전 모듈의 모든 테이블**의 `created_by`/`updated_by`가 그날부터 진짜 사용자로 바뀐다. 기존 도메인 코드는 **0줄** 수정.

---

## 1. Phase 1이 심어둔 복선

`BaseEntity`(Phase 1)는 모든 엔티티에 4개 감사 컬럼을 깔아뒀다:

```java
@CreatedBy   @Column(name="created_by", updatable=false, length=64) private String createdBy;
@LastModifiedBy @Column(name="updated_by", length=64) private String updatedBy;
@CreatedDate ... private LocalDateTime createdAt;
@LastModifiedDate ... private LocalDateTime updatedAt;
```

`@CreatedBy`/`@LastModifiedBy`에 "누구"를 채워주는 건 `AuditorAware<String>` 빈이다. Phase 1의 그 빈은 이랬다:

```java
// JpaAuditingConfig (Phase 1 원본)
@Bean
public AuditorAware<String> auditorProvider() {
    return () -> Optional.of("system");   // ← 항상 "system" (인증 시스템이 없으니까)
}
```

→ Phase 1~5 내내 모든 행의 `created_by`는 `"system"`이었다. 주석에 "Phase 6에서 SecurityContext 기반으로 교체된다"고 적어둔 **복선**.

---

## 2. 복선 회수 — `SecurityAuditorAware`

```java
public class SecurityAuditorAware implements AuditorAware<String> {

    static final String SYSTEM_USER = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(SYSTEM_USER);    // ← 미인증 컨텍스트는 "system" fallback
        }
        return Optional.of(authentication.getName());   // ← 로그인 사용자명(=email)
    }
}
```

핵심:

- **`SecurityContextHolder`에서 현재 인증을 꺼낸다.** 이건 워크스루 ①의 `JwtAuthenticationFilter`가 매 요청 채워둔 바로 그 컨텍스트다. 즉 "토큰 → 컨텍스트 → 감사"로 정보가 흐른다.
- **`authentication.getName()`** = 로그인 시 토큰의 `sub`에 넣은 username(=email, 예: `kim@hwlee-erp.example`).
- **3중 fallback 가드**: 인증이 없거나(`null`), 인증 안 됐거나(`!isAuthenticated()`), 익명(`AnonymousAuthenticationToken`)이면 `"system"`. 이게 왜 중요하냐면:
  - **Flyway 시드**(V8, V28)는 SQL이라 `created_by='system'` 직접 지정 — 무관.
  - **배치/스케줄러**(Phase 9 예정), **로그인 전 헬스체크** 등 인증 컨텍스트가 없는 경로는 자연스럽게 `"system"`으로 기록된다 — 예외 안 터지고 안전.

---

## 3. 설정 교체 — 한 줄

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SecurityAuditorAware();   // 기존: () -> Optional.of("system")
    }
}
```

- `@EnableJpaAuditing(auditorAwareRef = "auditorProvider")`는 **그대로**. 빈 이름도 그대로(`auditorProvider`). 반환 구현체만 바꿨다.
- 그래서 **이 빈을 참조하는 모든 곳(= JPA Auditing 전체)이 자동으로 새 동작을 따른다.** Customer 서비스도, JournalEntry 서비스도, 그 어떤 엔티티도 코드 변경이 필요 없다 — `BaseEntity`가 이미 깔아둔 레일 위라서.

---

## 4. 효과 — 무엇이 달라지나

로그인한 `kim@hwlee-erp.example`이 고객을 하나 만들면:

```sql
-- Phase 5까지
INSERT INTO customer (..., created_by, updated_by) VALUES (..., 'system', 'system');
-- Phase 6 이후 (코드 변경 0줄)
INSERT INTO customer (..., created_by, updated_by) VALUES (..., 'kim@hwlee-erp.example', 'kim@...');
```

이게 감사의 **층위 ①** — "지금 이 행을 마지막으로 만진 사람이 누구인가". 단, 층위 ①은 **현재 값**만 안다(덮어쓰기). "100만→500만→300만"의 전 과정은 층위 ②(워크스루 ⑤)가 맡는다.

---

## 5. 테스트로 본 회수 — `AuditorAwareTest`

```java
@SpringBootTest
@Transactional
class AuditorAwareTest {
    @Test
    @WithMockUser(username = "kim@hwlee-erp.example", roles = "ADMIN")
    void created_by_is_authenticated_user() {
        Customer c = Customer.create("CUST-AUD-2", "감사자테스트", "111-00-22200",
                "서울", new BigDecimal("100000"), PaymentTerms.NET30);
        Customer saved = customerRepository.saveAndFlush(c);

        assertThat(saved.getCreatedBy()).isEqualTo("kim@hwlee-erp.example");
        assertThat(saved.getUpdatedBy()).isEqualTo("kim@hwlee-erp.example");
    }
}
```

- **`@WithMockUser(username = ...)`**: Spring Security Test가 `SecurityContext`에 가짜 인증을 심는다. 그러면 `SecurityAuditorAware.getCurrentAuditor()`가 그 username을 반환한다.
- 핵심 검증: `created_by`가 `"system"`이 아니라 `"kim@..."`. 복선이 회수됐음을 코드로 증명.

> 💡 이 테스트가 통과한다는 건, **기존 Customer 코드를 한 줄도 안 고쳤는데** 변경자가 실제 사용자로 바뀌었다는 뜻. "인프라(BaseEntity + AuditorAware)에 미리 투자해두면 나중에 횡단 기능이 공짜로 얹힌다"의 산 증거.

---

## 6. 누적성

```
Phase 1: BaseEntity 4컬럼 + auditorProvider("system") 자리만 잡음   ← 복선 심기
   │
   └─→ Phase 6: SecurityAuditorAware 로 교체                        ← 복선 회수 (이 글)
          └─ 정보 출처: JwtAuthenticationFilter 가 채운 SecurityContext (워크스루 ①)
          └─ 같은 출처를 층위 ②(AuditLogWriter)도 공유 (워크스루 ⑤)
```

---

## 7. 자기 점검

- [ ] `SecurityAuditorAware`가 사용자명을 꺼내는 출처(`SecurityContext`)는 누가 채웠는가?
- [ ] 미인증 컨텍스트(배치/시드)에서 `created_by`는 무엇이 되는가? 왜 그 fallback이 필요한가?
- [ ] 기존 SD/MM/FI 엔티티 코드를 0줄 고치고도 변경자가 바뀌는 이유는? (힌트: `BaseEntity`, 빈 참조)
- [ ] 층위 ①이 "현재 값만 안다"는 한계는 무엇이고, 누가 보완하는가?

---

## 8. 한 줄 요약

> `AuditorAware` 빈의 구현체 하나를 `"system"` 고정에서 `SecurityContext` 기반으로 바꾸자, Phase 1이 깔아둔 `BaseEntity` 레일을 타고 **전 모듈의 변경자가 진짜 사용자로** 바뀐다 — 기존 코드 0줄 수정. Phase 6 "복선 회수"의 가장 선명한 장면.

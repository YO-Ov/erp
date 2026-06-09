# 6/6. Flyway V29~V31 + HR 인가 — 스키마를 얹고, 급여를 가린다

> 마지막 글. 4개 테이블과 5개 계정·HR 역할을 어떻게 **기존 V28 위에 안전하게** 얹었는지(Flyway), 그리고 "급여는 민감정보"를 어떻게 **역할 단위 인가**로 막았는지(Phase 6 RBAC 확장) 본다.

대상 파일:

```
hwlee-erp/src/main/resources/db/migration/
├─ V29__create_hr_payroll.sql   4테이블
├─ V30__seed_hr_accounts.sql    계정 5 + HR 역할/권한
└─ V31__seed_hr_demo.sql        인사팀 직원 + 급여계약 시드

각 Controller 의 @PreAuthorize("hasAnyRole('HR','ADMIN')")
```

---

## 🔥 V29 — 4테이블, 채번/이력의 흔적

기존 마지막은 V28(`seed_auth`). 이어서 V29 가 4테이블을 만든다. 핵심 제약만:

```sql
-- 급여계약: 직원당 여러 줄(이력), 발효 구간 인덱스
CREATE TABLE employment_contract (
    ...
    effective_from   DATE NOT NULL,
    effective_to     DATE,                    -- NULL = 현재 유효
    CONSTRAINT chk_contract_salary CHECK (base_salary > 0),
    KEY idx_contract_effective (effective_from, effective_to)
);

-- 근태: 하루 한 건 (1글/2글의 그 UNIQUE)
CREATE TABLE attendance (
    ...
    UNIQUE KEY uk_attendance_emp_date (employee_id, work_date)
);

-- 급여대장: 월 1건
CREATE TABLE payroll_run (
    ...
    period          VARCHAR(7) NOT NULL,       -- YYYY-MM
    UNIQUE KEY uk_payroll_run_number (number),
    UNIQUE KEY uk_payroll_run_period (period)
);

-- 명세서: 대장당 직원 한 번
CREATE TABLE payslip (
    ...
    UNIQUE KEY uk_payslip_run_employee (payroll_run_id, employee_id),
    CONSTRAINT fk_payslip_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_run(id)
);
```

> 모든 테이블이 `created_at/by + updated_at/by` 4컬럼을 갖는다 — `BaseEntity` 상속(soft delete 없음, 마스터가 아니라 트랜잭션/이력이라 `deletedAt` 불필요). 도메인 불변식을 **DB 제약으로도** 한 번 더 박았다(코드의 `isEffectiveOn`/`existsBy...` 와 이중).

---

## 🔥 V30 — 계정 5개를 트리에 매달기

Phase 5 의 계정 시드(V23) 패턴 그대로 — 부모를 코드로 lookup 해서 말단 계정을 단다:

```sql
INSERT INTO account (code, name, type, parent_id, postable, status, ...)
SELECT '5200', '급여비용',   'EXPENSE', id, 1, 'ACTIVE', ... FROM account WHERE code = '5000';
SELECT '5300', '법정복리비', 'EXPENSE', id, 1, 'ACTIVE', ... FROM account WHERE code = '5000';
SELECT '2400', '예수금-소득세',   'LIABILITY', id, 1, ... FROM account WHERE code = '2000';
SELECT '2500', '예수금-사회보험', 'LIABILITY', id, 1, ... FROM account WHERE code = '2000';
SELECT '2600', '미지급급여',     'LIABILITY', id, 1, ... FROM account WHERE code = '2000';
```

`postable=1`(말단, 라인 부착 가능), 부모는 5000(비용)/2000(부채). 4글의 `SystemAccounts` 상수와 코드가 정확히 맞물린다 — 코드 상수와 DB 시드는 **같은 약속의 양쪽 끝**이라, 하나만 바뀌면 `account(code)` 조회가 터진다(그래서 둘을 같은 PR 에서 함께 바꾼다).

같은 파일에서 **HR 역할**도 추가:

```sql
INSERT INTO role (code, name, ...) VALUES ('HR', '인사', ...);
INSERT INTO permission (code, name, ...) VALUES ('HR_READ', 'HR 조회', ...), ('HR_WRITE', 'HR 변경', ...);

-- HR 역할 ← HR_READ/WRITE + MASTER_READ
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'HR' AND p.code IN ('HR_READ', 'HR_WRITE', 'MASTER_READ');

-- ⚠️ ADMIN 에도 신규 HR 권한 추가 (V28 의 'ADMIN 전 권한' 은 그 시점 권한만 매핑했으므로)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'ADMIN' AND p.code IN ('HR_READ', 'HR_WRITE');
```

> 마지막 INSERT 가 미묘하다. V28 은 "ADMIN = 그때 존재하던 전 권한"을 박았다. 나중에 추가된 `HR_*` 권한은 ADMIN 에 **자동으로 붙지 않는다** — 시드는 실행 시점의 스냅샷이라서. 그래서 명시적으로 ADMIN↔HR 권한을 한 줄 더 넣는다. (참고: 현재 인가는 권한이 아니라 **역할 단위**(`hasAnyRole`)라 ADMIN 은 이미 통과하지만, 권한 구조의 일관성을 위해 채워둔다.)

---

## 🔥 V31 — 시연 데이터: 인사팀 직원 + 급여계약

급여 계산이 바로 돌려면 **급여계약**이 미리 있어야 한다(없으면 `findEffectiveOn` 이 빈 결과). V31 이 인사팀 직원 1명과 4명분 계약을 시드한다:

```sql
-- 1) 인사팀 직원 (V8 의 EMP 채번 다음 번호)
INSERT INTO employee (code, name, email, department_id, ...)
SELECT CONCAT('EMP-', YEAR(NOW()), '-0005'), '정인사', 'jung@hwlee-erp.example', d.id, ...
  FROM department d WHERE d.code = 'DEPT-HR';
UPDATE code_sequence SET next_number = 6 WHERE prefix = 'EMP';

-- 2) 로그인 계정 (pass1234, V28 과 동일 BCrypt 해시)
-- 3) 부서 기반 역할 부여 (DEPT-HR → HR)
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM app_user u
  JOIN employee e ON e.id = u.employee_id
  JOIN department d ON d.id = e.department_id
  JOIN role r ON r.code = 'HR'
 WHERE d.code = 'DEPT-HR';

-- 4) 급여계약 — 4명 모두 2026-01-01 발효, 소정 209h
INSERT INTO employment_contract (employee_id, position, base_salary, contracted_hours,
                                 effective_from, effective_to, status, ...)
SELECT e.id, CASE e.email WHEN 'kim@...' THEN 'SENIOR' ... END,
            CASE e.email WHEN 'kim@...' THEN 3500000.00 ... END,
       209, '2026-01-01', NULL, 'ACTIVE', ...
  FROM employee e WHERE e.email IN ('kim@...', 'lee@...', 'park@...', 'jung@...');
```

> **근태는 시드하지 않는다.** 의도된 선택 — 시연 때 API 로 직접 근태를 넣어 "연장근로가 급여에 반영되는" 흐름을 손으로 체득하는 게 학습에 낫다(2글). 계약은 미리, 근태는 손으로.
>
> 시연 계정: `jung@hwlee-erp.example / pass1234` → **HR 역할**. (Phase 6 의 kim=SALES, lee=FINANCE, park=역할없음, admin=ADMIN 에 더해진다.)

---

## 🔥 인가 — "급여는 아무나 못 본다"

급여는 가장 민감한 정보다. 세 컨트롤러 모두 **클래스 단위**로 HR/ADMIN 만 통과시킨다(Phase 6 의 메서드 보안 채택):

```java
@RestController
@RequestMapping("/api/payroll-runs")
@PreAuthorize("hasAnyRole('HR','ADMIN')")     // 급여대장
public class PayrollController { ... }

@PreAuthorize("hasAnyRole('HR','ADMIN')")     // 급여계약  (EmploymentContractController)
@PreAuthorize("hasAnyRole('HR','ADMIN')")     // 근태       (AttendanceController)
```

결과:
- `jung`(HR) / `admin`(ADMIN) → 급여 API 200 OK
- `kim`(SALES) / `lee`(FINANCE) / `park`(역할없음) → **403 Forbidden**

> 재무(lee)조차 막힌다 — 회계 전표(인건비 합계)는 보지만, 개인별 급여 명세는 인사 영역. "전표의 총액은 회계가, 개인 명세는 인사가" 라는 실무의 경계를 인가로 그었다. Phase 6 에서 깔아둔 `@EnableMethodSecurity` + 역할 모델 위에 컨트롤러 한 줄로 얹은 것 — 인증/인가 인프라를 한 번 만들어두면 새 모듈은 어노테이션 한 줄로 보호된다.

---

## Phase 7 전체 정리

| # | 글 | 핵심 |
| --- | --- | --- |
| 1 | 급여계약 이력테이블 | 발효일 구간 + 직전 계약 자동 종료 + 시급 환산 |
| 2 | 근태 누적·파생 | 시각→분 파생, 하루 한 건 이중 방어, 월 SUM |
| 3 | 급여계산 헤더-라인 | 세율 상수 + net 도출(라인 책임) + 만근 가정 |
| 4 ⭐ | 인건비 자동분개 | 5계정, **net 정의가 차/대 균형 보증** |
| 5 | 이벤트·2단계 지급 | HR↛FI, BEFORE_COMMIT, 발생주의 |
| 6 | Flyway·인가 | V29~31, 급여 HR/ADMIN 전용 |

**한 문장**: Phase 7 은 **근태(이력) → 급여 계산(헤더-라인) → 확정 이벤트 → 인건비 전표(복식부기) → 2단계 지급** 으로 Phase 1~6 의 패턴(이력/헤더-라인/이벤트/자동분개/RBAC)을 한데 모은 종합편이다.

→ 다음은 **시연 가이드** — 이 흐름을 Swagger/cURL 로 손으로 돌려본다.

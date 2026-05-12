# 4/9. `V1__init.sql` + Flyway 동작 원리

```sql
-- Phase 0: 스키마 마이그레이션 동작 확인용 더미 테이블
-- Phase 1에서 실제 마스터 테이블이 추가되면서 V2부터 이어진다.

CREATE TABLE schema_init_marker (
    id BIGINT NOT NULL AUTO_INCREMENT,
    note VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO schema_init_marker(note) VALUES ('Phase 0 init OK');
```

내용 자체는 단순. 하지만 **Flyway의 동작 메커니즘**이 ERP 학습의 핵심.

---

## 🔥 파일명 규칙 — `V1__init.sql`

```
V    1     __     init     .sql
│    │     │      │
│    │     │      └── 설명 (자유, 공백은 언더스코어로)
│    │     └── 구분자 (언더스코어 2개) ⚠️ 1개면 인식 안 됨
│    └── 버전 번호 (정수 또는 점 표기. V1, V1.1, V2.0.3 모두 가능)
└── Versioned migration (한 번만 실행)
```

다른 접두사도 있다:

- **`R__설명.sql`** = Repeatable. 내용이 바뀌면 다시 실행 (뷰/함수에 주로 씀)
- **`U1__설명.sql`** = Undo. 유료(Flyway Teams) — 무료 버전에선 못 씀

→ 그래서 **ERP 운영에서 마이그레이션을 되돌리는 정공법은 "되돌리는 V를 새로 추가"** 한다. 예: V5가 컬럼을 추가했고 되돌리고 싶다면 → V6에서 `DROP COLUMN`. 절대 V5 파일을 수정하지 않음.

---

## 🔥 Flyway가 기동 시 하는 일

기동 시점 시퀀스:

1. `flyway_schema_history` 테이블이 있는가? 없으면 만든다
2. `db/migration/` 폴더의 파일들을 **파일명 버전 순으로 정렬**
3. `flyway_schema_history`를 보고 "어디까지 적용됐는지" 확인
4. 미적용 파일들을 **한 트랜잭션씩** 실행 → 성공하면 `flyway_schema_history`에 기록

`flyway_schema_history` 테이블은 대략 이렇게 생겼다:

| installed_rank | version | description | script | checksum | installed_on | success |
|---|---|---|---|---|---|---|
| 1 | 1 | init | V1__init.sql | -847291 | 2026-05-06 | 1 |

**`checksum`이 핵심 안전장치다.**

- 이미 적용된 `V1__init.sql`의 내용을 누가 수정 → 다음 기동 시 checksum 불일치 → **`FlywayValidateException`으로 기동 실패**
- → "한번 운영에 나간 마이그레이션은 절대 수정 못 한다"를 코드가 강제

---

## 🔥 왜 ERP에 Flyway가 필수인가

ERP는 **운영 중에도 스키마가 계속 바뀐다**:

- 회계 모듈에 새 계정과목 필드 추가
- 세법 개정으로 세금계산서 컬럼 추가
- 거래처에 신규 KYC 필드 요구

이런 변경이 **누가, 언제, 어떤 순서로 적용됐는지**가 운영 사고 추적에 필수. Git 커밋과 `flyway_schema_history`가 한 묶음으로 추적성을 보장한다.

**비교**: Hibernate `ddl-auto: update`를 쓰면?

- 변경 이력이 어디에도 안 남음
- 개발자별 로컬 DB와 운영 DB가 미묘하게 다를 수 있음 (인덱스 이름, 컬럼 순서 등)
- 운영 사고 시 "이 인덱스가 언제부터 있었지?"를 추적 불가

---

## V1 파일 자체에서 짚어야 할 것

```sql
CREATE TABLE schema_init_marker (
    ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 1. `ENGINE=InnoDB` 명시

- MySQL 8에서 기본값이긴 하지만 명시 = 명세
- InnoDB는 **트랜잭션 지원 + 행 단위 락**. 기간계의 전제조건
- MyISAM(트랜잭션 미지원)이 옛날엔 default였던 시절의 잔재 — 명시 안 하면 "혹시?"가 남는다

### 2. `DEFAULT CHARSET=utf8mb4`

- `utf8mb4` = **진짜 UTF-8** (이모지, 한자 보조면 OK)
- 옛날 MySQL의 `utf8`은 3바이트까지만 = **이모지 들어가면 깨짐**
- ERP에서도 "고객명에 이모지"는 드물지만 비고/메모 필드에는 들어옴

### 3. `created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`

- 이 패턴이 Phase 1부터 모든 마스터 테이블에 들어갈 것 (`created_at`, `updated_at`, `created_by`, `updated_by`)
- ERP는 "**누가 언제 데이터를 넣었는가**"가 감사 필수 — Phase 1에서 JPA Auditing으로 자동화

### 4. 마지막 `INSERT`

- "Phase 0 init OK" 한 줄을 박아두는 것 → 헬스체크 후 `SELECT * FROM schema_init_marker;` 한 번 쳐보면 마이그레이션이 진짜 실행됐는지 눈으로 확인 가능

---

## Phase 1에서 일어날 일 미리보기

Phase 1 시작 시:

- `V2__create_customer.sql` — 고객 마스터 테이블
- `V3__create_item.sql` — 상품 마스터 테이블
- ... 식으로 누적

→ 마이그레이션 파일들이 **ERP 도메인 모델의 역사가 된다**. 처음부터 끝까지 읽으면 "이 회사 시스템이 어떻게 자라났는지" 보임.

-- V26: 인증/인가 기반 테이블 (Phase 6)
-- 로그인 계정(app_user) + 역할(role) + 권한(permission) + N:M 조인 2개.
-- app_user 는 Employee 와 1:1(employee_id UNIQUE). 인사정보와 로그인정보의 관심사 분리.

-- 로그인 계정 (BaseEntity 상속 → 채번/소프트삭제 없음, enabled 로 활성 관리)
CREATE TABLE app_user (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT       NOT NULL COMMENT '직원 1:1 (로그인 주체)',
    username       VARCHAR(200) NOT NULL COMMENT '로그인 ID (= employee.email)',
    password_hash  VARCHAR(100) NOT NULL COMMENT 'BCrypt 해시 (평문 저장 금지)',
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '계정 활성 여부',
    account_locked BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '계정 잠금 여부',
    created_at     DATETIME     NOT NULL,
    created_by     VARCHAR(64)  NOT NULL,
    updated_at     DATETIME     NOT NULL,
    updated_by     VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_employee (employee_id),
    UNIQUE KEY uk_app_user_username (username),
    CONSTRAINT fk_app_user_employee FOREIGN KEY (employee_id) REFERENCES employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='로그인 계정';

-- 역할 (SALES/PURCHASING/FINANCE/ADMIN)
CREATE TABLE role (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL COMMENT '역할 코드 (ROLE_ 접두어 없이 저장)',
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL,
    created_by VARCHAR(64)  NOT NULL,
    updated_at DATETIME     NOT NULL,
    updated_by VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='역할';

-- 권한 (세부 권한 — 이번 Phase 는 역할 단위 인가, permission 은 구조+시드만)
CREATE TABLE permission (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(50)  NOT NULL COMMENT '권한 코드 (예: FI_POST)',
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL,
    created_by VARCHAR(64)  NOT NULL,
    updated_at DATETIME     NOT NULL,
    updated_by VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='권한';

-- 사용자 ↔ 역할 (N:M)
CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자-역할 매핑';

-- 역할 ↔ 권한 (N:M)
CREATE TABLE role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permission_perm (permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_role_permission_perm FOREIGN KEY (permission_id) REFERENCES permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='역할-권한 매핑';

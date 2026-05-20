-- Phase 1: 직원 마스터.
-- department FK 가 Phase 1 에서 유일한 마스터 간 관계.

CREATE TABLE employee (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(30)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(200) NOT NULL                COMMENT 'Phase 6 부터 로그인 ID 로 사용',
    department_id BIGINT       NOT NULL,
    hire_date     DATE         NOT NULL,
    status        VARCHAR(16)  NOT NULL,
    created_at    DATETIME     NOT NULL,
    created_by    VARCHAR(64)  NOT NULL,
    updated_at    DATETIME     NOT NULL,
    updated_by    VARCHAR(64)  NOT NULL,
    deleted_at    DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_code (code),
    UNIQUE KEY uk_employee_email (email),
    KEY idx_employee_department_id (department_id),
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='직원 마스터';

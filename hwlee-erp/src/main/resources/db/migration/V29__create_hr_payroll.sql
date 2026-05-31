-- Phase 7: HR 간이 모듈 — 급여계약 / 근태 / 급여대장(헤더-라인) 4테이블.
--
-- 설계:
--  - employment_contract: 직원의 발효일 기준 급여조건 이력. 시급 = base_salary / contracted_hours.
--  - attendance: 하루 한 건 누적(직원+일자 UNIQUE). 월 급여 계산 때 overtime_minutes 를 SUM.
--  - payroll_run(헤더) ↔ payslip(라인): "YYYY-MM 한 달치 급여" 묶음. period UNIQUE (월 1건).
--  - 모든 엔티티는 BaseEntity(soft delete 없음) — created/updated 4컬럼만.

-- 1) 급여계약
CREATE TABLE employment_contract (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id      BIGINT          NOT NULL                COMMENT '대상 직원',
    position         VARCHAR(16)     NOT NULL                COMMENT 'STAFF/SENIOR/MANAGER/DIRECTOR',
    base_salary      DECIMAL(15, 2)  NOT NULL                COMMENT '월 기본급',
    contracted_hours INT             NOT NULL                COMMENT '월 소정근로시간 (시급 환산 분모)',
    effective_from   DATE            NOT NULL                COMMENT '발효일',
    effective_to     DATE                                    COMMENT '만료일 (NULL=현재 유효)',
    status           VARCHAR(16)     NOT NULL                COMMENT 'ACTIVE/INACTIVE',
    created_at       DATETIME        NOT NULL,
    created_by       VARCHAR(64)     NOT NULL,
    updated_at       DATETIME        NOT NULL,
    updated_by       VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_contract_employee (employee_id),
    KEY idx_contract_effective (effective_from, effective_to),
    CONSTRAINT fk_contract_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT chk_contract_salary CHECK (base_salary > 0),
    CONSTRAINT chk_contract_hours CHECK (contracted_hours > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='급여계약 (발효일 이력)';

-- 2) 근태
CREATE TABLE attendance (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id      BIGINT          NOT NULL                COMMENT '대상 직원',
    work_date        DATE            NOT NULL                COMMENT '근무일',
    clock_in         TIME            NOT NULL                COMMENT '출근 시각',
    clock_out        TIME            NOT NULL                COMMENT '퇴근 시각',
    worked_minutes   INT             NOT NULL                COMMENT '파생: 근무 분 (출근~퇴근)',
    overtime_minutes INT             NOT NULL                COMMENT '파생: 소정(480분) 초과 분',
    created_at       DATETIME        NOT NULL,
    created_by       VARCHAR(64)     NOT NULL,
    updated_at       DATETIME        NOT NULL,
    updated_by       VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_emp_date (employee_id, work_date),
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='근태 (하루 한 건 누적)';

-- 3) 급여대장 (헤더)
CREATE TABLE payroll_run (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    number          VARCHAR(30)     NOT NULL                COMMENT '예: PR-202605-001',
    period          VARCHAR(7)      NOT NULL                COMMENT '대상 월 YYYY-MM',
    run_date        DATE            NOT NULL                COMMENT '대장 생성일',
    status          VARCHAR(16)     NOT NULL                COMMENT 'DRAFT/CONFIRMED/PAID',
    total_gross     DECIMAL(15, 2)  NOT NULL                COMMENT '총 지급액 합계 (캐시)',
    total_deduction DECIMAL(15, 2)  NOT NULL                COMMENT '총 공제액 합계 (캐시)',
    total_net       DECIMAL(15, 2)  NOT NULL                COMMENT '총 실수령 합계 (캐시)',
    confirmed_at    DATETIME                                COMMENT '확정 시각',
    paid_at         DATETIME                                COMMENT '지급 시각',
    created_at      DATETIME        NOT NULL,
    created_by      VARCHAR(64)     NOT NULL,
    updated_at      DATETIME        NOT NULL,
    updated_by      VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payroll_run_number (number),
    UNIQUE KEY uk_payroll_run_period (period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='급여대장 헤더 (월 1건)';

-- 4) 급여명세서 (라인)
CREATE TABLE payslip (
    id                 BIGINT          NOT NULL AUTO_INCREMENT,
    payroll_run_id     BIGINT          NOT NULL                COMMENT '소속 급여대장',
    employee_id        BIGINT          NOT NULL                COMMENT '대상 직원',
    base_pay           DECIMAL(15, 2)  NOT NULL                COMMENT '기본급 (만근)',
    overtime_pay       DECIMAL(15, 2)  NOT NULL                COMMENT '연장수당 (연장시간×시급×1.5)',
    gross_pay          DECIMAL(15, 2)  NOT NULL                COMMENT '= base_pay + overtime_pay',
    income_tax         DECIMAL(15, 2)  NOT NULL                COMMENT '소득세 공제',
    insurance_employee DECIMAL(15, 2)  NOT NULL                COMMENT '4대보험 직원분 공제',
    insurance_company  DECIMAL(15, 2)  NOT NULL                COMMENT '4대보험 회사분 (공제아님, 법정복리비)',
    total_deduction    DECIMAL(15, 2)  NOT NULL                COMMENT '= income_tax + insurance_employee',
    net_pay            DECIMAL(15, 2)  NOT NULL                COMMENT '= gross - total_deduction (실수령)',
    created_at         DATETIME        NOT NULL,
    created_by         VARCHAR(64)     NOT NULL,
    updated_at         DATETIME        NOT NULL,
    updated_by         VARCHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payslip_run_employee (payroll_run_id, employee_id),
    KEY idx_payslip_employee (employee_id),
    CONSTRAINT fk_payslip_run      FOREIGN KEY (payroll_run_id) REFERENCES payroll_run(id),
    CONSTRAINT fk_payslip_employee FOREIGN KEY (employee_id)    REFERENCES employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='급여명세서 라인';

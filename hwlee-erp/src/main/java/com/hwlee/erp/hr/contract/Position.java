package com.hwlee.erp.hr.contract;

/**
 * 직급 — 급여계약(EmploymentContract)의 분류. 학습용 4단계.
 * 급여 계산엔 직접 쓰이지 않고(기본급이 진실), 조회/리포트용 메타데이터.
 */
public enum Position {
    STAFF,     // 사원
    SENIOR,    // 선임
    MANAGER,   // 책임/과장
    DIRECTOR   // 임원/이사
}

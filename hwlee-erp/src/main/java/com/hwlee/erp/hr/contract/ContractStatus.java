package com.hwlee.erp.hr.contract;

/**
 * 급여계약 상태.
 * <pre>
 * ACTIVE ──terminate()──▶ INACTIVE
 * </pre>
 * 새 계약이 발효되면 직전 ACTIVE 계약을 INACTIVE 로 닫는다(effective_to 설정).
 */
public enum ContractStatus {
    ACTIVE,
    INACTIVE
}

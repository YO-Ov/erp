package com.hwlee.erp.fi.journal;

/**
 * 자동 분개가 사용하는 시스템 계정 코드 상수.
 *
 * <p>설계 결정 #7 — 범용 규칙 엔진 대신 "사건별 자바 메서드 + 코드 상수" 로 단순화.
 * 분개 규칙이 코드로 한눈에 읽힌다.
 *
 * <p>실무 ERP 는 보통 이 매핑을 DB(예: 회계 키 테이블) 로 빼서 회사별/사업장별로 다르게 두지만,
 * 학습 단계엔 코드 상수가 충분히 명확하다.
 */
public final class SystemAccounts {

    public static final String CASH = "1100";          // 현금
    public static final String AR = "1200";            // 매출채권 (Account Receivable)
    public static final String INVENTORY = "1400";     // 재고자산
    public static final String AP = "2100";            // 매입채무 (Account Payable)
    public static final String VAT_PAYABLE = "2200";   // 부가세예수금 (매출 시)
    public static final String SALES = "4100";         // 매출
    public static final String COGS = "5100";          // 매출원가 (Cost of Goods Sold)

    // Phase 7 — HR 인건비 분개용 계정
    public static final String SALARY_EXPENSE = "5200";        // 급여비용 (직원 총지급액 gross)
    public static final String LEGAL_WELFARE = "5300";         // 법정복리비 (4대보험 회사부담분)
    public static final String WITHHOLDING_TAX = "2400";       // 예수금-소득세 (떼어둔 소득세)
    public static final String WITHHOLDING_INSURANCE = "2500"; // 예수금-사회보험 (4대보험 직원분+회사분)
    public static final String SALARY_PAYABLE = "2600";        // 미지급급여 (직원에게 줄 실수령액)

    private SystemAccounts() {}
}

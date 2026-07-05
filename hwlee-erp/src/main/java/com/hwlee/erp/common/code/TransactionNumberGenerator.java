package com.hwlee.erp.common.code;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 트랜잭션 번호 발급 래퍼 (모듈 공통).
 *
 * <p>형식: {@code <PREFIX>-<YYYYMMDD>-<NNN>} — 예: {@code SO-20260524-001}.
 * 같은 날짜 동안 prefix 별 일련번호가 1부터 증가하며, 자정이 지나면 새 periodKey 로 리셋된다.
 *
 * <p>{@link CodeGenerator#nextTransactionCode(String, String)} 에 위임 — 동시성 안전성과
 * "번호 구멍" 트레이드오프(REQUIRES_NEW 트랜잭션)는 위임 메서드 정의 참고.
 */
@Component
@RequiredArgsConstructor
public class TransactionNumberGenerator {

    static final String PREFIX_QUOTATION = "Q";
    static final String PREFIX_SALES_ORDER = "SO";
    static final String PREFIX_DELIVERY = "DLV";
    static final String PREFIX_INVOICE = "INV";
    static final String PREFIX_GOODS_RECEIPT = "GR";
    static final String PREFIX_GOODS_ISSUE = "GI";
    static final String PREFIX_JOURNAL_ENTRY = "JE";
    static final String PREFIX_PAYMENT = "PAY";
    static final String PREFIX_PAYROLL_RUN = "PR";
    static final String PREFIX_PRODUCTION_ORDER = "PO";
    static final String PREFIX_PLANNED_ORDER = "PLO";
    static final String PREFIX_CREDIT_REQUEST = "CLR";
    static final String PREFIX_APPROVAL = "APV";

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final CodeGenerator codeGenerator;

    public String nextQuotationNumber(LocalDate issuedDate) {
        return codeGenerator.nextTransactionCode(PREFIX_QUOTATION, periodKey(issuedDate));
    }

    public String nextSalesOrderNumber(LocalDate orderDate) {
        return codeGenerator.nextTransactionCode(PREFIX_SALES_ORDER, periodKey(orderDate));
    }

    public String nextDeliveryNumber(LocalDate shippedDate) {
        return codeGenerator.nextTransactionCode(PREFIX_DELIVERY, periodKey(shippedDate));
    }

    public String nextInvoiceNumber(LocalDate invoiceDate) {
        return codeGenerator.nextTransactionCode(PREFIX_INVOICE, periodKey(invoiceDate));
    }

    public String nextGoodsReceiptNumber(LocalDate receiptDate) {
        return codeGenerator.nextTransactionCode(PREFIX_GOODS_RECEIPT, periodKey(receiptDate));
    }

    public String nextGoodsIssueNumber(LocalDate issueDate) {
        return codeGenerator.nextTransactionCode(PREFIX_GOODS_ISSUE, periodKey(issueDate));
    }

    public String nextJournalEntryNumber(LocalDate entryDate) {
        return codeGenerator.nextTransactionCode(PREFIX_JOURNAL_ENTRY, periodKey(entryDate));
    }

    public String nextPaymentNumber(LocalDate paymentDate) {
        return codeGenerator.nextTransactionCode(PREFIX_PAYMENT, periodKey(paymentDate));
    }

    /** 생산지시 번호 — 일 단위. 예: {@code PO-20260604-001}. */
    public String nextProductionOrderNumber(LocalDate orderDate) {
        return codeGenerator.nextTransactionCode(PREFIX_PRODUCTION_ORDER, periodKey(orderDate));
    }

    /** 계획오더(MRP 제안) 번호 — 일 단위. 예: {@code PLO-20260610-001}. */
    public String nextPlannedOrderNumber(LocalDate orderDate) {
        return codeGenerator.nextTransactionCode(PREFIX_PLANNED_ORDER, periodKey(orderDate));
    }

    /** 여신(신용한도) 상향 요청 번호 — 일 단위. 예: {@code CLR-20260610-001}. */
    public String nextCreditLimitRequestNumber(LocalDate requestDate) {
        return codeGenerator.nextTransactionCode(PREFIX_CREDIT_REQUEST, periodKey(requestDate));
    }

    /** 전자결재 문서 번호 — 일 단위. 예: {@code APV-20260705-001}. */
    public String nextApprovalNumber(LocalDate requestDate) {
        return codeGenerator.nextTransactionCode(PREFIX_APPROVAL, periodKey(requestDate));
    }

    /**
     * 급여대장 번호 — 월 단위 periodKey 로 발급. 예: {@code PR-202605-001}.
     * 다른 트랜잭션 번호(일 단위)와 달리 급여는 "월 1건" 이므로 월 키를 쓴다.
     */
    public String nextPayrollNumber(java.time.YearMonth period) {
        if (period == null) {
            throw new IllegalArgumentException("period 는 null 일 수 없다.");
        }
        return codeGenerator.nextTransactionCode(PREFIX_PAYROLL_RUN, period.format(MONTH_FORMAT));
    }

    private static String periodKey(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date 는 null 일 수 없다.");
        }
        return date.format(PERIOD_FORMAT);
    }
}

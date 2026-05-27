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

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

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

    private static String periodKey(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date 는 null 일 수 없다.");
        }
        return date.format(PERIOD_FORMAT);
    }
}

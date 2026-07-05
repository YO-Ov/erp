package com.hwlee.erp.approval;

/**
 * 전자결재 대상 문서 종류 — 하나의 범용 결재 엔진이 여러 업무 문서를 태운다.
 *
 * <p>현재는 견적({@link #QUOTATION})만 연동돼 있고, 구매발주·지급결의·전표 등은
 * 단계적으로 추가한다. 전결 규정({@code approval_rule})은 이 문서 종류별로 정의된다.
 */
public enum ApprovalDocType {
    QUOTATION("견적", "/sd/quotations/"),
    SALES_ORDER("수주", "/sd/sales-orders/"),
    PURCHASE_ORDER("구매발주", "/mm/purchase-orders/"),
    PAYMENT("지급결의", "/fi/payments/"),
    JOURNAL("전표", "/fi/journal-entries/"),
    CREDIT_LIMIT("여신 상향", "/fi/credit-limit-requests?id=");

    private final String label;
    /** 원본 문서 상세 화면 경로 접두어. 딥링크 = linkPrefix + refId. */
    private final String linkPrefix;

    ApprovalDocType(String label, String linkPrefix) {
        this.label = label;
        this.linkPrefix = linkPrefix;
    }

    public String label() {
        return label;
    }

    public String linkTo(Long refId) {
        return linkPrefix + refId;
    }
}

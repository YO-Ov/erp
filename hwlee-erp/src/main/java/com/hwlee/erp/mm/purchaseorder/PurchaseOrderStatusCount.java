package com.hwlee.erp.mm.purchaseorder;

import java.math.BigDecimal;

/** 발주 상태별 집계(건수·금액) 프로젝션 — 구매 대시보드 파이프라인용. */
public interface PurchaseOrderStatusCount {
    PurchaseOrderStatus getStatus();

    long getCount();

    BigDecimal getAmount();
}

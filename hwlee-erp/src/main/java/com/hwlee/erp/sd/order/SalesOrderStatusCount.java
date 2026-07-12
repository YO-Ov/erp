package com.hwlee.erp.sd.order;

import java.math.BigDecimal;

/** 수주 상태별 집계(건수·금액) 프로젝션 — 대시보드 파이프라인용. */
public interface SalesOrderStatusCount {
    SalesOrderStatus getStatus();

    long getCount();

    BigDecimal getAmount();
}

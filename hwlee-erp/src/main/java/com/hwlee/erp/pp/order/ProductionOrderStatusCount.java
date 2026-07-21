package com.hwlee.erp.pp.order;

/** 생산지시 상태별 집계(건수) 프로젝션 — 대시보드 파이프라인용. */
public interface ProductionOrderStatusCount {
    ProductionOrderStatus getStatus();

    long getCount();
}

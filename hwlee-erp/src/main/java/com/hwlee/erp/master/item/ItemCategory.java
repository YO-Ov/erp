package com.hwlee.erp.master.item;

/**
 * 상품 카테고리. hyunwoo전자는 노트북과 모니터 2종으로 시작한다.
 */
public enum ItemCategory {
    NOTEBOOK,
    MONITOR,
    /** 부품/원재료 — 생산(PP)에 투입되는 자재(LCD/메모리/SSD 등). Phase 8 추가. */
    PART
}

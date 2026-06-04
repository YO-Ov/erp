package com.hwlee.erp.master.item;

/**
 * 품목 역할 구분 — 생산(PP)에서 BOM/생산지시가 완제품과 부품을 가른다.
 *
 * <ul>
 *   <li>{@link #FINISHED} 완제품 — 판매 대상. BOM의 부모(만들 대상). 재고 회계상 '제품'.</li>
 *   <li>{@link #COMPONENT} 부품/원재료 — 생산에 투입. BOM의 자식. 재고 회계상 '원재료'.</li>
 * </ul>
 *
 * <p>{@code category}(노트북/모니터/부품…)와 직교하는 '역할' 축이다. 기존 품목은 모두 FINISHED.
 */
public enum ItemType {
    FINISHED,
    COMPONENT
}

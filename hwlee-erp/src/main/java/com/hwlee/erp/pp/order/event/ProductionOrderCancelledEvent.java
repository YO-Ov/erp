package com.hwlee.erp.pp.order.event;

/**
 * 생산지시 취소 사건 — {@link com.hwlee.erp.pp.order.ProductionService#cancel} 가 발행한다.
 *
 * <p>계획오더(MRP)에서 전환돼 만들어진 생산지시가 취소되면, 같은 PP 모듈의 planning 리스너가
 * 이를 받아 원래 계획오더를 검토 대기(PROPOSED)로 되살린다 — 부족분이 아직 미해결임을 담당자에게
 * 다시 알리기 위함. 이벤트로 두는 이유: order ↔ planning 직접 의존(순환)을 피하려는 것.
 *
 * @param productionOrderId 취소된 생산지시 id
 * @param productionNumber  취소된 생산지시 번호 — 계획오더의 {@code convertedProductionNumber} 와 매칭
 */
public record ProductionOrderCancelledEvent(
        Long productionOrderId,
        String productionNumber
) {}

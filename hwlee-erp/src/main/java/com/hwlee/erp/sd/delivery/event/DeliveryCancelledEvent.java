package com.hwlee.erp.sd.delivery.event;

/**
 * 출하 취소 사건 — Delivery 가 CANCELLED 로 전이될 때 SD 가 발행한다.
 *
 * <p>{@link DeliveryShippedEvent} 와 달리 <b>라인 정보를 담지 않는다</b>(설계 §2.2):
 * 취소 listener 는 {@code deliveryId} 로 연결된 GoodsIssue 를 직접 찾아 그 GI 의 라인을
 * 복원하는 게 더 깔끔하다(라인 매칭 로직 불필요). 차감했던 창고도 GI 가 알고 있으므로
 * 이벤트에 warehouseId 조차 필요 없다.
 *
 * @param deliveryId 취소된 출하 ID — listener 가 이 ID 로 GoodsIssue 를 역추적한다.
 */
public record DeliveryCancelledEvent(
        Long deliveryId
) {}

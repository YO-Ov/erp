package com.hwlee.erp.sd.delivery.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 출하 확정 사건 — Delivery 가 SHIPPED 로 확정될 때 SD 가 발행한다.
 *
 * <p><b>이벤트는 발행자(SD)가 소유</b>한다. MM 모듈이 이 타입을 import 해서 구독할 뿐,
 * SD 는 누가 듣는지 모른다 — 의존 방향은 {@code MM → SD} 단방향.
 *
 * <p>본문은 <b>ID + 핵심 라인 정보</b>를 함께 담는다(설계 §2.2 옵션 B). listener 가
 * 같은 트랜잭션 안에서 즉시 사용하므로 다시 DB 를 조회할 필요가 없고, 일관성 우려도 없다.
 *
 * @param deliveryId  원천 출하 ID — 생성될 GoodsIssue 가 강한 FK 로 역참조한다.
 * @param warehouseId 출하지 창고 ID — MM 이 재고를 차감할 대상 창고.
 * @param shippedDate 출하일 — GoodsIssue 번호 채번/날짜에 사용.
 * @param lines       (itemId, quantity) 목록 — 차감할 품목과 수량.
 */
public record DeliveryShippedEvent(
        Long deliveryId,
        Long warehouseId,
        LocalDate shippedDate,
        List<Line> lines
) {
    public record Line(Long itemId, BigDecimal quantity) {}
}

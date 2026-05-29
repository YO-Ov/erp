package com.hwlee.erp.mm.goodsreceipt.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 입고 확정 사건 — {@link com.hwlee.erp.mm.goodsreceipt.GoodsReceiptService#post} 가 발행.
 *
 * <p>Phase 5 신규. 회계(FI) 모듈이 구독해 매입 분개를 자동 생성한다:
 * <pre>
 *   차) 재고자산 {totalCost} / 대) 매입채무 {totalCost}
 * </pre>
 * 학습 1차 범위에선 매입 부가세(부가세대급금) 는 분리하지 않는다 — 단가에 포함된 것으로 가정.
 */
public record GoodsReceiptPostedEvent(
        Long goodsReceiptId,
        String number,
        LocalDate receiptDate,
        List<Line> lines
) {
    public record Line(Long itemId, BigDecimal quantity, BigDecimal unitCost) {}
}

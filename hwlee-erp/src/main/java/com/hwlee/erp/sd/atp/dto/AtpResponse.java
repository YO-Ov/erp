package com.hwlee.erp.sd.atp.dto;

import java.math.BigDecimal;

/**
 * 약속가능재고(ATP, Available-To-Promise) 현황.
 *
 * <p>{@code atp = onHand - committed + inboundProduction}. 영업이 수주를 약속하기 전에
 * "지금 이 품목을 몇 개까지 안전하게 약속할 수 있나" 를 본다.
 */
public record AtpResponse(
        Long itemId,
        String itemCode,
        String itemName,
        BigDecimal onHand,             // 현재고 (전 창고 보유량 합)
        BigDecimal committed,          // 미출하 확정수주 (orderQty - shippedQty)
        BigDecimal inboundProduction,  // 진행 중 생산예정 (PLANNED/RELEASED 생산지시 수량)
        BigDecimal atp                 // 약속가능재고
) {}

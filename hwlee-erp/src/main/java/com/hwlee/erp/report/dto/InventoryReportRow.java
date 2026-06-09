package com.hwlee.erp.report.dto;

import java.math.BigDecimal;

/**
 * 재고 현황 리포트 한 행 — (품목, 창고)별 보유 수량·평균단가·평가액.
 */
public record InventoryReportRow(
        String itemCode,
        String itemName,
        String warehouseName,
        BigDecimal qtyOnHand,
        BigDecimal averageCost,
        BigDecimal valuationAmount) {
}

package com.hwlee.erp.pp.integration.mes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ERP → MES 작업지시 전송 본문. {@code erpOrderNo}(생산지시번호)가 멱등 키.
 * (계약: contracts/openapi/erp-to-mes-workorder.yaml)
 */
public record MesWorkOrderRequest(
        String erpOrderNo,
        String productCode,
        String productName,
        BigDecimal quantity,
        LocalDate plannedDate,
        List<ComponentLine> components) {

    public record ComponentLine(
            String componentCode,
            String componentName,
            BigDecimal requiredQty,
            String unit) {
    }
}

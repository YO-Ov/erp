package com.hwlee.erp.pp.integration.mes;

import com.hwlee.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MES 가 통보한 불량 발생 기록(통계용). ERP 재고/회계엔 영향 없음 — 품질 상세는 MES 소유.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quality_defect_log")
public class QualityDefectLog extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 80)
    private String eventId;

    @Column(name = "work_order_no", nullable = false, length = 30)
    private String workOrderNo;

    @Column(name = "erp_order_no", nullable = false, length = 30)
    private String erpOrderNo;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Column(name = "defect_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal defectQty;

    @Column(name = "defect_reason_code", length = 30)
    private String defectReasonCode;

    @Column(name = "defect_reason_name", length = 100)
    private String defectReasonName;

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    public static QualityDefectLog of(QualityDefectMessage m, LocalDateTime receivedAt) {
        QualityDefectLog log = new QualityDefectLog();
        log.eventId = m.eventId();
        log.workOrderNo = m.workOrderNo();
        log.erpOrderNo = m.erpOrderNo();
        log.productCode = m.productCode();
        log.defectQty = m.defectQty();
        log.defectReasonCode = m.defectReasonCode();
        log.defectReasonName = m.defectReasonName();
        log.inspectedAt = m.inspectedAt();
        log.receivedAt = receivedAt;
        return log;
    }
}

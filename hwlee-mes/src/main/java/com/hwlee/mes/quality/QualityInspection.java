package com.hwlee.mes.quality;

import com.hwlee.mes.common.BaseEntity;
import com.hwlee.mes.workorder.WorkOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 품질 검사 — 작업지시 산출물에 대한 합격/불량 판정.
 * 불량(defectQty &gt; 0)이면 사유가 필수이며 결과는 FAIL, 이 사실이 ERP 로 통보된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quality_inspection")
public class QualityInspection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "inspected_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal inspectedQty;

    @Column(name = "passed_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal passedQty;

    @Column(name = "defect_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal defectQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defect_reason_id")
    private DefectReason defectReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 16)
    private QualityResult result;

    @Column(name = "inspected_at", nullable = false)
    private LocalDateTime inspectedAt;

    @Column(name = "note", length = 255)
    private String note;

    public static QualityInspection of(WorkOrder workOrder, BigDecimal inspectedQty, BigDecimal passedQty,
                                       BigDecimal defectQty, DefectReason defectReason,
                                       LocalDateTime inspectedAt, String note) {
        QualityInspection qi = new QualityInspection();
        qi.workOrder = workOrder;
        qi.inspectedQty = inspectedQty;
        qi.passedQty = passedQty;
        qi.defectQty = defectQty;
        qi.defectReason = defectReason;
        qi.result = (defectQty != null && defectQty.signum() > 0) ? QualityResult.FAIL : QualityResult.PASS;
        qi.inspectedAt = inspectedAt;
        qi.note = note;
        return qi;
    }

    public boolean hasDefect() {
        return result == QualityResult.FAIL;
    }
}

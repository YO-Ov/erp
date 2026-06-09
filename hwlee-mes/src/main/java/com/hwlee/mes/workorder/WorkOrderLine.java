package com.hwlee.mes.workorder;

import com.hwlee.mes.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 작업지시 소요 자재 라인 — ERP 가 내려준 부품 소요량(현장 자재 준비용).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "work_order_line")
public class WorkOrderLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "component_code", nullable = false, length = 30)
    private String componentCode;

    @Column(name = "component_name", nullable = false, length = 100)
    private String componentName;

    @Column(name = "required_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal requiredQty;

    @Column(name = "unit", length = 20)
    private String unit;

    WorkOrderLine(WorkOrder workOrder, int lineNo, String componentCode, String componentName,
                  BigDecimal requiredQty, String unit) {
        this.workOrder = workOrder;
        this.lineNo = lineNo;
        this.componentCode = componentCode;
        this.componentName = componentName;
        this.requiredQty = requiredQty;
        this.unit = unit;
    }
}

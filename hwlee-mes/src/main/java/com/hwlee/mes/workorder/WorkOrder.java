package com.hwlee.mes.workorder;

import com.hwlee.mes.common.BaseEntity;
import com.hwlee.mes.master.equipment.Equipment;
import com.hwlee.mes.master.operator.Operator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 작업지시 — ERP 생산지시(PO)가 MES 로 내려온 것. 같은 실체에 대한 MES 의 관점.
 *
 * <p>{@code erpOrderNo}(ERP PO 번호)에 UNIQUE 제약을 두어 <b>멱등 수신</b>의 키로 쓴다.
 * 같은 PO 가 두 번 들어와도 한 번만 등록된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "work_order")
public class WorkOrder extends BaseEntity {

    @Column(name = "work_order_no", nullable = false, unique = true, length = 30)
    private String workOrderNo;

    @Column(name = "erp_order_no", nullable = false, unique = true, length = 30)
    private String erpOrderNo;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private WorkOrderStatus status = WorkOrderStatus.RECEIVED;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    // ── Phase 13: 현장 실행 ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_equipment_id")
    private Equipment assignedEquipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_operator_id")
    private Operator assignedOperator;

    @Column(name = "produced_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal producedQty = BigDecimal.ZERO;

    @Column(name = "defect_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal defectQty = BigDecimal.ZERO;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<WorkOrderLine> lines = new ArrayList<>();

    public static WorkOrder received(String workOrderNo, String erpOrderNo, String productCode,
                                     String productName, BigDecimal quantity, LocalDate plannedDate,
                                     LocalDateTime receivedAt) {
        WorkOrder wo = new WorkOrder();
        wo.workOrderNo = workOrderNo;
        wo.erpOrderNo = erpOrderNo;
        wo.productCode = productCode;
        wo.productName = productName;
        wo.quantity = quantity;
        wo.plannedDate = plannedDate;
        wo.receivedAt = receivedAt;
        wo.status = WorkOrderStatus.RECEIVED;
        return wo;
    }

    public void addLine(String componentCode, String componentName, BigDecimal requiredQty, String unit) {
        lines.add(new WorkOrderLine(this, lines.size() + 1, componentCode, componentName, requiredQty, unit));
    }

    // ── Phase 13: 현장 실행 상태전이 ──

    /** 작업 시작 — 설비·작업자 배정. RECEIVED → IN_PROGRESS. */
    public void start(Equipment equipment, Operator operator, LocalDateTime now) {
        if (status != WorkOrderStatus.RECEIVED) {
            throw new IllegalStateException("RECEIVED 상태에서만 시작할 수 있습니다. 현재: " + status);
        }
        this.assignedEquipment = equipment;
        this.assignedOperator = operator;
        this.startedAt = now;
        this.status = WorkOrderStatus.IN_PROGRESS;
    }

    public void pause() {
        if (status != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("IN_PROGRESS 상태에서만 일시정지할 수 있습니다. 현재: " + status);
        }
        this.status = WorkOrderStatus.PAUSED;
    }

    public void resume() {
        if (status != WorkOrderStatus.PAUSED) {
            throw new IllegalStateException("PAUSED 상태에서만 재개할 수 있습니다. 현재: " + status);
        }
        this.status = WorkOrderStatus.IN_PROGRESS;
    }

    /** 실적 누적 — 진행 중일 때만. 양품/불량 수량을 더한다. */
    public void addProduction(BigDecimal goodQty, BigDecimal defectQtyToAdd) {
        if (status != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("IN_PROGRESS 상태에서만 실적을 등록할 수 있습니다. 현재: " + status);
        }
        if (goodQty == null || goodQty.signum() < 0) {
            throw new IllegalArgumentException("양품 수량은 0 이상이어야 합니다.");
        }
        BigDecimal defect = (defectQtyToAdd == null) ? BigDecimal.ZERO : defectQtyToAdd;
        if (defect.signum() < 0) {
            throw new IllegalArgumentException("불량 수량은 0 이상이어야 합니다.");
        }
        this.producedQty = this.producedQty.add(goodQty);
        this.defectQty = this.defectQty.add(defect);
    }

    /** 작업 완료. IN_PROGRESS → COMPLETED. */
    public void complete(LocalDateTime now) {
        if (status != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("IN_PROGRESS 상태에서만 완료할 수 있습니다. 현재: " + status);
        }
        this.finishedAt = now;
        this.status = WorkOrderStatus.COMPLETED;
    }

    public List<WorkOrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}

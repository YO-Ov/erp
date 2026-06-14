package com.hwlee.mes.performance;

import com.hwlee.mes.common.BaseEntity;
import com.hwlee.mes.quality.DefectReason;
import com.hwlee.mes.workorder.WorkOrder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 생산 실적 — 한 작업지시에 대한 한 번의 실적 보고(부분 실적 가능).
 *
 * <p>현장에서 "방금 양품 N개·불량 M개 만들었다"를 기록한다. 같은 작업지시에 여러 건이 쌓인다.
 * 각 실적에는 BOM 비례로 계산된 자재 투입({@link MaterialConsumption})이 함께 기록된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "production_result")
public class ProductionResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "seq", nullable = false)
    private int seq;

    @Column(name = "good_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal goodQty;

    @Column(name = "defect_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal defectQty;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    /** 불량 사유(코드). 불량이 없으면 null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defect_reason_id")
    private DefectReason defectReason;

    @OneToMany(mappedBy = "productionResult", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<MaterialConsumption> consumptions = new ArrayList<>();

    public static ProductionResult of(WorkOrder workOrder, int seq, BigDecimal goodQty,
                                      BigDecimal defectQty, LocalDateTime reportedAt, DefectReason defectReason) {
        ProductionResult r = new ProductionResult();
        r.workOrder = workOrder;
        r.seq = seq;
        r.goodQty = goodQty;
        r.defectQty = (defectQty == null) ? BigDecimal.ZERO : defectQty;
        r.reportedAt = reportedAt;
        r.defectReason = defectReason;
        return r;
    }

    public void addConsumption(String componentCode, String componentName, BigDecimal consumedQty) {
        consumptions.add(new MaterialConsumption(this, componentCode, componentName, consumedQty));
    }

    public List<MaterialConsumption> getConsumptions() {
        return Collections.unmodifiableList(consumptions);
    }
}

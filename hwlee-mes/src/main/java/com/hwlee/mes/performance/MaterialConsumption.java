package com.hwlee.mes.performance;

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
 * 자재 투입 — 한 생산 실적에서 소모된 부품. BOM 단위소요 × 실적 양품수량으로 산출.
 * Phase 14 에서 이 데이터가 ERP 로 전송되어 재고 차감의 근거가 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "material_consumption")
public class MaterialConsumption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_result_id", nullable = false)
    private ProductionResult productionResult;

    @Column(name = "component_code", nullable = false, length = 30)
    private String componentCode;

    @Column(name = "component_name", nullable = false, length = 100)
    private String componentName;

    @Column(name = "consumed_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal consumedQty;

    MaterialConsumption(ProductionResult productionResult, String componentCode,
                        String componentName, BigDecimal consumedQty) {
        this.productionResult = productionResult;
        this.componentCode = componentCode;
        this.componentName = componentName;
        this.consumedQty = consumedQty;
    }
}

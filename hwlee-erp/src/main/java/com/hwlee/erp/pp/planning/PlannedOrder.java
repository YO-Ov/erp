package com.hwlee.erp.pp.planning;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.item.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 계획오더 (MRP 제안) — "수주는 들어왔는데 재고가 모자라니, 이만큼 생산하면 됩니다" 라는 제안.
 *
 * <p>수주 확정 시 {@code pp.integration.sd} 리스너가 완제품별로 <b>주문량 vs 현재고</b>를 비교해
 * 부족분({@link #shortageQty})만큼 자동 생성한다(PROPOSED). 이건 아직 자재·설비를 잡지 않는 "제안"이며,
 * 생산 담당자가 검토 후 {@link #markConverted}(승인→생산지시) 또는 {@link #dismiss}(기각)한다.
 *
 * <p>창고는 일부러 담지 않는다 — "어느 창고에 생산해 넣을지" 는 계획 단계엔 미정이고, 전환할 때
 * 담당자가 지정한다(실무도 동일). {@link #requiredQty}·{@link #onHandQty} 는 제안 <b>근거</b>를
 * 보여주기 위한 생성 시점 스냅샷이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "planned_order")
public class PlannedOrder extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    /** 생산해야 할 완제품 (itemType=FINISHED). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_item_id", nullable = false)
    private Item product;

    /** 총 필요량 = 수주 라인의 주문량 (제안 근거). */
    @Column(name = "required_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal requiredQty;

    /** 생성 시점 현재고(전 창고 보유 합) 스냅샷 (제안 근거). */
    @Column(name = "on_hand_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal onHandQty;

    /** 부족분 = requiredQty - onHandQty = 생산 제안 수량. 항상 > 0. */
    @Column(name = "shortage_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal shortageQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PlannedOrderStatus status = PlannedOrderStatus.PROPOSED;

    /** 이 제안을 촉발한 수주 (추적용). */
    @Column(name = "source_sales_order_id")
    private Long sourceSalesOrderId;

    @Column(name = "source_sales_order_number", length = 30)
    private String sourceSalesOrderNumber;

    /** 전환된 생산지시 번호 (CONVERTED 후 채워짐, 추적용). */
    @Column(name = "converted_production_number", length = 30)
    private String convertedProductionNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    /**
     * 부족분이 있는 완제품에 대해 계획오더를 제안한다. 호출 측이 shortage > 0 을 보장한 뒤 부른다.
     */
    public static PlannedOrder propose(String number, Item product, BigDecimal requiredQty,
                                       BigDecimal onHandQty, Long sourceSalesOrderId,
                                       String sourceSalesOrderNumber, LocalDate orderDate) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (product == null) throw new IllegalArgumentException("product 는 null 일 수 없다.");
        if (requiredQty == null || onHandQty == null) throw new IllegalArgumentException("수량은 null 일 수 없다.");
        BigDecimal shortage = requiredQty.subtract(onHandQty);
        if (shortage.signum() <= 0)
            throw new IllegalArgumentException("부족분이 없는 완제품엔 계획오더를 만들지 않는다: " + product.getCode());
        if (orderDate == null) throw new IllegalArgumentException("orderDate 는 null 일 수 없다.");

        PlannedOrder po = new PlannedOrder();
        po.number = number;
        po.product = product;
        po.requiredQty = requiredQty;
        po.onHandQty = onHandQty;
        po.shortageQty = shortage;
        po.sourceSalesOrderId = sourceSalesOrderId;
        po.sourceSalesOrderNumber = sourceSalesOrderNumber;
        po.orderDate = orderDate;
        return po;
    }

    /** 승인 — PROPOSED → CONVERTED. 전환으로 만들어진 생산지시 번호를 기록한다. */
    public void markConverted(String productionNumber) {
        if (status != PlannedOrderStatus.PROPOSED)
            throw new IllegalStateException("검토 대기(PROPOSED) 계획오더만 생산지시로 전환할 수 있습니다. 현재: " + status);
        this.status = PlannedOrderStatus.CONVERTED;
        this.convertedProductionNumber = productionNumber;
    }

    /** 기각 — PROPOSED → DISMISSED. 담당자가 "이번엔 생산하지 않음" 으로 판단. */
    public void dismiss() {
        if (status != PlannedOrderStatus.PROPOSED)
            throw new IllegalStateException("검토 대기(PROPOSED) 계획오더만 기각할 수 있습니다. 현재: " + status);
        this.status = PlannedOrderStatus.DISMISSED;
    }

    /**
     * 전환 취소 — CONVERTED → PROPOSED. 전환으로 만든 생산지시가 취소되면 제안을 되살려
     * 담당자의 검토 대기 목록에 다시 띄운다(부족분이 아직 미해결임을 알림). 견적 → 수주의
     * {@code Quotation.revertConversion} 과 같은 패턴.
     */
    public void revertConversion() {
        if (status != PlannedOrderStatus.CONVERTED)
            throw new IllegalStateException("전환됨(CONVERTED) 계획오더만 전환 취소할 수 있습니다. 현재: " + status);
        this.status = PlannedOrderStatus.PROPOSED;
        this.convertedProductionNumber = null;
    }
}

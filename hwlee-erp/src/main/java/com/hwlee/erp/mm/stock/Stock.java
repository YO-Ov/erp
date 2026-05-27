package com.hwlee.erp.mm.stock;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.mm.warehouse.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 캐시 — (상품, 창고) 당 한 행.
 *
 * <p>{@link #qtyOnHand} 는 "현재 보유" 의 캐시. 진실의 원천은 {@link StockMovement} 의 누적.
 *
 * <p>동시성 보호 두 겹:
 * <ul>
 *   <li>{@link Version} — 모든 UPDATE 에 자동으로 {@code WHERE version = ?} 추가 (낙관 락).
 *       입고 경로의 기본 보호.</li>
 *   <li>출고 경로({@code GoodsIssueService.post}) 는 추가로 {@link StockRepository#findForUpdate}
 *       (PESSIMISTIC_WRITE) 로 행을 점유 — 동시 차감 race 자체를 막는다.</li>
 * </ul>
 *
 * <p>외부에서 직접 setter 호출 금지. {@link #receive} / {@link #issue} 도메인 메서드로만 변경.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "stock",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_item_warehouse",
                columnNames = {"item_id", "warehouse_id"})
)
public class Stock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "qty_on_hand", nullable = false, precision = 18, scale = 4)
    private BigDecimal qtyOnHand = BigDecimal.ZERO;

    @Column(name = "average_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal averageCost = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static Stock empty(Item item, Warehouse warehouse) {
        if (item == null) throw new IllegalArgumentException("item 은 null 일 수 없다.");
        if (warehouse == null) throw new IllegalArgumentException("warehouse 는 null 일 수 없다.");
        Stock s = new Stock();
        s.item = item;
        s.warehouse = warehouse;
        s.qtyOnHand = BigDecimal.ZERO;
        s.averageCost = BigDecimal.ZERO;
        return s;
    }

    /**
     * 입고 — 가중평균으로 평균 단가를 갱신하고 보유량을 늘린다.
     *
     * <pre>
     * 새 평균 = (기존 수량 × 기존 평균 + 입고 수량 × 입고 단가) / (기존 수량 + 입고 수량)
     * </pre>
     */
    public void receive(BigDecimal qty, BigDecimal unitCost) {
        if (qty == null || qty.signum() <= 0)
            throw new IllegalArgumentException("입고 수량은 0보다 커야 한다.");
        if (unitCost == null || unitCost.signum() < 0)
            throw new IllegalArgumentException("입고 단가는 0 이상이어야 한다.");

        BigDecimal newQty = this.qtyOnHand.add(qty);
        if (this.qtyOnHand.signum() == 0) {
            // 첫 입고 (또는 0 → 양수) — 입고 단가가 곧 평균.
            this.averageCost = unitCost;
        } else {
            BigDecimal totalValue = this.qtyOnHand.multiply(this.averageCost)
                    .add(qty.multiply(unitCost));
            this.averageCost = totalValue.divide(newQty, 2, RoundingMode.HALF_UP);
        }
        this.qtyOnHand = newQty;
    }

    /**
     * 출고 — 보유량을 차감한다. 평균 단가는 유지.
     *
     * @return 이번 출고에 적용된 단가 (= 차감 직전의 평균 단가). StockMovement.unit_cost 에 기록됨.
     * @throws InsufficientStockException 가용 재고가 요청보다 적은 경우.
     */
    public BigDecimal issue(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0)
            throw new IllegalArgumentException("출고 수량은 0보다 커야 한다.");
        if (this.qtyOnHand.compareTo(qty) < 0) {
            throw new InsufficientStockException(
                    item != null ? item.getId() : null,
                    warehouse != null ? warehouse.getId() : null,
                    this.qtyOnHand, qty);
        }
        BigDecimal applied = this.averageCost;
        this.qtyOnHand = this.qtyOnHand.subtract(qty);
        return applied;
    }

    /**
     * 입고 취소 — 수량만 차감하고 평균은 건드리지 않는다.
     * (평균 역산은 사이 출고가 끼면 원리적으로 불가능 — 학습 범위 밖.)
     *
     * @return 취소에 적용된 단가 (= 현재 평균 단가). StockMovement 음수 행에 기록.
     * @throws InsufficientStockException 그 사이 출고로 빠져 이미 부족한 경우.
     */
    public BigDecimal cancelReceipt(BigDecimal qty) {
        // 의미상 issue 와 동일 — "수량만 빠지고 평균은 그대로" 가 같다.
        return issue(qty);
    }

    /**
     * 출고 취소 — 수량을 다시 더한다. 평균은 건드리지 않는다.
     */
    public void cancelIssue(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0)
            throw new IllegalArgumentException("취소 수량은 0보다 커야 한다.");
        this.qtyOnHand = this.qtyOnHand.add(qty);
    }
}

package com.hwlee.erp.mm.goodsissue;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.mm.warehouse.Warehouse;
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
 * 출고 헤더 — 창고에서 실제 물건이 빠져나가는 사건.
 *
 * <p>{@link #post} 시점에 라인별로 {@code stockRepo.findForUpdate()} (비관 락) →
 * 가용 검증 → {@code stock.issue()} → StockMovement(-) 적재가 한 트랜잭션에서 일어난다.
 * 가용 재고 < 요청이면 {@code InsufficientStockException}.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "goods_issue")
public class GoodsIssue extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30, updatable = false)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GoodsIssueStatus status = GoodsIssueStatus.DRAFT;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private GoodsIssueReason reason;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    /**
     * 이 출고가 비롯된 출하 ID (Phase 4). 출하 확정 시 자동 생성된 GI 만 채워지고,
     * 사용자가 직접 등록한 GI(실사 조정/폐기 등)는 {@code null} 이다.
     * DB 에는 {@code fk_goods_issue_delivery} FK 가 걸려 있지만, MM 이 SD 의 {@code Delivery}
     * 엔티티를 import 하지 않도록 Long 으로만 매핑한다(의존 방향 {@code MM → SD} 단방향).
     */
    @Column(name = "delivery_id")
    private Long deliveryId;

    @OneToMany(mappedBy = "goodsIssue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<GoodsIssueLine> lines = new ArrayList<>();

    public static GoodsIssue draft(String number, Warehouse warehouse, LocalDate issueDate, GoodsIssueReason reason) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("number 는 비어 있을 수 없다.");
        if (warehouse == null) throw new IllegalArgumentException("warehouse 는 null 일 수 없다.");
        if (issueDate == null) throw new IllegalArgumentException("issueDate 는 null 일 수 없다.");
        if (reason == null) throw new IllegalArgumentException("reason 은 null 일 수 없다.");
        GoodsIssue gi = new GoodsIssue();
        gi.number = number;
        gi.warehouse = warehouse;
        gi.issueDate = issueDate;
        gi.reason = reason;
        return gi;
    }

    /**
     * 출하 연계 전용 팩토리 (Phase 4) — {@link #draft} 와 동일하되 원천 {@code deliveryId} 를 채운다.
     * reason 은 항상 {@link GoodsIssueReason#SHIPMENT}.
     */
    public static GoodsIssue draftForDelivery(String number, Warehouse warehouse, LocalDate issueDate, Long deliveryId) {
        if (deliveryId == null) throw new IllegalArgumentException("deliveryId 는 null 일 수 없다.");
        GoodsIssue gi = draft(number, warehouse, issueDate, GoodsIssueReason.SHIPMENT);
        gi.deliveryId = deliveryId;
        return gi;
    }

    public GoodsIssueLine addLine(Item item, BigDecimal quantity) {
        ensureEditable();
        GoodsIssueLine line = new GoodsIssueLine(this, lines.size() + 1, item, quantity);
        lines.add(line);
        return line;
    }

    public void clearLines() {
        ensureEditable();
        lines.clear();
    }

    public void updateHeader(Warehouse warehouse, LocalDate issueDate, GoodsIssueReason reason) {
        ensureEditable();
        if (warehouse == null) throw new IllegalArgumentException("warehouse 는 null 일 수 없다.");
        if (issueDate == null) throw new IllegalArgumentException("issueDate 는 null 일 수 없다.");
        if (reason == null) throw new IllegalArgumentException("reason 은 null 일 수 없다.");
        this.warehouse = warehouse;
        this.issueDate = issueDate;
        this.reason = reason;
    }

    /**
     * DRAFT → POSTED. Stock 차감과 StockMovement 적재는 호출 측 서비스가 수행한다.
     */
    public void post(LocalDateTime now) {
        if (status != GoodsIssueStatus.DRAFT)
            throw new IllegalStateException("DRAFT 출고만 확정 가능합니다. 현재: " + status);
        if (lines.isEmpty())
            throw new IllegalStateException("라인이 비어 있는 출고는 확정할 수 없습니다.");
        this.status = GoodsIssueStatus.POSTED;
        this.postedAt = now;
    }

    public void cancel() {
        if (status != GoodsIssueStatus.POSTED)
            throw new IllegalStateException("POSTED 출고만 취소 가능합니다. 현재: " + status);
        this.status = GoodsIssueStatus.CANCELLED;
    }

    public List<GoodsIssueLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private void ensureEditable() {
        if (status != GoodsIssueStatus.DRAFT)
            throw new IllegalStateException("DRAFT 상태에서만 수정 가능합니다. 현재: " + status);
    }
}

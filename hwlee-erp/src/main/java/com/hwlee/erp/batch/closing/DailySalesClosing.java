package com.hwlee.erp.batch.closing;

import com.hwlee.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일일 매출 마감 스냅샷 — 기준일의 ISSUED 인보이스를 합산한 1행.
 *
 * <p>{@code closing_date} 에 UNIQUE 제약. 배치는 재실행 시 같은 날짜 행을 삭제 후 재삽입하므로
 * 몇 번을 다시 돌려도 결과가 동일하다(멱등).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "daily_sales_closing")
public class DailySalesClosing extends BaseEntity {

    @Column(name = "closing_date", nullable = false, unique = true)
    private LocalDate closingDate;

    @Column(name = "invoice_count", nullable = false)
    private int invoiceCount;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;

    public static DailySalesClosing of(LocalDate closingDate, int invoiceCount,
                                       BigDecimal subtotal, BigDecimal taxAmount,
                                       BigDecimal totalAmount, LocalDateTime closedAt) {
        DailySalesClosing c = new DailySalesClosing();
        c.closingDate = closingDate;
        c.invoiceCount = invoiceCount;
        c.subtotal = subtotal;
        c.taxAmount = taxAmount;
        c.totalAmount = totalAmount;
        c.closedAt = closedAt;
        return c;
    }
}

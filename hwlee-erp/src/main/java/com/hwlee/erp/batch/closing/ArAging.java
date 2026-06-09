package com.hwlee.erp.batch.closing;

import com.hwlee.erp.common.entity.BaseEntity;
import com.hwlee.erp.master.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채권 노령화 스냅샷 — 노령화 기준일의 고객별 미수금을 경과일 버킷으로 분류.
 *
 * <p>미수금 = {@code SUM(ISSUED 인보이스 합계) - SUM(RECEIPT 입금액)}. 인보이스일 기준 경과일로 버킷 배정.
 * 별도 Receivable 엔티티가 없어 인보이스/입금에서 파생 집계한다(학습용 간이 AR Aging).
 * (노령화일, 고객) UNIQUE — 재실행 시 기준일 단위로 삭제 후 재삽입.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ar_aging",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ar_aging_date_customer",
                columnNames = {"aging_date", "customer_id"})
)
public class ArAging extends BaseEntity {

    @Column(name = "aging_date", nullable = false)
    private LocalDate agingDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "bucket_0_30", nullable = false, precision = 15, scale = 2)
    private BigDecimal bucket0to30;

    @Column(name = "bucket_31_60", nullable = false, precision = 15, scale = 2)
    private BigDecimal bucket31to60;

    @Column(name = "bucket_61_90", nullable = false, precision = 15, scale = 2)
    private BigDecimal bucket61to90;

    @Column(name = "bucket_over_90", nullable = false, precision = 15, scale = 2)
    private BigDecimal bucketOver90;

    @Column(name = "total_outstanding", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalOutstanding;

    public static ArAging of(LocalDate agingDate, Customer customer,
                             BigDecimal bucket0to30, BigDecimal bucket31to60,
                             BigDecimal bucket61to90, BigDecimal bucketOver90,
                             BigDecimal totalOutstanding) {
        ArAging a = new ArAging();
        a.agingDate = agingDate;
        a.customer = customer;
        a.bucket0to30 = bucket0to30;
        a.bucket31to60 = bucket31to60;
        a.bucket61to90 = bucket61to90;
        a.bucketOver90 = bucketOver90;
        a.totalOutstanding = totalOutstanding;
        return a;
    }
}

package com.hwlee.erp.approval;

import com.hwlee.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전결 규정 — 문서 종류·금액 구간별로 "어디까지 결재가 올라가고, 합의가 필요한가".
 *
 * <p>상신 시 이 규칙을 조회해 결재 레벨({@link ApprovalLevel})과 합의 여부를 얻고,
 * 그에 맞춰 조직 트리를 타고 결재선을 자동 구성한다. 실무의 전결권한(결재 규정)에 해당.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "approval_rule")
public class ApprovalRule extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 24)
    private ApprovalDocType docType;

    @Column(name = "min_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    /** 상한(미만). null 이면 무한대(최고 구간). */
    @Column(name = "max_amount", precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false, length = 16)
    private ApprovalLevel approvalLevel;

    /** 이 구간에서 재무팀 합의가 필요한가(고액 통제). */
    @Column(name = "require_finance_agreement", nullable = false)
    private boolean requireFinanceAgreement;

    /**
     * 고정 결재 부서 코드(선택). 지정되면 상신자 조직 트리 상향 대신 이 부서의 부서장이 결재한다.
     * 예: 여신 상향은 상신자(영업)와 무관하게 재무팀장(DEPT-FINANCE)이 결재 → {@code DEPT-FINANCE}.
     */
    @Column(name = "fixed_approver_dept_code", length = 30)
    private String fixedApproverDeptCode;

    /**
     * 참조 부서 코드(선택). 지정되면 이 부서의 부서장이 참조(REFERENCE) 단계로 붙어 결재권 없이
     * 열람·통보만 받는다. 예: 본부장 전결 건에 대표(DEPT-HQ)를 참조로 두어 상위 인지시킴.
     */
    @Column(name = "reference_dept_code", length = 30)
    private String referenceDeptCode;

    /** amount 가 이 규칙의 금액 구간 [min, max) 에 드는가. */
    boolean covers(BigDecimal amount) {
        BigDecimal a = amount == null ? BigDecimal.ZERO : amount;
        if (a.compareTo(minAmount) < 0) return false;
        return maxAmount == null || a.compareTo(maxAmount) < 0;
    }
}

package com.hwlee.erp.approval;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRuleRepository extends JpaRepository<ApprovalRule, Long> {

    /** 문서 종류의 전결 규정 — 금액 구간 판정은 서비스에서 covers() 로 한다. */
    List<ApprovalRule> findByDocTypeOrderByMinAmountAsc(ApprovalDocType docType);
}

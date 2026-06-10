package com.hwlee.erp.fi.credit;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CreditLimitRequestRepository
        extends JpaRepository<CreditLimitRequest, Long>, JpaSpecificationExecutor<CreditLimitRequest> {

    /** 한 고객에 특정 상태(보통 PENDING) 요청이 있는지 — 중복 신청 방지. */
    boolean existsByCustomerIdAndStatus(Long customerId, CreditLimitRequestStatus status);

    /** 한 고객의 해당 상태 최신 요청 1건 — 화면이 "검토 중" 안내/링크에 쓴다. */
    Optional<CreditLimitRequest> findFirstByCustomerIdAndStatusOrderByIdDesc(
            Long customerId, CreditLimitRequestStatus status);
}

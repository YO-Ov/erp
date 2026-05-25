package com.hwlee.erp.sd.order.creditcheck;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.sd.order.SalesOrderRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 수주 확정 직전의 외부 의존 검증(고객 상태 + 신용한도).
 *
 * <p>도메인 메서드가 아닌 서비스 단에서 호출하는 이유:
 * <ul>
 *   <li>"이 고객의 다른 활성 수주 합계" 같은 외부 조회를 도메인 안에 둘 수 없음.</li>
 *   <li>실패 시 비즈니스 메시지로 응답하기 위해 {@link IllegalStateException} 으로 변환.</li>
 * </ul>
 *
 * <p>Phase 2 의도적 미구현: 동시 차감 race condition — Phase 3 동시성 학습에서 락 도입.
 */
@Component
@RequiredArgsConstructor
public class CreditLimitChecker {

    private final SalesOrderRepository repository;

    public void check(Customer customer, BigDecimal orderTotal, Long excludeOrderId) {
        if (customer.getStatus() != MasterStatus.ACTIVE) {
            throw new IllegalStateException("거래 불가 고객입니다 (status=" + customer.getStatus() + ").");
        }
        BigDecimal used = repository.sumActiveOrderAmountByCustomer(customer.getId(), excludeOrderId);
        if (used == null) used = BigDecimal.ZERO;
        BigDecimal remaining = customer.getCreditLimit().subtract(used);
        if (orderTotal.compareTo(remaining) > 0) {
            throw new IllegalStateException(
                    "신용한도 초과: 남은 한도 " + remaining + ", 요청 " + orderTotal);
        }
    }
}

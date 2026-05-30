package com.hwlee.erp.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.master.customer.PaymentTerms;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

/**
 * 감사 로그(층위 ②) — 엔티티 변경 시 audit_log 에 이력이 남고, 변경자(changed_by)가
 * 인증 사용자로 기록되는지(층위 ①·② 가 같은 "누가" 공유).
 *
 * <p>감사 기록은 트랜잭션 커밋 직후(afterCommit) 별도 트랜잭션에서 일어나므로,
 * 변경을 별도 트랜잭션으로 커밋시킨 뒤 audit_log 를 조회한다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@WithMockUser(username = "tester@hwlee-erp.example", roles = "ADMIN")
class AuditLogTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    @Test
    void customer_insert_and_update_are_audited_with_user() {
        // 1) 생성 — 별도 트랜잭션으로 커밋 → afterCommit 에서 INSERT 감사 기록
        Long id = txTemplate.execute(s -> {
            Customer c = Customer.create("CUST-AUDIT-1", "감사테스트상사", "999-88-77777",
                    "서울", new BigDecimal("1000000"), PaymentTerms.NET30);
            return customerRepository.save(c).getId();
        });

        // 2) 수정 — 신용한도 변경 → afterCommit 에서 UPDATE 감사 기록
        txTemplate.executeWithoutResult(s -> {
            Customer c = customerRepository.findById(id).orElseThrow();
            c.update("감사테스트상사", "부산", new BigDecimal("5000000"), PaymentTerms.NET60);
        });

        var logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByChangedAtDesc(
                "Customer", id, PageRequest.of(0, 10)).getContent();

        assertThat(logs).hasSizeGreaterThanOrEqualTo(2);
        assertThat(logs).anyMatch(l -> l.getAction() == AuditAction.INSERT);
        assertThat(logs).anyMatch(l -> l.getAction() == AuditAction.UPDATE);
        // 변경자가 인증 사용자로 기록됐는지 (system 이 아님)
        assertThat(logs).allMatch(l -> l.getChangedBy().equals("tester@hwlee-erp.example"));
        // 스냅샷에 변경된 신용한도가 담겼는지
        assertThat(logs).anyMatch(l -> l.getChanges() != null && l.getChanges().contains("5000000"));
    }
}

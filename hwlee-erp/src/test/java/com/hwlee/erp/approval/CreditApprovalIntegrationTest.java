package com.hwlee.erp.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.approval.dto.ApprovalActionRequest;
import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.fi.credit.CreditLimitRequestService;
import com.hwlee.erp.fi.credit.CreditLimitRequestStatus;
import com.hwlee.erp.fi.credit.dto.CreditLimitRequestCreateRequest;
import com.hwlee.erp.master.customer.CustomerService;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.customer.dto.CustomerCreateRequest;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 여신 상향의 전자결재 통합 검증 — 영업 요청이 결재 문서(CREDIT_LIMIT)로 상신되고,
 * 재무팀장 결재선이 자동 구성되며, 승인/반려 결과가 여신 상태와 고객 한도에 반영된다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CreditApprovalIntegrationTest {

    private static final String SALES = "kim@hyunwoo.com";

    @Autowired CreditLimitRequestService creditService;
    @Autowired ApprovalService approvalService;
    @Autowired CustomerService customerService;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("여신 요청 → 재무팀장 결재선 자동 구성 → 승인 시 고객 한도 반영")
    void 여신_결재_승인() {
        authenticate(SALES);
        Long custId = newCustomer();

        var cr = creditService.create(new CreditLimitRequestCreateRequest(
                custId, new BigDecimal("50000000"), "거래 6개월 무연체"));
        assertThat(cr.status()).isEqualTo(CreditLimitRequestStatus.PENDING);

        // 결재 문서가 CREDIT_LIMIT 로 상신되고 결재자는 재무팀장 1명(상신자 조직 무관).
        ApprovalResponse a = approvalService.findLatestForDoc(ApprovalDocType.CREDIT_LIMIT, cr.id(), SALES);
        assertThat(a).isNotNull();
        assertThat(a.steps()).hasSize(1);
        assertThat(a.steps().get(0).approver()).isEqualTo("finance.mgr@hyunwoo.com");

        approvalService.approve(a.id(), null, a.steps().get(0).approver());

        assertThat(creditService.findById(cr.id()).status()).isEqualTo(CreditLimitRequestStatus.APPROVED);
        assertThat(customerService.findById(custId).creditLimit()).isEqualByComparingTo("50000000");
    }

    @Test
    @DisplayName("결재 반려 시 여신 요청은 REJECTED, 고객 한도는 그대로")
    void 여신_결재_반려() {
        authenticate(SALES);
        Long custId = newCustomer();

        var cr = creditService.create(new CreditLimitRequestCreateRequest(
                custId, new BigDecimal("20000000"), "한도 상향 요청"));
        ApprovalResponse a = approvalService.findLatestForDoc(ApprovalDocType.CREDIT_LIMIT, cr.id(), SALES);

        approvalService.reject(a.id(), new ApprovalActionRequest("한도 근거 부족"), a.steps().get(0).approver());

        assertThat(creditService.findById(cr.id()).status()).isEqualTo(CreditLimitRequestStatus.REJECTED);
        assertThat(customerService.findById(custId).creditLimit()).isEqualByComparingTo("0"); // 미반영
    }

    private Long newCustomer() {
        return customerService.create(new CustomerCreateRequest(
                "고객-" + System.nanoTime(), uniqueBusinessNo(), "주소", PaymentTerms.NET30)).id();
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "x",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_SALES"))));
    }

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime());

    private static String uniqueBusinessNo() {
        long n = SEQ.incrementAndGet();
        return String.format("%03d-%02d-%05d",
                (int) ((n / 10_000_000L) % 900) + 100,
                (int) ((n / 100_000L) % 100),
                (int) (n % 100_000L));
    }
}

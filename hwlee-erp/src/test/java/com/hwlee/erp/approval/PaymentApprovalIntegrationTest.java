package com.hwlee.erp.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.fi.payment.PaymentService;
import com.hwlee.erp.fi.payment.PaymentStatus;
import com.hwlee.erp.fi.payment.PaymentType;
import com.hwlee.erp.fi.payment.dto.PaymentCreateRequest;
import com.hwlee.erp.master.vendor.Vendor;
import com.hwlee.erp.master.vendor.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * 지급(출금)의 전자결재 통합 검증 — 초안(DRAFT) 지급을 상신하고, 결재 최종 승인 시
 * 자동으로 전기(POSTED)+출금 분개가 실행된다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PaymentApprovalIntegrationTest {

    private static final String FINANCE = "lee@hyunwoo.com";

    @Autowired PaymentService paymentService;
    @Autowired ApprovalService approvalService;
    @Autowired VendorRepository vendorRepository;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("지급 초안 → 결재 상신 → 승인 시 전기(POSTED)")
    void 지급_결재_승인() {
        authenticate(FINANCE);
        Vendor vendor = vendorRepository.findAll().stream().findFirst().orElseThrow();

        var draft = paymentService.createDraft(new PaymentCreateRequest(
                PaymentType.DISBURSEMENT, null, vendor.getId(),
                new BigDecimal("3000000"), LocalDate.now(), "부품 대금 지급"));
        assertThat(draft.status()).isEqualTo(PaymentStatus.DRAFT);

        ApprovalResponse a = paymentService.submitForApproval(draft.id(), FINANCE);
        assertThat(a.docType()).isEqualTo(ApprovalDocType.PAYMENT);

        ApprovalResponse done = approveAll(a.id());
        assertThat(done.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(paymentService.findById(draft.id()).status()).isEqualTo(PaymentStatus.POSTED);
    }

    private ApprovalResponse approveAll(Long approvalId) {
        ApprovalResponse a = approvalService.findById(approvalId, FINANCE);
        int guard = 0;
        while (a.status() == ApprovalStatus.PENDING && guard++ < 20) {
            final int cur = a.currentStep();
            var next = a.steps().stream()
                    .filter(s -> s.status() == ApprovalStepStatus.PENDING
                            && (s.type() == ApprovalStepType.AGREEMENT
                            || (s.type() == ApprovalStepType.APPROVAL && s.stepNo() == cur)))
                    .findFirst().orElseThrow();
            a = approvalService.approve(approvalId, null, next.approver());
        }
        return a;
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "x",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_FINANCE"))));
    }
}

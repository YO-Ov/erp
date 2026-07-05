package com.hwlee.erp.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.approval.dto.ApprovalStepResponse;
import com.hwlee.erp.master.customer.CustomerService;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.customer.dto.CustomerCreateRequest;
import com.hwlee.erp.master.item.ItemService;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.master.item.dto.ItemCreateRequest;
import com.hwlee.erp.sd.quotation.QuotationService;
import com.hwlee.erp.sd.quotation.QuotationStatus;
import com.hwlee.erp.sd.quotation.dto.QuotationCreateRequest;
import com.hwlee.erp.sd.quotation.dto.QuotationLineRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
 * 전자결재 조직 연동 + 견적 e2e 통합 검증 (Testcontainers, V64/V65 적용).
 *
 * <p>상신자 = {@code sales.global@hyunwoo.com}(해외영업팀, 팀장 미지정) → 결재선은 상위
 * 영업본부장부터 자동 구성된다("부서장 없으면 상위로" 규칙). 금액 구간별 전결 차등과
 * 최종 승인 시 견적 자동 발송(SENT)을 확인한다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApprovalScenarioTest {

    private static final String REQUESTER = "sales.global@hyunwoo.com";

    @Autowired QuotationService quotationService;
    @Autowired ApprovalService approvalService;
    @Autowired CustomerService customerService;
    @Autowired ItemService itemService;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("소액 견적: 팀장 전결(결재선 1단계) → 승인하면 견적이 자동 발송(SENT)된다")
    void 소액_견적_e2e() {
        authenticate(REQUESTER);
        Long qid = createQuotation(new BigDecimal("1000000"), new BigDecimal("5")); // 500만 < 1천만 = TEAM

        ApprovalResponse a = quotationService.submitForApproval(qid, REQUESTER);
        assertThat(a.status()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(countType(a, ApprovalStepType.APPROVAL)).isEqualTo(1);   // 팀장 전결
        assertThat(countType(a, ApprovalStepType.AGREEMENT)).isZero();

        ApprovalResponse done = approveAll(a.id());
        assertThat(done.status()).isEqualTo(ApprovalStatus.APPROVED);
        // 최종 승인 → 이벤트 콜백으로 견적 발송
        assertThat(quotationService.findById(qid).status()).isEqualTo(QuotationStatus.SENT);
    }

    @Test
    @DisplayName("전결 차등: 금액이 클수록 결재선이 길어지고, 고액은 재무 합의가 붙는다")
    void 전결_차등() {
        authenticate(REQUESTER);
        // 중액 2천만(1천만~5천만) = DIVISION: 본부장+대표
        ApprovalResponse mid = quotationService.submitForApproval(
                createQuotation(new BigDecimal("2000000"), new BigDecimal("10")), REQUESTER);
        assertThat(countType(mid, ApprovalStepType.APPROVAL)).isEqualTo(2);
        assertThat(countType(mid, ApprovalStepType.AGREEMENT)).isZero();

        // 고액 6천만(5천만+) = COMPANY + 재무 합의
        ApprovalResponse high = quotationService.submitForApproval(
                createQuotation(new BigDecimal("10000000"), new BigDecimal("6")), REQUESTER);
        assertThat(countType(high, ApprovalStepType.APPROVAL)).isGreaterThanOrEqualTo(2);
        assertThat(countType(high, ApprovalStepType.AGREEMENT)).isEqualTo(1);   // 재무 합의
    }

    @Test
    @DisplayName("중복 상신 방지: 진행 중 결재가 있으면 다시 상신할 수 없다")
    void 중복_상신_방지() {
        authenticate(REQUESTER);
        Long qid = createQuotation(new BigDecimal("1000000"), new BigDecimal("5"));
        quotationService.submitForApproval(qid, REQUESTER);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> quotationService.submitForApproval(qid, REQUESTER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 진행 중");
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────

    /** 결재선의 모든 처리 대상 단계를 순서대로 승인한다. */
    private ApprovalResponse approveAll(Long approvalId) {
        ApprovalResponse a = approvalService.findById(approvalId, REQUESTER);
        int guard = 0;
        while (a.status() == ApprovalStatus.PENDING && guard++ < 20) {
            final int cur = a.currentStep();
            ApprovalStepResponse next = a.steps().stream()
                    .filter(s -> s.status() == ApprovalStepStatus.PENDING
                            && (s.type() == ApprovalStepType.AGREEMENT
                            || (s.type() == ApprovalStepType.APPROVAL && s.stepNo() == cur)))
                    .findFirst().orElseThrow();
            a = approvalService.approve(approvalId, null, next.approver());
        }
        return a;
    }

    private long countType(ApprovalResponse a, ApprovalStepType type) {
        return a.steps().stream().filter(s -> s.type() == type).count();
    }

    private Long createQuotation(BigDecimal unitPrice, BigDecimal qty) {
        Long customerId = customerService.create(new CustomerCreateRequest(
                "고객-" + System.nanoTime(), uniqueBusinessNo(), "주소", PaymentTerms.NET30)).id();
        Long itemId = itemService.create(new ItemCreateRequest(
                "상품-" + System.nanoTime(), "NOTEBOOK", ItemUnit.EA, unitPrice, unitPrice)).id();
        return quotationService.create(new QuotationCreateRequest(
                customerId, LocalDate.now(), null,
                List.of(new QuotationLineRequest(itemId, qty, unitPrice)))).id();
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "x",
                        List.of(new SimpleGrantedAuthority("ROLE_SALES"))));
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

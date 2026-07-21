package com.hwlee.erp.sd.quotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.customer.CustomerService;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.customer.dto.CustomerCreateRequest;
import com.hwlee.erp.master.item.ItemService;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.master.item.dto.ItemCreateRequest;
import com.hwlee.erp.sd.quotation.dto.QuotationCreateRequest;
import com.hwlee.erp.sd.quotation.dto.QuotationLineRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 기간별 견적 집계 — "이번 달 견적 합계" 류 질의가 정확한 값을 받는지.
 * 기준일은 발행일(issuedDate).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuotationSummaryIntegrationTest {

    @Autowired QuotationService quotationService;
    @Autowired CustomerService customerService;
    @Autowired ItemService itemService;

    @Test
    @DisplayName("기간별 집계는 발행일이 기간 밖인 견적을 빼고 건수·금액을 합산한다")
    void 기간별_집계는_기간_내_견적만_합산한다() {
        // 다른 테스트(LocalDate.now() 사용)와 섞이지 않도록 과거의 한 달을 쓴다.
        LocalDate 기간내 = LocalDate.of(2018, 5, 9);
        LocalDate 기간밖 = LocalDate.of(2018, 6, 1);

        createQuotation(기간내, new BigDecimal("2"));   // 20만
        createQuotation(기간내, new BigDecimal("3"));   // 30만
        createQuotation(기간밖, new BigDecimal("7"));   // 70만 — 섞이면 안 됨

        var summary = quotationService.summary(
                LocalDate.of(2018, 5, 1), LocalDate.of(2018, 5, 31));

        assertThat(summary.quotationCount()).isEqualTo(2);
        assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    @DisplayName("견적이 없는 기간도 금액은 null 이 아니라 0")
    void 견적이_없는_기간은_0으로_집계된다() {
        // null 이 새어 나가면 이 값을 그대로 포맷하는 쪽(에이전트)이 터진다.
        var summary = quotationService.summary(
                LocalDate.of(2009, 1, 1), LocalDate.of(2009, 1, 31));

        assertThat(summary.quotationCount()).isZero();
        assertThat(summary.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private void createQuotation(LocalDate issuedDate, BigDecimal qty) {
        BigDecimal unitPrice = new BigDecimal("100000");
        Long customerId = customerService.create(new CustomerCreateRequest(
                "고객-" + System.nanoTime(), uniqueBusinessNo(), "주소", PaymentTerms.NET30)).id();
        Long itemId = itemService.create(new ItemCreateRequest(
                "상품-" + System.nanoTime(), "NOTEBOOK", ItemUnit.EA, unitPrice, unitPrice)).id();
        quotationService.create(new QuotationCreateRequest(
                customerId, issuedDate, null,
                List.of(new QuotationLineRequest(itemId, qty, unitPrice))));
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

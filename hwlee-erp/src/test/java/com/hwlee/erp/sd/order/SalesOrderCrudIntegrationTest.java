package com.hwlee.erp.sd.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.customer.dto.CustomerCreateRequest;
import com.hwlee.erp.master.customer.CustomerService;
import com.hwlee.erp.master.item.ItemCategory;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.master.item.ItemService;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.master.item.dto.ItemCreateRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderCreateRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderLineRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 수주 핵심 업무 규칙 통합 검증 — 번호 발급, 신용한도, 활성 고객 정책.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SalesOrderCrudIntegrationTest {

    @Autowired SalesOrderService salesOrderService;
    @Autowired CustomerService customerService;
    @Autowired ItemService itemService;
    @Autowired CustomerRepository customerRepository;
    @Autowired ItemRepository itemRepository;

    @Test
    @DisplayName("수주를 생성하면 SO-YYYYMMDD-NNN 형식의 번호가 자동 발급된다")
    void 수주를_생성하면_번호가_자동_발급된다() {
        var customerId = createCustomer(new BigDecimal("100000000")).id();
        var itemId = createItem(new BigDecimal("100000")).id();

        SalesOrderResponse created = salesOrderService.create(new SalesOrderCreateRequest(
                customerId, null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(itemId, new BigDecimal("3"), new BigDecimal("100000")))
        ));

        assertThat(created.number()).matches("SO-\\d{8}-\\d{3}");
        assertThat(created.status()).isEqualTo(SalesOrderStatus.DRAFT);
        assertThat(created.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
    }

    @Test
    @DisplayName("신용한도 부족이면 confirm 이 거부된다")
    void 신용한도_부족이면_confirm_거부된다() {
        var customerId = createCustomer(new BigDecimal("100000")).id();   // 한도 10만
        var itemId = createItem(new BigDecimal("50000")).id();

        SalesOrderResponse over = salesOrderService.create(new SalesOrderCreateRequest(
                customerId, null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(itemId, new BigDecimal("3"), new BigDecimal("50000")))
        ));   // 15만 → 한도 초과

        assertThatThrownBy(() -> salesOrderService.confirm(over.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("신용한도 초과");
    }

    @Test
    @DisplayName("DRAFT 수주는 한도 영향 없음 — 같은 고객으로 한도 한도 한 번 더 가능")
    void DRAFT_수주는_한도_영향_없음() {
        var customerId = createCustomer(new BigDecimal("200000")).id();
        var itemId = createItem(new BigDecimal("100000")).id();

        // 한 건은 확정 (10만 차감)
        var so1 = salesOrderService.create(new SalesOrderCreateRequest(
                customerId, null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(itemId, new BigDecimal("1"), new BigDecimal("100000")))
        ));
        salesOrderService.confirm(so1.id());

        // 또 한 건은 DRAFT 로만 두면 한도에 영향 없음
        var so2 = salesOrderService.create(new SalesOrderCreateRequest(
                customerId, null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(itemId, new BigDecimal("1"), new BigDecimal("100000")))
        ));
        // 남은 한도 10만 = 신규 10만, OK
        salesOrderService.confirm(so2.id());
        assertThat(salesOrderService.findById(so2.id()).status()).isEqualTo(SalesOrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("DRAFT 수주만 수정 가능 — 확정 후 수정은 거부")
    void 확정된_수주는_수정_불가() {
        var customerId = createCustomer(new BigDecimal("100000000")).id();
        var itemId = createItem(new BigDecimal("100000")).id();

        var so = salesOrderService.create(new SalesOrderCreateRequest(
                customerId, null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(itemId, new BigDecimal("1"), new BigDecimal("100000")))
        ));
        salesOrderService.confirm(so.id());

        assertThatThrownBy(() -> salesOrderService.update(so.id(),
                new com.hwlee.erp.sd.order.dto.SalesOrderUpdateRequest(
                        null, LocalDate.now(),
                        List.of(new SalesOrderLineRequest(itemId, new BigDecimal("2"), new BigDecimal("100000")))
                )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    private com.hwlee.erp.master.customer.dto.CustomerResponse createCustomer(BigDecimal creditLimit) {
        return customerService.create(new CustomerCreateRequest(
                "고객-" + System.nanoTime(),
                uniqueBusinessNo(),
                "주소",
                creditLimit,
                PaymentTerms.NET30
        ));
    }

    private com.hwlee.erp.master.item.dto.ItemResponse createItem(BigDecimal price) {
        return itemService.create(new ItemCreateRequest(
                "상품-" + System.nanoTime(),
                ItemCategory.NOTEBOOK,
                ItemUnit.EA,
                price,
                price
        ));
    }

    private static final java.util.concurrent.atomic.AtomicLong SEQ =
            new java.util.concurrent.atomic.AtomicLong(System.nanoTime());

    private static String uniqueBusinessNo() {
        long n = SEQ.incrementAndGet();
        return String.format("%03d-%02d-%05d",
                (int) ((n / 10_000_000L) % 900) + 100,
                (int) ((n / 100_000L) % 100),
                (int) (n % 100_000L));
    }
}

package com.hwlee.erp.sd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.master.customer.CustomerService;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.customer.dto.CustomerCreateRequest;
import com.hwlee.erp.master.customer.dto.CustomerResponse;
import com.hwlee.erp.master.item.ItemCategory;
import com.hwlee.erp.master.item.ItemService;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.master.item.dto.ItemCreateRequest;
import com.hwlee.erp.master.vendor.VendorService;
import com.hwlee.erp.master.vendor.dto.VendorCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.GoodsReceiptService;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptLineRequest;
import com.hwlee.erp.mm.warehouse.WarehouseService;
import com.hwlee.erp.mm.warehouse.dto.WarehouseCreateRequest;
import com.hwlee.erp.sd.delivery.DeliveryService;
import com.hwlee.erp.sd.delivery.dto.DeliveryCreateRequest;
import com.hwlee.erp.sd.delivery.dto.DeliveryLineRequest;
import com.hwlee.erp.sd.delivery.dto.DeliveryResponse;
import com.hwlee.erp.sd.invoice.InvoiceService;
import com.hwlee.erp.sd.invoice.dto.InvoiceCreateRequest;
import com.hwlee.erp.sd.invoice.dto.InvoiceLineRequest;
import com.hwlee.erp.sd.invoice.dto.InvoiceResponse;
import com.hwlee.erp.sd.order.SalesOrderService;
import com.hwlee.erp.sd.order.SalesOrderStatus;
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
 * Phase 2 의 하이라이트 — 부분 출하/청구 시나리오.
 *
 * <p>10대 수주 → 6대 출하 → 6대 인보이스 → 4대 출하 → 4대 인보이스.
 * 매 단계마다 SO 라인의 shipped_qty / invoiced_qty 누적과 헤더 상태 전이를 검증한다.
 *
 * <p><b>Phase 4 변경</b>: 출하 확정 시 MM 재고가 자동 차감되므로, 출하 전에 충분한 재고를
 * 입고해 둬야 한다(재고 없으면 출하 자체가 롤백). 출하/청구 누적 검증의 본질은 그대로.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PartialDeliveryAndInvoiceScenarioTest {

    @Autowired CustomerService customerService;
    @Autowired CustomerRepository customerRepository;
    @Autowired ItemService itemService;
    @Autowired VendorService vendorService;
    @Autowired WarehouseService warehouseService;
    @Autowired GoodsReceiptService goodsReceiptService;
    @Autowired SalesOrderService salesOrderService;
    @Autowired DeliveryService deliveryService;
    @Autowired InvoiceService invoiceService;

    @Test
    @DisplayName("10대 수주 → 6대 출하 → 6대 청구 → 4대 출하 → 4대 청구 전체 흐름")
    void 부분_출하와_청구가_각_단계마다_누적되며_상태가_전이된다() {
        // given — 한도 충분한 고객 + 노트북
        var customer = createCustomerWithCreditLimit(
                "현우테크-" + System.nanoTime(), "서울시", new BigDecimal("100000000"));
        var item = itemService.create(new ItemCreateRequest(
                "노트북-" + System.nanoTime(),
                ItemCategory.NOTEBOOK, ItemUnit.EA,
                new BigDecimal("800000"),
                new BigDecimal("1200000")));

        // Phase 4: 출하될 창고에 10대 입고 (출하 시 재고 차감)
        Long warehouseId = warehouseId();
        stockUp(item.id(), warehouseId, 10);

        // step 1: 10대 수주 → 확정
        SalesOrderResponse order = salesOrderService.create(new SalesOrderCreateRequest(
                customer.id(), null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(item.id(), new BigDecimal("10"), new BigDecimal("1200000")))
        ));
        salesOrderService.confirm(order.id());

        Long soId = order.id();
        Long solId = salesOrderService.findById(soId).lines().get(0).id();

        // step 2: 6대 출하 → SHIPPING
        DeliveryResponse dlv1 = deliveryService.create(new DeliveryCreateRequest(
                soId, warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(solId, new BigDecimal("6")))));
        assertThat(dlv1.number()).matches("DLV-\\d{8}-\\d{3}");

        var afterShip1 = salesOrderService.findById(soId);
        assertThat(afterShip1.status()).isEqualTo(SalesOrderStatus.SHIPPING);
        assertThat(afterShip1.lines().get(0).shippedQty()).isEqualByComparingTo(new BigDecimal("6"));
        assertThat(afterShip1.lines().get(0).invoicedQty()).isEqualByComparingTo(BigDecimal.ZERO);

        // step 3: 6대 인보이스 → INVOICING
        InvoiceResponse inv1 = invoiceService.create(new InvoiceCreateRequest(
                soId, LocalDate.now(),
                List.of(new InvoiceLineRequest(solId, new BigDecimal("6")))));
        // 6 × 1_200_000 = 7_200_000, 세금 720_000, 합 7_920_000
        assertThat(inv1.subtotal()).isEqualByComparingTo(new BigDecimal("7200000"));
        assertThat(inv1.taxAmount()).isEqualByComparingTo(new BigDecimal("720000"));
        assertThat(inv1.totalAmount()).isEqualByComparingTo(new BigDecimal("7920000"));

        var afterInv1 = salesOrderService.findById(soId);
        assertThat(afterInv1.status()).isEqualTo(SalesOrderStatus.INVOICING);
        assertThat(afterInv1.lines().get(0).invoicedQty()).isEqualByComparingTo(new BigDecimal("6"));

        // step 4: 보내지 않은 수량을 청구하면 거부됨 — "보낸 만큼만 청구"
        assertThatThrownBy(() -> invoiceService.create(new InvoiceCreateRequest(
                soId, LocalDate.now(),
                List.of(new InvoiceLineRequest(solId, new BigDecimal("1"))))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔여 청구 가능량");

        // step 5: 4대 출하 → SHIPPED 가 되지만 헤더는 이미 INVOICING 이므로 그대로 INVOICING 유지
        // (실무 모델: 청구가 일부 끝난 상태에서 추가 출하는 INVOICING 상태 유지)
        deliveryService.create(new DeliveryCreateRequest(
                soId, warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(solId, new BigDecimal("4")))));

        var afterShip2 = salesOrderService.findById(soId);
        assertThat(afterShip2.lines().get(0).shippedQty()).isEqualByComparingTo(new BigDecimal("10"));

        // step 6: 잔여 4대 청구 → INVOICED (전량 청구 완료)
        invoiceService.create(new InvoiceCreateRequest(
                soId, LocalDate.now(),
                List.of(new InvoiceLineRequest(solId, new BigDecimal("4")))));

        var finalState = salesOrderService.findById(soId);
        assertThat(finalState.status()).isEqualTo(SalesOrderStatus.INVOICED);
        assertThat(finalState.lines().get(0).shippedQty()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(finalState.lines().get(0).invoicedQty()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    @DisplayName("주문량 10대를 초과해 11대를 출하하려고 하면 거부된다")
    void 주문량을_초과한_출하는_거부된다() {
        var customer = createCustomerWithCreditLimit(
                "한도큰-" + System.nanoTime(), null, new BigDecimal("100000000"));
        var item = itemService.create(new ItemCreateRequest(
                "노트북-" + System.nanoTime(), ItemCategory.NOTEBOOK, ItemUnit.EA,
                new BigDecimal("100000"), new BigDecimal("200000")));

        var order = salesOrderService.create(new SalesOrderCreateRequest(
                customer.id(), null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(item.id(), new BigDecimal("10"), new BigDecimal("200000")))));
        salesOrderService.confirm(order.id());
        Long solId = salesOrderService.findById(order.id()).lines().get(0).id();

        // 11대 출하는 잔여 출하 가능량(10) 초과로 DeliveryLine 생성 단계에서 거부 — 재고/이벤트 도달 전.
        assertThatThrownBy(() -> deliveryService.create(new DeliveryCreateRequest(
                order.id(), warehouseId(), LocalDate.now(),
                List.of(new DeliveryLineRequest(solId, new BigDecimal("11"))))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔여 출하 가능량");
    }

    @Test
    @DisplayName("출하 취소시 SO 라인 shipped_qty 가 복원되고 헤더 상태가 CONFIRMED 로 되돌아간다")
    void 출하_취소시_누적이_복원된다() {
        var customer = createCustomerWithCreditLimit(
                "취소대상-" + System.nanoTime(), null, new BigDecimal("100000000"));
        var item = itemService.create(new ItemCreateRequest(
                "노트북-" + System.nanoTime(), ItemCategory.NOTEBOOK, ItemUnit.EA,
                new BigDecimal("100000"), new BigDecimal("200000")));

        Long warehouseId = warehouseId();
        stockUp(item.id(), warehouseId, 5);

        var order = salesOrderService.create(new SalesOrderCreateRequest(
                customer.id(), null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(item.id(), new BigDecimal("5"), new BigDecimal("200000")))));
        salesOrderService.confirm(order.id());
        Long solId = salesOrderService.findById(order.id()).lines().get(0).id();

        var dlv = deliveryService.create(new DeliveryCreateRequest(
                order.id(), warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(solId, new BigDecimal("5")))));
        assertThat(salesOrderService.findById(order.id()).status()).isEqualTo(SalesOrderStatus.SHIPPED);

        deliveryService.cancel(dlv.id());
        var after = salesOrderService.findById(order.id());
        assertThat(after.status()).isEqualTo(SalesOrderStatus.CONFIRMED);
        assertThat(after.lines().get(0).shippedQty()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // === helpers ===

    /** 고객을 생성한 뒤 신용한도를 충분히 올려준다(생성 시 한도는 항상 0이므로). */
    private CustomerResponse createCustomerWithCreditLimit(String name, String address, BigDecimal creditLimit) {
        var customer = customerService.create(new CustomerCreateRequest(
                name, uniqueBusinessNo(), address, PaymentTerms.NET30));
        customerRepository.findById(customer.id()).orElseThrow().changeCreditLimit(creditLimit);
        customerRepository.flush();
        return customer;
    }

    /** 출하될 새 창고 하나 생성, id 반환. */
    private Long warehouseId() {
        return warehouseService.create(new WarehouseCreateRequest(
                "WH-" + whSuffix(), "테스트창고", "서울시")).id();
    }

    /** 주어진 품목을 창고에 qty 만큼 입고하고 확정 — 출하 시 차감될 재고 확보. */
    private void stockUp(Long itemId, Long warehouseId, int qty) {
        var vendor = vendorService.create(new VendorCreateRequest(
                "거래처-" + System.nanoTime(), uniqueBusinessNo(), "인천시", PaymentTerms.NET30));
        var gr = goodsReceiptService.create(new GoodsReceiptCreateRequest(
                vendor.id(), warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(itemId, new BigDecimal(qty), new BigDecimal("1000")))));
        goodsReceiptService.post(gr.id());
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

    private static String whSuffix() {
        long n = SEQ.incrementAndGet();
        return "T" + Long.toString(n, 36).toUpperCase().replace("-", "");
    }
}

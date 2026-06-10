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
import com.hwlee.erp.mm.goodsissue.GoodsIssueRepository;
import com.hwlee.erp.mm.goodsissue.GoodsIssueService;
import com.hwlee.erp.mm.goodsissue.GoodsIssueStatus;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueResponse;
import com.hwlee.erp.mm.goodsreceipt.GoodsReceiptService;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptLineRequest;
import com.hwlee.erp.mm.stock.InsufficientStockException;
import com.hwlee.erp.mm.stock.MovementReason;
import com.hwlee.erp.mm.stock.Stock;
import com.hwlee.erp.mm.stock.StockMovement;
import com.hwlee.erp.mm.stock.StockMovementRepository;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.mm.warehouse.WarehouseService;
import com.hwlee.erp.mm.warehouse.dto.WarehouseCreateRequest;
import com.hwlee.erp.sd.delivery.DeliveryService;
import com.hwlee.erp.sd.delivery.dto.DeliveryCreateRequest;
import com.hwlee.erp.sd.delivery.dto.DeliveryLineRequest;
import com.hwlee.erp.sd.delivery.dto.DeliveryResponse;
import com.hwlee.erp.sd.order.SalesOrderService;
import com.hwlee.erp.sd.order.dto.SalesOrderCreateRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderLineRequest;
import jakarta.persistence.EntityNotFoundException;
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
 * Phase 4 의 하이라이트 — SD(출하) ↔ MM(재고) 이벤트 연계.
 *
 * <p>{@code DeliveryService.create} 가 출하를 확정하면 {@code DeliveryShippedEvent} 가 발행되고,
 * {@code mm/integration/sd/DeliveryEventListener} 가 {@code @TransactionalEventListener(BEFORE_COMMIT)}
 * 로 같은 트랜잭션 안에서 GoodsIssue 를 자동 생성·확정하며 재고를 차감한다.
 *
 * <p>핵심 검증: <b>한 비즈니스 사건(출하) = 한 트랜잭션</b> — 재고 차감이 실패하면 출하·SO 누적까지
 * 전부 롤백되고, 성공하면 Delivery↔GoodsIssue 가 강한 FK 로 연결된다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SdMmIntegrationTest {

    @Autowired CustomerService customerService;
    @Autowired CustomerRepository customerRepository;
    @Autowired ItemService itemService;
    @Autowired VendorService vendorService;
    @Autowired WarehouseService warehouseService;
    @Autowired GoodsReceiptService goodsReceiptService;
    @Autowired SalesOrderService salesOrderService;
    @Autowired DeliveryService deliveryService;
    @Autowired GoodsIssueService goodsIssueService;
    @Autowired GoodsIssueRepository goodsIssueRepository;
    @Autowired StockRepository stockRepository;
    @Autowired StockMovementRepository stockMovementRepository;

    @Test
    @DisplayName("출하 확정 시 재고가 자동 차감되고 GoodsIssue 가 POSTED 로 생성된다")
    void 출하_확정_시_재고가_자동_차감되고_GoodsIssue_가_POSTED_로_생성된다() {
        var ctx = setup(10);   // 10대 입고

        // 6대 출하 확정 — 이벤트 발행 → 리스너가 GI 생성 + 재고 차감
        DeliveryResponse dlv = deliveryService.create(new DeliveryCreateRequest(
                ctx.soId, ctx.warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(ctx.solId, bd(6)))));

        // 재고는 4대 남는다
        Stock stock = stockRepository
                .findByItemIdAndWarehouseId(ctx.itemId, ctx.warehouseId).orElseThrow();
        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(bd(4));

        // 출하에 연결된 GoodsIssue 가 자동 생성되고 POSTED
        Long giId = goodsIssueRepository.findByDeliveryId(dlv.id()).orElseThrow().getId();
        GoodsIssueResponse gi = goodsIssueService.findById(giId);
        assertThat(gi.status()).isEqualTo(GoodsIssueStatus.POSTED);
        assertThat(gi.deliveryId()).isEqualTo(dlv.id());
        assertThat(gi.lines()).hasSize(1);
        assertThat(gi.lines().get(0).quantity()).isEqualByComparingTo(bd(6));

        // 원장에 출고(-6) 한 줄
        StockMovement giMv = stockMovementRepository.findAll().stream()
                .filter(m -> m.getReason() == MovementReason.GOODS_ISSUE && m.getRefId().equals(giId))
                .findFirst().orElseThrow();
        assertThat(giMv.getQtyDelta()).isEqualByComparingTo(bd(-6));
    }

    @Test
    @DisplayName("가용 재고가 부족하면 InsufficientStockException 과 함께 출하 전체가 롤백된다")
    void 가용_재고_부족_출하는_전체_롤백된다() {
        var ctx = setup(5);   // 5대만 입고

        // 10대 출하 시도 (SO 잔여는 10이라 라인 생성은 통과, 재고 차감에서 실패)
        assertThatThrownBy(() -> deliveryService.create(new DeliveryCreateRequest(
                ctx.soId, ctx.warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(ctx.solId, bd(10))))))
                .isInstanceOf(InsufficientStockException.class);

        // 전체 롤백 — 재고는 5대 그대로
        Stock stock = stockRepository
                .findByItemIdAndWarehouseId(ctx.itemId, ctx.warehouseId).orElseThrow();
        assertThat(stock.getQtyOnHand())
                .as("리스너 예외가 BEFORE_COMMIT 이라 재고 차감도 롤백")
                .isEqualByComparingTo(bd(5));

        // SO 라인의 shipped_qty 도 누적되지 않음 (출하 트랜잭션 통째로 롤백)
        var so = salesOrderService.findById(ctx.soId);
        assertThat(so.lines().get(0).shippedQty())
                .as("출하가 롤백됐으므로 SO 출하 누적도 0")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("출하 취소 시 연결된 GoodsIssue 도 CANCELLED 되고 재고가 복원된다")
    void 출하_취소_시_GoodsIssue_도_취소되고_재고가_복원된다() {
        var ctx = setup(10);

        DeliveryResponse dlv = deliveryService.create(new DeliveryCreateRequest(
                ctx.soId, ctx.warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(ctx.solId, bd(6)))));
        Long giId = goodsIssueRepository.findByDeliveryId(dlv.id()).orElseThrow().getId();

        // 출하 취소 → 이벤트 → 리스너가 GI 취소 + 재고 복원
        deliveryService.cancel(dlv.id());

        Stock stock = stockRepository
                .findByItemIdAndWarehouseId(ctx.itemId, ctx.warehouseId).orElseThrow();
        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(bd(10));

        assertThat(goodsIssueService.findById(giId).status()).isEqualTo(GoodsIssueStatus.CANCELLED);

        // 복원 원장(ADJUSTMENT_PLUS +6) 추가
        StockMovement restore = stockMovementRepository.findAll().stream()
                .filter(m -> m.getReason() == MovementReason.ADJUSTMENT_PLUS && m.getRefId().equals(giId))
                .findFirst().orElseThrow();
        assertThat(restore.getQtyDelta()).isEqualByComparingTo(bd(6));
    }

    @Test
    @DisplayName("여러 라인 출하는 한 GoodsIssue 가 여러 라인을 가지고 생성된다")
    void 다중_라인_출하는_한_GoodsIssue_가_여러_라인을_가진다() {
        // 노트북 5대 + 모니터 3대 한 출하
        long nano = System.nanoTime();
        var customer = createCustomerWithCreditLimit(
                "다중라인-" + nano, "서울시", new BigDecimal("100000000"));
        var notebook = itemService.create(new ItemCreateRequest(
                "노트북-" + nano, ItemCategory.NOTEBOOK, ItemUnit.EA, bd(800000), bd(1200000)));
        var monitor = itemService.create(new ItemCreateRequest(
                "모니터-" + nano, ItemCategory.MONITOR, ItemUnit.EA, bd(200000), bd(350000)));
        Long warehouseId = warehouseId();
        stockUp(notebook.id(), warehouseId, 5);
        stockUp(monitor.id(), warehouseId, 3);

        var order = salesOrderService.create(new SalesOrderCreateRequest(
                customer.id(), null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(notebook.id(), bd(5), bd(1200000)),
                        new SalesOrderLineRequest(monitor.id(), bd(3), bd(350000)))));
        salesOrderService.confirm(order.id());
        var soLines = salesOrderService.findById(order.id()).lines();
        Long notebookSolId = soLines.get(0).id();
        Long monitorSolId = soLines.get(1).id();

        DeliveryResponse dlv = deliveryService.create(new DeliveryCreateRequest(
                order.id(), warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(notebookSolId, bd(5)),
                        new DeliveryLineRequest(monitorSolId, bd(3)))));

        // 한 출하 → 한 GoodsIssue, 라인 2개
        Long giId = goodsIssueRepository.findByDeliveryId(dlv.id()).orElseThrow().getId();
        GoodsIssueResponse gi = goodsIssueService.findById(giId);
        assertThat(gi.lines()).hasSize(2);

        // 두 품목 재고 모두 0 차감
        assertThat(stockRepository.findByItemIdAndWarehouseId(notebook.id(), warehouseId)
                .orElseThrow().getQtyOnHand()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stockRepository.findByItemIdAndWarehouseId(monitor.id(), warehouseId)
                .orElseThrow().getQtyOnHand()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("BEFORE_COMMIT 리스너에서 어떤 예외가 나도 Delivery 트랜잭션이 통째로 롤백된다")
    void 리스너_예외는_Delivery_트랜잭션을_롤백시킨다() {
        // 입고를 전혀 안 함 → 출하 시 리스너에서 Stock not found(EntityNotFoundException)
        long nano = System.nanoTime();
        var customer = createCustomerWithCreditLimit(
                "롤백검증-" + nano, "서울시", new BigDecimal("100000000"));
        var item = itemService.create(new ItemCreateRequest(
                "노트북-" + nano, ItemCategory.NOTEBOOK, ItemUnit.EA, bd(800000), bd(1200000)));
        Long warehouseId = warehouseId();   // 재고 행 없음

        var order = salesOrderService.create(new SalesOrderCreateRequest(
                customer.id(), null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(item.id(), bd(3), bd(1200000)))));
        salesOrderService.confirm(order.id());
        Long solId = salesOrderService.findById(order.id()).lines().get(0).id();

        assertThatThrownBy(() -> deliveryService.create(new DeliveryCreateRequest(
                order.id(), warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(solId, bd(3))))))
                .isInstanceOf(EntityNotFoundException.class);

        // Delivery·SO 누적 모두 시도 전 상태 — shipped_qty 0
        var so = salesOrderService.findById(order.id());
        assertThat(so.lines().get(0).shippedQty()).isEqualByComparingTo(BigDecimal.ZERO);
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

    private record TestContext(Long itemId, Long warehouseId, Long soId, Long solId) {}

    /** 고객 + 노트북 + 창고 + qty 입고 + 10대 수주(확정) 까지 준비. */
    private TestContext setup(int stockQty) {
        long nano = System.nanoTime();
        var customer = createCustomerWithCreditLimit(
                "현우테크-" + nano, "서울시", new BigDecimal("100000000"));
        var item = itemService.create(new ItemCreateRequest(
                "노트북-" + nano, ItemCategory.NOTEBOOK, ItemUnit.EA, bd(800000), bd(1200000)));
        Long warehouseId = warehouseId();
        stockUp(item.id(), warehouseId, stockQty);

        var order = salesOrderService.create(new SalesOrderCreateRequest(
                customer.id(), null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(item.id(), bd(10), bd(1200000)))));
        salesOrderService.confirm(order.id());
        Long solId = salesOrderService.findById(order.id()).lines().get(0).id();
        return new TestContext(item.id(), warehouseId, order.id(), solId);
    }

    private Long warehouseId() {
        return warehouseService.create(new WarehouseCreateRequest(
                "WH-" + whSuffix(), "테스트창고", "서울시")).id();
    }

    private void stockUp(Long itemId, Long warehouseId, int qty) {
        var vendor = vendorService.create(new VendorCreateRequest(
                "거래처-" + System.nanoTime(), uniqueBusinessNo(), "인천시", PaymentTerms.NET30));
        var gr = goodsReceiptService.create(new GoodsReceiptCreateRequest(
                vendor.id(), warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(itemId, new BigDecimal(qty), new BigDecimal("1000")))));
        goodsReceiptService.post(gr.id());
    }

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime());

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

    private static BigDecimal bd(long n) { return new BigDecimal(n); }
}

package com.hwlee.erp.mm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.item.ItemCategory;
import com.hwlee.erp.master.item.ItemService;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.master.item.dto.ItemCreateRequest;
import com.hwlee.erp.master.vendor.VendorService;
import com.hwlee.erp.master.vendor.dto.VendorCreateRequest;
import com.hwlee.erp.mm.goodsissue.GoodsIssueReason;
import com.hwlee.erp.mm.goodsissue.GoodsIssueService;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueCreateRequest;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueLineRequest;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueResponse;
import com.hwlee.erp.mm.goodsreceipt.GoodsReceiptService;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptLineRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptResponse;
import com.hwlee.erp.mm.stock.InsufficientStockException;
import com.hwlee.erp.mm.stock.MovementReason;
import com.hwlee.erp.mm.stock.Stock;
import com.hwlee.erp.mm.stock.StockMovement;
import com.hwlee.erp.mm.stock.StockMovementRepository;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.mm.warehouse.WarehouseService;
import com.hwlee.erp.mm.warehouse.dto.WarehouseCreateRequest;
import com.hwlee.erp.mm.warehouse.dto.WarehouseResponse;
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
 * MM 핵심 시나리오 — 입고/출고 흐름 + 가중평균 + 음수 재고 방지 + 원장 정합성.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MmScenarioTest {

    @Autowired ItemService itemService;
    @Autowired VendorService vendorService;
    @Autowired WarehouseService warehouseService;
    @Autowired GoodsReceiptService goodsReceiptService;
    @Autowired GoodsIssueService goodsIssueService;
    @Autowired StockRepository stockRepository;
    @Autowired StockMovementRepository stockMovementRepository;

    @Test
    @DisplayName("입고 시 stock 행이 생기고, 두 번째 입고는 가중평균으로 갱신된다")
    void 입고_두번이면_가중평균으로_갱신된다() {
        var ctx = setup();

        // 첫 입고: 10대 @ 1000
        var gr1 = goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(ctx.itemId, bd(10), bd(1000)))));
        goodsReceiptService.post(gr1.id());

        Stock stock1 = stockRepository
                .findByItemIdAndWarehouseId(ctx.itemId, ctx.warehouseId).orElseThrow();
        assertThat(stock1.getQtyOnHand()).isEqualByComparingTo(bd(10));
        assertThat(stock1.getAverageCost()).isEqualByComparingTo(bd(1000));

        // 두 번째 입고: 10대 @ 1200 → 가중평균 = 1100
        var gr2 = goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(ctx.itemId, bd(10), bd(1200)))));
        goodsReceiptService.post(gr2.id());

        Stock stock2 = stockRepository
                .findByItemIdAndWarehouseId(ctx.itemId, ctx.warehouseId).orElseThrow();
        assertThat(stock2.getQtyOnHand()).isEqualByComparingTo(bd(20));
        assertThat(stock2.getAverageCost()).isEqualByComparingTo(bd(1100));
    }

    @Test
    @DisplayName("출고 시 직전 평균 단가가 StockMovement.unit_cost 로 박힌다")
    void 출고는_직전_평균을_원장에_기록한다() {
        var ctx = setup();

        // 입고로 20대, 평균 1100 만들기
        post(goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(ctx.itemId, bd(10), bd(1000))))).id());
        post(goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(ctx.itemId, bd(10), bd(1200))))).id());

        // 출고 7대
        var gi = goodsIssueService.create(new GoodsIssueCreateRequest(
                ctx.warehouseId, LocalDate.now(), GoodsIssueReason.SHIPMENT,
                List.of(new GoodsIssueLineRequest(ctx.itemId, bd(7)))));
        goodsIssueService.post(gi.id());

        Stock stock = stockRepository
                .findByItemIdAndWarehouseId(ctx.itemId, ctx.warehouseId).orElseThrow();
        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(bd(13));
        assertThat(stock.getAverageCost()).isEqualByComparingTo(bd(1100));

        StockMovement giMovement = stockMovementRepository.findAll().stream()
                .filter(m -> m.getReason() == MovementReason.GOODS_ISSUE && m.getRefId().equals(gi.id()))
                .findFirst().orElseThrow();
        assertThat(giMovement.getQtyDelta()).isEqualByComparingTo(bd(-7));
        assertThat(giMovement.getUnitCost()).isEqualByComparingTo(bd(1100));
    }

    @Test
    @DisplayName("가용 재고가 부족하면 InsufficientStockException — 음수 재고 방지")
    void 가용_재고_부족이면_거부된다() {
        var ctx = setup();
        post(goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(ctx.itemId, bd(5), bd(1000))))).id());

        GoodsIssueResponse draft = goodsIssueService.create(new GoodsIssueCreateRequest(
                ctx.warehouseId, LocalDate.now(), GoodsIssueReason.SHIPMENT,
                List.of(new GoodsIssueLineRequest(ctx.itemId, bd(6)))));

        assertThatThrownBy(() -> goodsIssueService.post(draft.id()))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("재고 부족");

        Stock stock = stockRepository
                .findByItemIdAndWarehouseId(ctx.itemId, ctx.warehouseId).orElseThrow();
        assertThat(stock.getQtyOnHand())
                .as("실패한 출고는 트랜잭션 롤백 — 재고는 그대로 5대")
                .isEqualByComparingTo(bd(5));
    }

    @Test
    @DisplayName("StockMovement.qty_delta 의 합은 Stock.qty_on_hand 와 항상 일치한다 (원장 정합성)")
    void 원장_합계는_Stock_과_일치한다() {
        var ctx = setup();

        // 입고 10대 → 출고 3대 → 입고 5대 → 출고 7대
        post(goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(ctx.itemId, bd(10), bd(1000))))).id());
        goodsIssueService.post(goodsIssueService.create(new GoodsIssueCreateRequest(
                ctx.warehouseId, LocalDate.now(), GoodsIssueReason.SHIPMENT,
                List.of(new GoodsIssueLineRequest(ctx.itemId, bd(3))))).id());
        post(goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(ctx.itemId, bd(5), bd(1400))))).id());
        goodsIssueService.post(goodsIssueService.create(new GoodsIssueCreateRequest(
                ctx.warehouseId, LocalDate.now(), GoodsIssueReason.SHIPMENT,
                List.of(new GoodsIssueLineRequest(ctx.itemId, bd(7))))).id());

        Stock stock = stockRepository
                .findByItemIdAndWarehouseId(ctx.itemId, ctx.warehouseId).orElseThrow();
        BigDecimal ledgerSum = stockMovementRepository.findAll().stream()
                .filter(m -> m.getItem().getId().equals(ctx.itemId)
                        && m.getWarehouse().getId().equals(ctx.warehouseId))
                .map(StockMovement::getQtyDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(stock.getQtyOnHand())
                .as("Stock.qty_on_hand == SUM(stock_movement.qty_delta)")
                .isEqualByComparingTo(ledgerSum);
        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(bd(5));   // 10 - 3 + 5 - 7 = 5
    }

    @Test
    @DisplayName("출고 취소시 Stock 수량이 복원된다 (평균은 그대로)")
    void 출고_취소시_수량이_복원된다() {
        var ctx = setup();
        post(goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(ctx.itemId, bd(10), bd(1000))))).id());
        var gi = goodsIssueService.create(new GoodsIssueCreateRequest(
                ctx.warehouseId, LocalDate.now(), GoodsIssueReason.SHIPMENT,
                List.of(new GoodsIssueLineRequest(ctx.itemId, bd(7)))));
        goodsIssueService.post(gi.id());

        goodsIssueService.cancel(gi.id());

        Stock stock = stockRepository
                .findByItemIdAndWarehouseId(ctx.itemId, ctx.warehouseId).orElseThrow();
        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(bd(10));
        assertThat(stock.getAverageCost()).isEqualByComparingTo(bd(1000));
    }

    // === helpers ===

    private void post(Long grId) {
        goodsReceiptService.post(grId);
    }

    private TestContext setup() {
        long nano = System.nanoTime();
        var item = itemService.create(new ItemCreateRequest(
                "노트북-" + nano, ItemCategory.NOTEBOOK, ItemUnit.EA, bd(800000), bd(1200000)));
        var vendor = vendorService.create(new VendorCreateRequest(
                "거래처-" + nano, uniqueBusinessNo(), "인천시",
                com.hwlee.erp.master.customer.PaymentTerms.NET30));
        WarehouseResponse wh = warehouseService.create(new WarehouseCreateRequest(
                "WH-" + uniqueWhSuffix(), "테스트창고", "서울시"));
        return new TestContext(item.id(), vendor.id(), wh.id());
    }

    private record TestContext(Long itemId, Long vendorId, Long warehouseId) {}

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime());

    private static String uniqueBusinessNo() {
        long n = SEQ.incrementAndGet();
        return String.format("%03d-%02d-%05d",
                (int) ((n / 10_000_000L) % 900) + 100,
                (int) ((n / 100_000L) % 100),
                (int) (n % 100_000L));
    }

    private static String uniqueWhSuffix() {
        long n = SEQ.incrementAndGet();
        // WH-XXX 형식 만족 (대문자/숫자/하이픈)
        return "T" + Long.toString(n, 36).toUpperCase().replace("-", "");
    }

    private static BigDecimal bd(long n) { return new BigDecimal(n); }
}

package com.hwlee.erp.mm;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.hwlee.erp.mm.goodsreceipt.GoodsReceiptService;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptLineRequest;
import com.hwlee.erp.mm.stock.InsufficientStockException;
import com.hwlee.erp.mm.stock.Stock;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.mm.warehouse.WarehouseService;
import com.hwlee.erp.mm.warehouse.dto.WarehouseCreateRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Phase 3 의 하이라이트 — 동시 출고가 비관적 락으로 직렬화되어 음수 재고가 발생하지 않는다.
 *
 * <p>10대 재고에 8개 스레드가 각자 2대씩 동시 출고 (총 요청 16대) →
 * 5개 성공 (10대 = 5 × 2), 3개는 INSUFFICIENT_STOCK 거부. 재고 종료값은 0.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrentGoodsIssueTest {

    @Autowired ItemService itemService;
    @Autowired VendorService vendorService;
    @Autowired WarehouseService warehouseService;
    @Autowired GoodsReceiptService goodsReceiptService;
    @Autowired GoodsIssueService goodsIssueService;
    @Autowired StockRepository stockRepository;

    @Test
    @DisplayName("동시_출고_요청은_비관적_락으로_직렬화되어_음수_재고가_발생하지_않는다")
    void 동시_출고_요청은_비관적_락으로_직렬화되어_음수_재고가_발생하지_않는다() throws Exception {
        long nano = System.nanoTime();
        var item = itemService.create(new ItemCreateRequest(
                "노트북-" + nano, ItemCategory.NOTEBOOK, ItemUnit.EA,
                new BigDecimal("800000"), new BigDecimal("1200000")));
        var vendor = vendorService.create(new VendorCreateRequest(
                "거래처-" + nano, uniqueBusinessNo(), "인천시",
                com.hwlee.erp.master.customer.PaymentTerms.NET30));
        var warehouse = warehouseService.create(new WarehouseCreateRequest(
                "WH-" + uniqueWhSuffix(), "동시성테스트창고", "서울시"));

        // 시작 재고 10대
        var gr = goodsReceiptService.create(new GoodsReceiptCreateRequest(
                vendor.id(), warehouse.id(), LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(item.id(), new BigDecimal("10"), new BigDecimal("1000")))));
        goodsReceiptService.post(gr.id());

        // 8 개 스레드가 각자 2대씩 출고 시도 — 총 요청 16대, 가능한 만큼만 성공
        int threads = 8;
        BigDecimal perRequest = new BigDecimal("2");

        // DRAFT 출고들을 먼저 만들어둔다 (트랜잭션 번호 발급도 동시성 영향을 받지만 그건 별도 테스트에서 검증된 것)
        List<Long> issueDraftIds = IntStream.range(0, threads).mapToObj(i ->
                goodsIssueService.create(new GoodsIssueCreateRequest(
                        warehouse.id(), LocalDate.now(), GoodsIssueReason.SHIPMENT,
                        List.of(new GoodsIssueLineRequest(item.id(), perRequest)))).id()
        ).toList();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        List<Future<?>> futures = issueDraftIds.stream().<Future<?>>map(giId -> pool.submit(() -> {
            try {
                startGate.await();
                goodsIssueService.post(giId);
                successes.incrementAndGet();
            } catch (InsufficientStockException isx) {
                failures.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        })).toList();

        startGate.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        for (Future<?> f : futures) f.get();

        // 5개 성공, 3개 실패 (10 / 2 = 5)
        assertThat(successes.get()).as("성공한 출고 수").isEqualTo(5);
        assertThat(failures.get()).as("실패한 출고 수 (가용 부족)").isEqualTo(3);

        Stock stock = stockRepository
                .findByItemIdAndWarehouseId(item.id(), warehouse.id()).orElseThrow();
        assertThat(stock.getQtyOnHand()).as("종료 재고는 정확히 0").isEqualByComparingTo(BigDecimal.ZERO);
    }

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
        return "T" + Long.toString(n, 36).toUpperCase().replace("-", "");
    }
}

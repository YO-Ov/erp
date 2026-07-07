package com.hwlee.erp.mm.purchaseorder;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.item.ItemService;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.master.item.dto.ItemCreateRequest;
import com.hwlee.erp.master.vendor.VendorService;
import com.hwlee.erp.master.vendor.dto.VendorCreateRequest;
import com.hwlee.erp.master.vendoritem.VendorItemService;
import com.hwlee.erp.master.vendoritem.dto.VendorItemCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.GoodsReceiptService;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptLineRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptResponse;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderCreateRequest;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderLineRequest;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderResponse;
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
 * 구매발주(PO) ↔ 입고(GR) 연동 통합 검증 — 발주 대비 입고 누계 집계 + 부분/전량 입고 상태 전이.
 *
 * <p>결재 상신·승인은 이 테스트의 관심사가 아니므로 {@code confirmByApproval} 로 CONFIRMED 를
 * 직접 만들고, 그 이후 입고 흐름(집계 쿼리·매퍼·post/cancel 동기화)을 검증한다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PurchaseOrderReceiptIntegrationTest {

    @Autowired ItemService itemService;
    @Autowired VendorService vendorService;
    @Autowired VendorItemService vendorItemService;
    @Autowired WarehouseService warehouseService;
    @Autowired PurchaseOrderService purchaseOrderService;
    @Autowired GoodsReceiptService goodsReceiptService;

    @Test
    @DisplayName("부분 입고면 CONFIRMED 유지, 전량 입고되면 RECEIVED 로 자동 전이한다")
    void 발주_대비_부분입고_전량입고_상태전이() {
        var ctx = setup();

        // 발주: 부품A 100 @ 1000, 부품B 50 @ 2000
        var po = purchaseOrderService.create(new PurchaseOrderCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(), null, "통합테스트 발주",
                List.of(new PurchaseOrderLineRequest(ctx.itemA, bd(100), bd(1000)),
                        new PurchaseOrderLineRequest(ctx.itemB, bd(50), bd(2000)))));
        purchaseOrderService.confirmByApproval(po.id());   // 결재 우회로 CONFIRMED

        // 1차 부분 입고: A 60만 입고
        var gr1 = goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(), po.id(),
                List.of(new GoodsReceiptLineRequest(ctx.itemA, bd(60), bd(1000)))));
        goodsReceiptService.post(gr1.id());

        PurchaseOrderResponse afterPartial = purchaseOrderService.findById(po.id());
        assertThat(afterPartial.status())
                .as("아직 미납이 남아 CONFIRMED 유지")
                .isEqualTo(PurchaseOrderStatus.CONFIRMED);
        var lineA = lineOf(afterPartial, ctx.itemA);
        assertThat(lineA.receivedQuantity()).isEqualByComparingTo(bd(60));
        assertThat(lineA.openQuantity()).isEqualByComparingTo(bd(40));
        var lineB = lineOf(afterPartial, ctx.itemB);
        assertThat(lineB.receivedQuantity()).isEqualByComparingTo(bd(0));
        assertThat(lineB.openQuantity()).isEqualByComparingTo(bd(50));

        // 2차 잔량 입고: A 40 + B 50 → 전량 충족
        var gr2 = goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(), po.id(),
                List.of(new GoodsReceiptLineRequest(ctx.itemA, bd(40), bd(1000)),
                        new GoodsReceiptLineRequest(ctx.itemB, bd(50), bd(2000)))));
        goodsReceiptService.post(gr2.id());

        PurchaseOrderResponse afterFull = purchaseOrderService.findById(po.id());
        assertThat(afterFull.status())
                .as("전 라인 입고 충족 → RECEIVED 자동 전이")
                .isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(lineOf(afterFull, ctx.itemA).openQuantity()).isEqualByComparingTo(bd(0));
        assertThat(lineOf(afterFull, ctx.itemB).openQuantity()).isEqualByComparingTo(bd(0));
    }

    @Test
    @DisplayName("전량 입고 후 그 입고를 취소하면 발주가 RECEIVED→CONFIRMED 로 복귀한다")
    void 입고_취소시_발주가_CONFIRMED로_복귀() {
        var ctx = setup();

        var po = purchaseOrderService.create(new PurchaseOrderCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(), null, null,
                List.of(new PurchaseOrderLineRequest(ctx.itemA, bd(30), bd(1000)))));
        purchaseOrderService.confirmByApproval(po.id());

        GoodsReceiptResponse gr = goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(), po.id(),
                List.of(new GoodsReceiptLineRequest(ctx.itemA, bd(30), bd(1000)))));
        goodsReceiptService.post(gr.id());
        assertThat(purchaseOrderService.findById(po.id()).status())
                .isEqualTo(PurchaseOrderStatus.RECEIVED);

        goodsReceiptService.cancel(gr.id());
        assertThat(purchaseOrderService.findById(po.id()).status())
                .as("입고가 집계에서 빠져 미달 → CONFIRMED 복귀")
                .isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(lineOf(purchaseOrderService.findById(po.id()), ctx.itemA).receivedQuantity())
                .isEqualByComparingTo(bd(0));
    }

    // === helpers ===

    private static com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderLineResponse lineOf(
            PurchaseOrderResponse po, Long itemId) {
        return po.lines().stream().filter(l -> l.itemId().equals(itemId)).findFirst().orElseThrow();
    }

    private TestContext setup() {
        long nano = System.nanoTime();
        // 카테고리는 활성 코드면 무엇이든 무관(발주·입고는 취급품목 게이트만 본다). NOTEBOOK 재사용.
        var itemA = itemService.create(new ItemCreateRequest(
                "품목A-" + nano, "NOTEBOOK", ItemUnit.EA, bd(1000), bd(1500)));
        var itemB = itemService.create(new ItemCreateRequest(
                "품목B-" + nano, "NOTEBOOK", ItemUnit.EA, bd(2000), bd(3000)));
        var vendor = vendorService.create(new VendorCreateRequest(
                "거래처-" + nano, uniqueBusinessNo(), "인천시", PaymentTerms.NET30));
        WarehouseResponse wh = warehouseService.create(new WarehouseCreateRequest(
                "WH-" + uniqueWhSuffix(), "테스트창고", "서울시"));
        // 발주·입고 모두 거래처 취급품목(구매정보레코드) 게이트를 거치므로 두 품목을 매핑.
        vendorItemService.create(new VendorItemCreateRequest(vendor.id(), itemA.id(), bd(1000), 7));
        vendorItemService.create(new VendorItemCreateRequest(vendor.id(), itemB.id(), bd(2000), 7));
        return new TestContext(itemA.id(), itemB.id(), vendor.id(), wh.id());
    }

    private record TestContext(Long itemA, Long itemB, Long vendorId, Long warehouseId) {}

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

    private static BigDecimal bd(long n) { return new BigDecimal(n); }
}

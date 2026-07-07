package com.hwlee.erp.mm.purchaseorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.vendor.Vendor;
import com.hwlee.erp.mm.warehouse.Warehouse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 구매발주(PO) 도메인 상태 머신 검증 — 결재 없이 확정 불가·라인 합계·취소/종료 규칙의 살아있는 문서.
 */
class PurchaseOrderTest {

    @Test
    @DisplayName("DRAFT 발주만 confirm 가능 — 두 번 확정하면 거부")
    void confirm_DRAFT가_아니면_거부된다() {
        PurchaseOrder po = poWith2Lines();
        po.confirm();

        assertThatThrownBy(po::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    @DisplayName("라인이 비어 있는 발주는 confirm 불가")
    void confirm_라인이_비어있으면_거부된다() {
        PurchaseOrder po = PurchaseOrder.draft("PORD-20260706-001", vendor(), warehouse(),
                LocalDate.now(), null, null);

        assertThatThrownBy(po::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("라인이 비어");
    }

    @Test
    @DisplayName("확정된 발주는 라인 추가/수정 불가 — DRAFT 에서만 편집")
    void addLine_확정된_발주는_라인_추가_불가() {
        PurchaseOrder po = poWith2Lines();
        po.confirm();

        assertThatThrownBy(() -> po.addLine(part(), bd(1), bd(1000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    @DisplayName("CONFIRMED 발주만 close 가능 — DRAFT 종료는 거부")
    void close_CONFIRMED가_아니면_거부된다() {
        PurchaseOrder po = poWith2Lines();

        assertThatThrownBy(po::close)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFIRMED");

        po.confirm();
        po.close();
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.CLOSED);
    }

    @Test
    @DisplayName("DRAFT/CONFIRMED 발주만 취소 가능, CLOSED 취소는 거부")
    void cancel_DRAFT_CONFIRMED만_가능() {
        PurchaseOrder draft = poWith2Lines();
        draft.cancel();
        assertThat(draft.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);

        PurchaseOrder confirmed = poWith2Lines();
        confirmed.confirm();
        confirmed.cancel();
        assertThat(confirmed.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);

        PurchaseOrder closed = poWith2Lines();
        closed.confirm();
        closed.close();
        assertThatThrownBy(closed::cancel)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("전량 입고되면 CONFIRMED→RECEIVED, 입고 취소로 미달되면 다시 CONFIRMED")
    void syncReceiptStatus_전량입고면_RECEIVED_미달되면_CONFIRMED() {
        PurchaseOrder po = poWith2Lines();
        po.confirm();

        // 부분 입고(미달) — 여전히 CONFIRMED
        po.syncReceiptStatus(false);
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);

        // 전량 입고 — RECEIVED 로 전이
        po.syncReceiptStatus(true);
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);

        // 입고 취소로 다시 미달 — CONFIRMED 로 복귀
        po.syncReceiptStatus(false);
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("DRAFT 발주는 입고 동기화의 영향을 받지 않는다")
    void syncReceiptStatus_DRAFT는_무시된다() {
        PurchaseOrder po = poWith2Lines();   // DRAFT
        po.syncReceiptStatus(true);
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
    }

    @Test
    @DisplayName("RECEIVED(입고완료) 발주도 close 로 종료 가능하고, 취소는 거부된다")
    void received_발주는_close가능_cancel거부() {
        PurchaseOrder received = poWith2Lines();
        received.confirm();
        received.syncReceiptStatus(true);
        assertThat(received.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);

        assertThatThrownBy(received::cancel)
                .isInstanceOf(IllegalStateException.class);

        received.close();
        assertThat(received.getStatus()).isEqualTo(PurchaseOrderStatus.CLOSED);
    }

    @Test
    @DisplayName("totalAmount 는 라인(수량×매입단가) 합계로 집계된다")
    void totalAmount는_라인_합계로_집계된다() {
        PurchaseOrder po = PurchaseOrder.draft("PORD-20260706-001", vendor(), warehouse(),
                LocalDate.now(), null, null);
        po.addLine(part(), bd(500), bd(1200));   //   600_000
        po.addLine(part(), bd(300), bd(2000));   //   600_000

        assertThat(po.totalAmount()).isEqualByComparingTo(bd(1_200_000));
    }

    // === helpers ===

    private static PurchaseOrder poWith2Lines() {
        PurchaseOrder po = PurchaseOrder.draft("PORD-20260706-001", vendor(), warehouse(),
                LocalDate.now(), LocalDate.now().plusDays(7), "긴급 발주");
        po.addLine(part(), bd(500), bd(1200));
        po.addLine(part(), bd(300), bd(2000));
        return po;
    }

    private static Vendor vendor() {
        return Vendor.create("VEND-2026-0001", "대한부품", "222-33-44444", "인천시", PaymentTerms.NET30);
    }

    private static Warehouse warehouse() {
        return Warehouse.create("WH-HQ", "중앙물류창고", "수원시");
    }

    private static Item part() {
        return Item.create("ITEM-2026-0100", "메인보드", "PART", ItemUnit.EA, bd(1200), bd(0));
    }

    private static BigDecimal bd(long n) {
        return new BigDecimal(n);
    }
}

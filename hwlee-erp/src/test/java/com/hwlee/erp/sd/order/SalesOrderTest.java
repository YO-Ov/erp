package com.hwlee.erp.sd.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 수주 도메인 메서드의 상태 머신/누적 검증 — Phase 2 핵심 업무 규칙의 살아있는 문서.
 */
class SalesOrderTest {

    @Test
    @DisplayName("DRAFT 가 아닌 수주를 confirm 하면 거부된다")
    void confirm_DRAFT가_아니면_거부된다() {
        SalesOrder order = orderWith2Lines();
        order.confirm(LocalDateTime.now());

        assertThatThrownBy(() -> order.confirm(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    @DisplayName("라인이 비어 있는 수주는 confirm 불가")
    void confirm_라인이_비어있으면_거부된다() {
        SalesOrder order = SalesOrder.draft("SO-20260524-001", customer(), null, null, LocalDate.now());

        assertThatThrownBy(() -> order.confirm(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("라인이 비어");
    }

    @Test
    @DisplayName("확정된 수주는 라인 추가 불가")
    void addLine_확정된_수주는_라인_추가_불가() {
        SalesOrder order = orderWith2Lines();
        order.confirm(LocalDateTime.now());

        assertThatThrownBy(() -> order.addLine(itemNotebook(), bd(1), bd(1000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    @DisplayName("출하 누적이 주문량을 초과하면 거부된다")
    void recordShipment_주문량_초과면_거부된다() {
        SalesOrder order = orderWith2Lines();
        order.confirm(LocalDateTime.now());
        SalesOrderLine line = order.getLines().get(0);  // order_qty=10

        order.recordShipment(line, bd(7));
        assertThatThrownBy(() -> order.recordShipment(line, bd(5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주문량을 초과");
    }

    @Test
    @DisplayName("전량 출하되면 SHIPPED 상태로 전이된다")
    void recordShipment_전량_출하되면_SHIPPED_상태로_전이() {
        SalesOrder order = orderWith2Lines();
        order.confirm(LocalDateTime.now());
        SalesOrderLine line1 = order.getLines().get(0);  // qty=10
        SalesOrderLine line2 = order.getLines().get(1);  // qty=5

        order.recordShipment(line1, bd(6));
        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.SHIPPING);

        order.recordShipment(line1, bd(4));   // line1 완료
        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.SHIPPING);

        order.recordShipment(line2, bd(5));   // line2 완료 — 전량 출하
        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("청구 누적이 출하 누적을 초과하면 거부된다 — 보낸 만큼만 청구 가능")
    void recordInvoicing_출하량_초과면_거부된다() {
        SalesOrder order = orderWith2Lines();
        order.confirm(LocalDateTime.now());
        SalesOrderLine line = order.getLines().get(0);

        order.recordShipment(line, bd(6));
        order.recordInvoicing(line, bd(6));
        assertThatThrownBy(() -> order.recordInvoicing(line, bd(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("출하 누적을 초과");
    }

    @Test
    @DisplayName("출하 실적이 있는 수주는 취소할 수 없다 — SHIPPING 이후 cancel 금지")
    void cancel_출하_실적이_있으면_거부된다() {
        SalesOrder order = orderWith2Lines();
        order.confirm(LocalDateTime.now());
        order.recordShipment(order.getLines().get(0), bd(3));   // → SHIPPING

        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT 또는 CONFIRMED");
    }

    @Test
    @DisplayName("DRAFT 또는 CONFIRMED 수주만 취소 가능")
    void cancel_DRAFT_CONFIRMED만_가능() {
        SalesOrder draftOrder = orderWith2Lines();
        draftOrder.cancel();
        assertThat(draftOrder.getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);

        SalesOrder confirmedOrder = orderWith2Lines();
        confirmedOrder.confirm(LocalDateTime.now());
        confirmedOrder.cancel();
        assertThat(confirmedOrder.getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("전량 청구(INVOICED)된 수주는 CLOSED 로 마감된다")
    void close_전량_청구되면_CLOSED로_마감된다() {
        SalesOrder order = fullyInvoicedOrder();
        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.INVOICED);

        order.close();
        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.CLOSED);
    }

    @Test
    @DisplayName("INVOICED 가 아닌 수주는 close 할 수 없다 — SHIPPED 단계 마감 금지")
    void close_INVOICED가_아니면_거부된다() {
        SalesOrder order = orderWith2Lines();
        order.confirm(LocalDateTime.now());
        order.recordShipment(order.getLines().get(0), bd(10));
        order.recordShipment(order.getLines().get(1), bd(5));   // → SHIPPED (청구 전)

        assertThatThrownBy(order::close)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INVOICED");
    }

    @Test
    @DisplayName("CLOSED 수주는 추가 출하/청구가 거부된다 — 마감 후 동결")
    void close_이후_진행이_거부된다() {
        SalesOrder order = fullyInvoicedOrder();
        order.close();

        assertThatThrownBy(() -> order.recordShipment(order.getLines().get(0), bd(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("totalAmount 는 라인 합계로 재계산된다")
    void totalAmount는_라인_합계로_재계산된다() {
        SalesOrder order = SalesOrder.draft("SO-20260524-001", customer(), null, null, LocalDate.now());
        order.addLine(itemNotebook(), bd(10), bd(1200000));   // 12_000_000
        order.addLine(itemMonitor(), bd(5), bd(350000));      //  1_750_000

        assertThat(order.getTotalAmount()).isEqualByComparingTo(bd(13_750_000));
    }

    // === helpers ===

    private static SalesOrder orderWith2Lines() {
        SalesOrder order = SalesOrder.draft("SO-20260524-001", customer(), null, null, LocalDate.now());
        order.addLine(itemNotebook(), bd(10), bd(1200000));  // line1
        order.addLine(itemMonitor(), bd(5), bd(350000));     // line2
        return order;
    }

    /** 두 라인 모두 전량 출하·청구까지 마쳐 INVOICED 상태가 된 수주. */
    private static SalesOrder fullyInvoicedOrder() {
        SalesOrder order = orderWith2Lines();
        order.confirm(LocalDateTime.now());
        SalesOrderLine line1 = order.getLines().get(0);  // qty=10
        SalesOrderLine line2 = order.getLines().get(1);  // qty=5
        order.recordShipment(line1, bd(10));
        order.recordShipment(line2, bd(5));
        order.recordInvoicing(line1, bd(10));
        order.recordInvoicing(line2, bd(5));
        return order;
    }

    private static Customer customer() {
        return Customer.create("CUST-2026-0001", "신원전자", "111-22-33333",
                "서울시", new BigDecimal("100000000"), PaymentTerms.NET30);
    }

    private static Item itemNotebook() {
        return Item.create("ITEM-2026-0001", "노트북", "NOTEBOOK", ItemUnit.EA,
                bd(800000), bd(1200000));
    }

    private static Item itemMonitor() {
        return Item.create("ITEM-2026-0002", "모니터", "MONITOR", ItemUnit.EA,
                bd(200000), bd(350000));
    }

    private static BigDecimal bd(long n) {
        return new BigDecimal(n);
    }
}

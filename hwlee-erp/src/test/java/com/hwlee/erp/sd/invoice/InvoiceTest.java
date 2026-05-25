package com.hwlee.erp.sd.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemCategory;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.sd.order.SalesOrder;
import com.hwlee.erp.sd.order.SalesOrderLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 인보이스 부가세/총액 계산 — Phase 2 단순화 규칙(한국 부가세 10% 단일).
 */
class InvoiceTest {

    @Test
    @DisplayName("부가세는 공급가의 10퍼센트")
    void 부가세는_공급가의_10퍼센트() {
        SalesOrder order = orderWithLine10();
        SalesOrderLine sol = order.getLines().get(0);
        order.recordShipment(sol, new BigDecimal("6"));

        Invoice invoice = Invoice.draft("INV-20260524-001", order, LocalDate.now());
        invoice.addLine(sol, new BigDecimal("6"));

        // 6 * 1_200_000 = 7_200_000, tax = 720_000, total = 7_920_000
        assertThat(invoice.getSubtotal()).isEqualByComparingTo(new BigDecimal("7200000"));
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo(new BigDecimal("720000"));
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("7920000"));
    }

    @Test
    @DisplayName("subtotal + tax = total 합산 정합성")
    void subtotal_tax_total_합산_정합성() {
        SalesOrder order = orderWithLine10();
        SalesOrderLine sol = order.getLines().get(0);
        order.recordShipment(sol, new BigDecimal("3"));

        Invoice invoice = Invoice.draft("INV-20260524-001", order, LocalDate.now());
        invoice.addLine(sol, new BigDecimal("3"));

        BigDecimal sum = invoice.getSubtotal().add(invoice.getTaxAmount());
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(sum);
    }

    private static SalesOrder orderWithLine10() {
        Customer c = Customer.create("CUST-2026-0001", "신원전자", "111-22-33333",
                "서울시", new BigDecimal("100000000"), PaymentTerms.NET30);
        Item item = Item.create("ITEM-2026-0001", "노트북", ItemCategory.NOTEBOOK, ItemUnit.EA,
                new BigDecimal("800000"), new BigDecimal("1200000"));
        SalesOrder order = SalesOrder.draft("SO-20260524-001", c, null, null, LocalDate.now());
        order.addLine(item, new BigDecimal("10"), new BigDecimal("1200000"));
        order.confirm(LocalDateTime.now());
        return order;
    }
}

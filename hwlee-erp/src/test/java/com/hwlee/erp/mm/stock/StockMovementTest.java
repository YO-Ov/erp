package com.hwlee.erp.mm.stock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemCategory;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.mm.warehouse.Warehouse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StockMovementTest {

    @Test
    @DisplayName("qty_delta 가 0 이면 거부된다")
    void qty_delta_0_이면_거부() {
        assertThatThrownBy(() -> StockMovement.of(item(), warehouse(), bd(0), bd(1000),
                MovementReason.GOODS_RECEIPT, "GR", 1L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("qtyDelta");
    }

    @Test
    @DisplayName("GOODS_RECEIPT 인데 qty_delta 가 음수면 거부된다 — 부호와 reason 일치")
    void reason_부호_불일치_거부() {
        assertThatThrownBy(() -> StockMovement.of(item(), warehouse(), bd(-5), bd(1000),
                MovementReason.GOODS_RECEIPT, "GR", 1L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("양수");

        assertThatThrownBy(() -> StockMovement.of(item(), warehouse(), bd(5), bd(1000),
                MovementReason.GOODS_ISSUE, "GI", 1L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");
    }

    private static Item item() {
        return Item.create("ITEM-2026-0001", "노트북", ItemCategory.NOTEBOOK, ItemUnit.EA,
                bd(800000), bd(1200000));
    }

    private static Warehouse warehouse() {
        return Warehouse.create("WH-HQ", "본사창고", "서울시");
    }

    private static BigDecimal bd(long n) {
        return new BigDecimal(n);
    }
}

package com.hwlee.erp.mm.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.mm.warehouse.Warehouse;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Stock 도메인 메서드 — 가중평균 / 가용 검증의 살아있는 명세.
 */
class StockTest {

    @Test
    @DisplayName("첫 입고는 입고 단가가 그대로 평균이 된다")
    void 첫_입고는_입고_단가가_평균이_된다() {
        Stock stock = Stock.empty(item(), warehouse());
        stock.receive(bd(10), bd(1000));

        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(bd(10));
        assertThat(stock.getAverageCost()).isEqualByComparingTo(bd(1000));
    }

    @Test
    @DisplayName("두 번째 입고는 가중평균으로 평균을 갱신한다")
    void 두번째_입고는_가중평균으로_갱신된다() {
        Stock stock = Stock.empty(item(), warehouse());
        stock.receive(bd(10), bd(1000));     // qty=10, avg=1000
        stock.receive(bd(10), bd(1200));     // qty=20, avg=(10*1000+10*1200)/20=1100

        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(bd(20));
        assertThat(stock.getAverageCost()).isEqualByComparingTo(bd(1100));
    }

    @Test
    @DisplayName("출고는 보유량을 차감하고, 평균 단가는 그대로 둔다")
    void 출고는_평균_단가를_유지한다() {
        Stock stock = Stock.empty(item(), warehouse());
        stock.receive(bd(20), bd(1100));

        BigDecimal applied = stock.issue(bd(7));

        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(bd(13));
        assertThat(stock.getAverageCost()).isEqualByComparingTo(bd(1100));
        assertThat(applied).as("적용 단가 = 직전 평균").isEqualByComparingTo(bd(1100));
    }

    @Test
    @DisplayName("가용 재고보다 많이 출고하려고 하면 InsufficientStockException")
    void 가용_재고_미만이면_InsufficientStockException() {
        Stock stock = Stock.empty(item(), warehouse());
        stock.receive(bd(5), bd(1000));

        assertThatThrownBy(() -> stock.issue(bd(6)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("재고 부족");
    }

    @Test
    @DisplayName("출고 취소는 수량을 다시 더하고 평균은 건드리지 않는다")
    void 출고_취소는_수량을_다시_더하고_평균은_건드리지_않는다() {
        Stock stock = Stock.empty(item(), warehouse());
        stock.receive(bd(10), bd(1000));
        stock.issue(bd(7));                  // qty=3, avg=1000

        stock.cancelIssue(bd(7));            // qty=10, avg=1000 (그대로)

        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(bd(10));
        assertThat(stock.getAverageCost()).isEqualByComparingTo(bd(1000));
    }

    @Test
    @DisplayName("입고 취소는 수량만 차감, 평균은 그대로 — 사이 출고가 끼면 역산 불가하므로")
    void 입고_취소는_수량만_차감하고_평균은_건드리지_않는다() {
        Stock stock = Stock.empty(item(), warehouse());
        stock.receive(bd(10), bd(1000));
        stock.receive(bd(10), bd(1200));    // qty=20, avg=1100

        stock.cancelReceipt(bd(10));        // qty=10, avg=1100 (그대로 — 역산 X)

        assertThat(stock.getQtyOnHand()).isEqualByComparingTo(bd(10));
        assertThat(stock.getAverageCost()).isEqualByComparingTo(bd(1100));
    }

    @Test
    @DisplayName("입고 수량이 0 이하이면 거부된다")
    void 입고_수량_0_이하_거부() {
        Stock stock = Stock.empty(item(), warehouse());
        assertThatThrownBy(() -> stock.receive(bd(0), bd(1000)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.receive(bd(-1), bd(1000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // === helpers ===

    private static Item item() {
        return Item.create("ITEM-2026-0001", "노트북", "NOTEBOOK", ItemUnit.EA,
                bd(800000), bd(1200000));
    }

    private static Warehouse warehouse() {
        Warehouse w = Warehouse.create("WH-HQ", "본사창고", "서울시");
        // status 는 BaseEntityWithCode 가 ACTIVE 로 자동 세팅
        if (w.getStatus() != MasterStatus.ACTIVE) {
            w.changeStatus(MasterStatus.ACTIVE);
        }
        return w;
    }

    private static BigDecimal bd(long n) {
        return new BigDecimal(n);
    }
}

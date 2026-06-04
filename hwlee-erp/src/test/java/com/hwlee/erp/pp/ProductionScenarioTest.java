package com.hwlee.erp.pp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.fi.journal.JournalEntry;
import com.hwlee.erp.fi.journal.JournalEntryRepository;
import com.hwlee.erp.fi.journal.JournalSource;
import com.hwlee.erp.fi.journal.SystemAccounts;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.master.item.ItemType;
import com.hwlee.erp.mm.stock.InsufficientStockException;
import com.hwlee.erp.mm.stock.Stock;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.mm.warehouse.Warehouse;
import com.hwlee.erp.mm.warehouse.WarehouseRepository;
import com.hwlee.erp.pp.order.ProductionOrderStatus;
import com.hwlee.erp.pp.order.ProductionService;
import com.hwlee.erp.pp.order.dto.ProductionOrderCreateRequest;
import com.hwlee.erp.pp.order.dto.ProductionOrderResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Phase 8 의 하이라이트 — 생산이 재고를 변신시키고(부품↓ + 완제품↑) 회계로 흐르는 흐름.
 *
 * <p>V35 시드(부품 5종 + 노트북 BOM + 부품 기초재고 500개)를 활용한다.
 * <ol>
 *   <li>생산지시 생성 → 소요 자재 = BOM × 수량 (MRP 미니)</li>
 *   <li>완료 → 부품 출고(재고 −) + 완제품 입고(재고 +), 원가 보존</li>
 *   <li>생산 완료 전표 차)제품 / 대)원재료 = 직접재료비</li>
 *   <li>부품 부족 시 완료 → InsufficientStockException + 전체 롤백(부분 출고 없음)</li>
 * </ol>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProductionScenarioTest {

    @Autowired ProductionService productionService;
    @Autowired ItemRepository itemRepository;
    @Autowired WarehouseRepository warehouseRepository;
    @Autowired StockRepository stockRepository;
    @Autowired JournalEntryRepository journalEntryRepository;

    private Item notebook() {
        return itemRepository.findAll().stream()
                .filter(i -> i.getItemType() == ItemType.FINISHED && i.getName().contains("노트북"))
                .findFirst().orElseThrow();
    }

    private Item component(String name) {
        return itemRepository.findAll().stream()
                .filter(i -> i.getItemType() == ItemType.COMPONENT && i.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private Warehouse hqWarehouse() {
        return warehouseRepository.findAll().stream()
                .filter(w -> "WH-HQ".equals(w.getCode()))
                .findFirst().orElseThrow();
    }

    private BigDecimal onHand(Item item, Warehouse wh) {
        return stockRepository.findByItemIdAndWarehouseId(item.getId(), wh.getId())
                .map(Stock::getQtyOnHand).orElse(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("생산지시: BOM 전개 → 완료 시 부품 출고 + 완제품 입고 + 차)제품/대)원재료 전표")
    void produceNotebooks() {
        Item nb = notebook();
        Warehouse wh = hqWarehouse();
        Item memory = component("16GB 메모리");

        BigDecimal memBefore = onHand(memory, wh);     // 시드 500
        BigDecimal nbBefore = onHand(nb, wh);          // 시드 없음 → 0

        // 1) 생성: 노트북 5대 → 소요 자재 = BOM × 5 (메모리 2×5=10)
        ProductionOrderResponse po = productionService.createDraft(
                new ProductionOrderCreateRequest(nb.getId(), wh.getId(), new BigDecimal("5"),
                        LocalDate.of(2026, 6, 4), null));
        assertThat(po.status()).isEqualTo(ProductionOrderStatus.PLANNED);
        assertThat(po.lines()).hasSize(5);
        assertThat(po.lines()).anySatisfy(l -> {
            assertThat(l.componentName()).isEqualTo("16GB 메모리");
            assertThat(l.requiredQty()).isEqualByComparingTo("10");
        });

        // 2) 착수 → 완료
        productionService.release(po.id());
        ProductionOrderResponse done = productionService.complete(po.id());
        assertThat(done.status()).isEqualTo(ProductionOrderStatus.COMPLETED);

        // 3) 재고: 메모리 −10, 완제품 +5
        assertThat(onHand(memory, wh)).isEqualByComparingTo(memBefore.subtract(new BigDecimal("10")));
        assertThat(onHand(nb, wh)).isEqualByComparingTo(nbBefore.add(new BigDecimal("5")));

        // 4) 출고 단가 기록됨(완제품 원가 = 직접재료비 합 ÷ 수량)
        assertThat(done.lines()).allSatisfy(l -> assertThat(l.issuedUnitCost()).isNotNull());

        // 5) 생산 완료 전표 차)제품 / 대)원재료 — 직접재료비 = 5 × 680,000 = 3,400,000
        BigDecimal materialCost = new BigDecimal("3400000.00");
        JournalEntry je = journalEntryRepository.findAll().stream()
                .filter(e -> e.getSourceType() == JournalSource.PROD && po.id().equals(e.getSourceId()))
                .findFirst().orElseThrow();
        assertThat(je.getLines()).anySatisfy(l -> {
            assertThat(l.getAccount().getCode()).isEqualTo(SystemAccounts.INVENTORY); // 제품(1400)
            assertThat(l.getDebit()).isEqualByComparingTo(materialCost);
        });
        assertThat(je.getLines()).anySatisfy(l -> {
            assertThat(l.getAccount().getCode()).isEqualTo(SystemAccounts.RAW_MATERIAL); // 원재료(1410)
            assertThat(l.getCredit()).isEqualByComparingTo(materialCost);
        });
    }

    @Test
    @DisplayName("부품 부족 시 완료 → InsufficientStockException + 전체 롤백(부분 출고 없음)")
    void insufficientStockRollsBack() {
        Item nb = notebook();
        Warehouse wh = hqWarehouse();
        Item lcd = component("15\" LCD 패널");
        Item memory = component("16GB 메모리");

        BigDecimal lcdBefore = onHand(lcd, wh);
        BigDecimal memBefore = onHand(memory, wh);

        // 노트북 600대 → 메모리 1200 > 재고(≈500) → 완료 시 메모리에서 부족
        ProductionOrderResponse po = productionService.createDraft(
                new ProductionOrderCreateRequest(nb.getId(), wh.getId(), new BigDecimal("600"),
                        LocalDate.of(2026, 6, 4), null));
        productionService.release(po.id());

        assertThatThrownBy(() -> productionService.complete(po.id()))
                .isInstanceOf(InsufficientStockException.class);

        // 롤백: LCD·메모리 재고 변동 없음(앞 라인 출고도 원복)
        assertThat(onHand(lcd, wh)).isEqualByComparingTo(lcdBefore);
        assertThat(onHand(memory, wh)).isEqualByComparingTo(memBefore);
    }
}

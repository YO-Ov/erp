package com.hwlee.erp.sd.atp;

import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.pp.order.ProductionOrderRepository;
import com.hwlee.erp.sd.atp.dto.AtpResponse;
import com.hwlee.erp.sd.order.SalesOrderLineRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ATP(약속가능재고) 계산 — 영업이 "지금 이 품목을 몇 개까지 약속할 수 있나" 를 본다.
 *
 * <p>총 재고만 보면 이미 다른 수주에 묶인 물량을 중복 약속하는 사고가 난다. 그래서
 * <pre>ATP = 현재고(전 창고) − 미출하 확정수주(orderQty−shippedQty) + 진행 중 생산예정(PLANNED/RELEASED)</pre>
 * 으로 계산한다. 구매발주(미입고 입고예정)는 이 시스템에 별도 모델이 없고, 영업이 약속하는
 * 완제품은 생산으로 충당되므로 입고예정은 생산예정으로 대표한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtpService {

    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final SalesOrderLineRepository salesOrderLineRepository;
    private final ProductionOrderRepository productionOrderRepository;

    public AtpResponse atp(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + itemId));
        BigDecimal onHand = nz(stockRepository.sumOnHandByItem(itemId));
        BigDecimal committed = nz(salesOrderLineRepository.sumUnshippedCommittedByItem(itemId));
        BigDecimal inboundProduction = nz(productionOrderRepository.sumOpenProductionQtyByProduct(itemId));
        BigDecimal atp = onHand.subtract(committed).add(inboundProduction);
        return new AtpResponse(itemId, item.getCode(), item.getName(),
                onHand, committed, inboundProduction, atp);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

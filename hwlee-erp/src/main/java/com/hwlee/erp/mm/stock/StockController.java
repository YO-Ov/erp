package com.hwlee.erp.mm.stock;

import static com.hwlee.erp.mm.stock.StockSpecifications.itemIdEquals;
import static com.hwlee.erp.mm.stock.StockSpecifications.qtyGreaterThan;
import static com.hwlee.erp.mm.stock.StockSpecifications.warehouseIdEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.mm.stock.dto.StockResponse;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stock 은 조회 전용 API. 변경은 입고/출고/조정만이 수행한다.
 *
 * <p>재고 "조회는 넓게, 변경은 PURCHASING 만" 원칙 — 영업(SALES)도 출하/약속을 위해
 * 재고를 봐야 하므로 조회 권한을 영업까지 넓힌다. (입고·출고 변경 API 는 여전히 PURCHASING 만.)
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','PURCHASING','ADMIN')")
public class StockController {

    private final StockService service;

    @GetMapping("/{id}")
    public StockResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<StockResponse> search(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) BigDecimal qtyGt,
            Pageable pageable
    ) {
        return service.search(
                where(itemIdEquals(itemId))
                        .and(warehouseIdEquals(warehouseId))
                        .and(qtyGreaterThan(qtyGt)),
                pageable
        );
    }
}

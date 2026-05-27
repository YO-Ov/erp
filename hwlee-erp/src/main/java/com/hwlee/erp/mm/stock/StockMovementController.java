package com.hwlee.erp.mm.stock;

import static com.hwlee.erp.mm.stock.StockMovementSpecifications.itemIdEquals;
import static com.hwlee.erp.mm.stock.StockMovementSpecifications.movedFrom;
import static com.hwlee.erp.mm.stock.StockMovementSpecifications.movedTo;
import static com.hwlee.erp.mm.stock.StockMovementSpecifications.reasonEquals;
import static com.hwlee.erp.mm.stock.StockMovementSpecifications.warehouseIdEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.mm.stock.dto.StockMovementResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 재고 이동 원장 조회 전용 API.
 * 입고/출고 트랜잭션이 자동으로 행을 추가하며, 외부에서 직접 등록하는 엔드포인트는 없다.
 */
@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService service;

    @GetMapping("/{id}")
    public StockMovementResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<StockMovementResponse> search(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) MovementReason reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(itemIdEquals(itemId))
                        .and(warehouseIdEquals(warehouseId))
                        .and(reasonEquals(reason))
                        .and(movedFrom(dateFrom))
                        .and(movedTo(dateTo)),
                pageable
        );
    }
}

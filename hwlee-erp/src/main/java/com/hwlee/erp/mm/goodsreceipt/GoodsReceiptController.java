package com.hwlee.erp.mm.goodsreceipt;

import static com.hwlee.erp.mm.goodsreceipt.GoodsReceiptSpecifications.receiptFrom;
import static com.hwlee.erp.mm.goodsreceipt.GoodsReceiptSpecifications.receiptTo;
import static com.hwlee.erp.mm.goodsreceipt.GoodsReceiptSpecifications.statusEquals;
import static com.hwlee.erp.mm.goodsreceipt.GoodsReceiptSpecifications.vendorIdEquals;
import static com.hwlee.erp.mm.goodsreceipt.GoodsReceiptSpecifications.warehouseIdEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptResponse;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
public class GoodsReceiptController {

    private final GoodsReceiptService service;

    @PostMapping
    public ResponseEntity<GoodsReceiptResponse> create(@Valid @RequestBody GoodsReceiptCreateRequest req) {
        GoodsReceiptResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/goods-receipts/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public GoodsReceiptResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<GoodsReceiptResponse> search(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) GoodsReceiptStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(vendorIdEquals(vendorId))
                        .and(warehouseIdEquals(warehouseId))
                        .and(statusEquals(status))
                        .and(receiptFrom(dateFrom))
                        .and(receiptTo(dateTo)),
                pageable
        );
    }

    @PutMapping("/{id}")
    public GoodsReceiptResponse update(@PathVariable Long id,
                                       @Valid @RequestBody GoodsReceiptUpdateRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/post")
    public GoodsReceiptResponse post(@PathVariable Long id) {
        return service.post(id);
    }

    @PostMapping("/{id}/cancel")
    public GoodsReceiptResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}

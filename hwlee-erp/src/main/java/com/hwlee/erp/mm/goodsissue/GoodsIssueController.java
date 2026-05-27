package com.hwlee.erp.mm.goodsissue;

import static com.hwlee.erp.mm.goodsissue.GoodsIssueSpecifications.issueFrom;
import static com.hwlee.erp.mm.goodsissue.GoodsIssueSpecifications.issueTo;
import static com.hwlee.erp.mm.goodsissue.GoodsIssueSpecifications.reasonEquals;
import static com.hwlee.erp.mm.goodsissue.GoodsIssueSpecifications.statusEquals;
import static com.hwlee.erp.mm.goodsissue.GoodsIssueSpecifications.warehouseIdEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueCreateRequest;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueResponse;
import com.hwlee.erp.mm.goodsissue.dto.GoodsIssueUpdateRequest;
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
@RequestMapping("/api/goods-issues")
@RequiredArgsConstructor
public class GoodsIssueController {

    private final GoodsIssueService service;

    @PostMapping
    public ResponseEntity<GoodsIssueResponse> create(@Valid @RequestBody GoodsIssueCreateRequest req) {
        GoodsIssueResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/goods-issues/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public GoodsIssueResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<GoodsIssueResponse> search(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) GoodsIssueStatus status,
            @RequestParam(required = false) GoodsIssueReason reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(warehouseIdEquals(warehouseId))
                        .and(statusEquals(status))
                        .and(reasonEquals(reason))
                        .and(issueFrom(dateFrom))
                        .and(issueTo(dateTo)),
                pageable
        );
    }

    @PutMapping("/{id}")
    public GoodsIssueResponse update(@PathVariable Long id,
                                     @Valid @RequestBody GoodsIssueUpdateRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/post")
    public GoodsIssueResponse post(@PathVariable Long id) {
        return service.post(id);
    }

    @PostMapping("/{id}/cancel")
    public GoodsIssueResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}

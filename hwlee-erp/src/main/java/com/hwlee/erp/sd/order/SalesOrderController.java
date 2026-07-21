package com.hwlee.erp.sd.order;

import static com.hwlee.erp.sd.order.SalesOrderSpecifications.customerIdEquals;
import static com.hwlee.erp.sd.order.SalesOrderSpecifications.orderedFrom;
import static com.hwlee.erp.sd.order.SalesOrderSpecifications.orderedTo;
import static com.hwlee.erp.sd.order.SalesOrderSpecifications.salespersonIdEquals;
import static com.hwlee.erp.sd.order.SalesOrderSpecifications.statusEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.sd.order.dto.CreditStatusResponse;
import com.hwlee.erp.sd.order.dto.SalesOrderCreateRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderResponse;
import com.hwlee.erp.sd.order.dto.SalesOrderSummaryResponse;
import com.hwlee.erp.sd.order.dto.SalesOrderUpdateRequest;
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
@RequestMapping("/api/sales-orders")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','ADMIN')")
public class SalesOrderController {

    private final SalesOrderService service;

    @PostMapping
    public ResponseEntity<SalesOrderResponse> create(@Valid @RequestBody SalesOrderCreateRequest req) {
        SalesOrderResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/sales-orders/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public SalesOrderResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    /** 고객 신용한도 현황 — 수주 화면이 확정 전 "남은 한도" 를 미리 보여주기 위해 호출. */
    @GetMapping("/credit-status")
    public CreditStatusResponse creditStatus(@RequestParam Long customerId) {
        return service.creditStatus(customerId);
    }

    /**
     * 기간별 수주 집계 — "이번 달 수주 합계" 류 질의용. 건수·금액을 서버에서 정확히 합산해 내려준다.
     *
     * <p>'합계' 는 건수/금액 어느 쪽인지 중의적이라 둘 다 담는다.
     * ('이번 달' 고정 집계는 {@code /api/sd/dashboard} 에도 있고, 여기는 임의 기간용.)
     */
    @GetMapping("/summary")
    public SalesOrderSummaryResponse summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return service.summary(dateFrom, dateTo);
    }

    @GetMapping
    public Page<SalesOrderResponse> search(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long salespersonId,
            @RequestParam(required = false) SalesOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(customerIdEquals(customerId))
                        .and(salespersonIdEquals(salespersonId))
                        .and(statusEquals(status))
                        .and(orderedFrom(dateFrom))
                        .and(orderedTo(dateTo)),
                pageable
        );
    }

    @PutMapping("/{id}")
    public SalesOrderResponse update(@PathVariable Long id, @Valid @RequestBody SalesOrderUpdateRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/confirm")
    public SalesOrderResponse confirm(@PathVariable Long id) {
        return service.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public SalesOrderResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    /** 전량 청구된 수주를 CLOSED(거래 종료)로 마감. */
    @PostMapping("/{id}/close")
    public SalesOrderResponse close(@PathVariable Long id) {
        return service.close(id);
    }
}

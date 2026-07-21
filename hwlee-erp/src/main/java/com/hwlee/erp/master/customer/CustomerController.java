package com.hwlee.erp.master.customer;

import static com.hwlee.erp.master.customer.CustomerSpecifications.businessNoEquals;
import static com.hwlee.erp.master.customer.CustomerSpecifications.createdFrom;
import static com.hwlee.erp.master.customer.CustomerSpecifications.createdTo;
import static com.hwlee.erp.master.customer.CustomerSpecifications.nameContains;
import static com.hwlee.erp.master.customer.CustomerSpecifications.statusEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.customer.dto.CustomerCreateRequest;
import com.hwlee.erp.master.customer.dto.CustomerResponse;
import com.hwlee.erp.master.customer.dto.CustomerUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
// master 데이터: 조회는 전 업무 역할, 변경은 ADMIN (메서드 레벨에서 좁힘)
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','PURCHASING','FINANCE','ADMIN')")
public class CustomerController {

    private final CustomerService service;

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','ADMIN')")
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest req) {
        CustomerResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/customers/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/by-code/{code}")
    public CustomerResponse findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    /** 고객 목록. createdFrom·createdTo 를 주면 등록일 기준으로 좁힌다 (예: "이번 달 신규 등록 고객"). */
    @GetMapping
    public Page<CustomerResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) MasterStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            Pageable pageable
    ) {
        return service.search(
                where(nameContains(name)).and(businessNoEquals(businessNo)).and(statusEquals(status))
                        .and(createdFrom(createdFrom)).and(createdTo(createdTo)),
                pageable
        );
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','ADMIN')")
    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest req) {
        return service.update(id, req);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

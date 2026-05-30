package com.hwlee.erp.master.item;

import static com.hwlee.erp.master.item.ItemSpecifications.categoryEquals;
import static com.hwlee.erp.master.item.ItemSpecifications.nameContains;
import static com.hwlee.erp.master.item.ItemSpecifications.statusEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.item.dto.ItemCreateRequest;
import com.hwlee.erp.master.item.dto.ItemResponse;
import com.hwlee.erp.master.item.dto.ItemUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/items")
@RequiredArgsConstructor
// master 데이터: 조회는 전 업무 역할, 변경은 ADMIN (메서드 레벨에서 좁힘)
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SALES','PURCHASING','FINANCE','ADMIN')")
public class ItemController {

    private final ItemService service;

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemCreateRequest req) {
        ItemResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/items/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public ItemResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/by-code/{code}")
    public ItemResponse findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @GetMapping
    public Page<ItemResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) MasterStatus status,
            Pageable pageable
    ) {
        return service.search(
                where(nameContains(name)).and(categoryEquals(category)).and(statusEquals(status)),
                pageable
        );
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemUpdateRequest req) {
        return service.update(id, req);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.hwlee.erp.mm.warehouse;

import com.hwlee.erp.mm.warehouse.dto.WarehouseCreateRequest;
import com.hwlee.erp.mm.warehouse.dto.WarehouseResponse;
import com.hwlee.erp.mm.warehouse.dto.WarehouseUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository repository;
    private final WarehouseMapper mapper;

    @Transactional
    public WarehouseResponse create(WarehouseCreateRequest req) {
        if (repository.existsByCode(req.code())) {
            throw new IllegalStateException("이미 등록된 창고 코드입니다: " + req.code());
        }
        Warehouse warehouse = Warehouse.create(req.code(), req.name(), req.address());
        return mapper.toResponse(repository.save(warehouse));
    }

    public WarehouseResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public WarehouseResponse findByCode(String code) {
        return mapper.toResponse(repository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: code=" + code)));
    }

    public Page<WarehouseResponse> search(Specification<Warehouse> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public WarehouseResponse update(Long id, WarehouseUpdateRequest req) {
        Warehouse warehouse = getOrThrow(id);
        warehouse.update(req.name(), req.address());
        return mapper.toResponse(warehouse);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Warehouse getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: id=" + id));
    }
}

package com.hwlee.erp.mm.stock;

import com.hwlee.erp.mm.stock.dto.StockResponse;
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
public class StockService {

    private final StockRepository repository;
    private final StockMapper mapper;

    public StockResponse findById(Long id) {
        return mapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found: id=" + id)));
    }

    public Page<StockResponse> search(Specification<Stock> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }
}

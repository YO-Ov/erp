package com.hwlee.erp.master.item;

import com.hwlee.erp.common.code.CodeGenerator;
import com.hwlee.erp.master.item.dto.ItemCreateRequest;
import com.hwlee.erp.master.item.dto.ItemResponse;
import com.hwlee.erp.master.item.dto.ItemUpdateRequest;
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
public class ItemService {

    static final String CODE_PREFIX = "ITEM";

    private final ItemRepository repository;
    private final ItemMapper mapper;
    private final CodeGenerator codeGenerator;

    @Transactional
    public ItemResponse create(ItemCreateRequest req) {
        String code = codeGenerator.nextCode(CODE_PREFIX);
        Item item = Item.create(
                code,
                req.name(),
                req.category(),
                req.unit(),
                req.standardCost(),
                req.standardPrice()
        );
        return mapper.toResponse(repository.save(item));
    }

    public ItemResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public ItemResponse findByCode(String code) {
        return mapper.toResponse(
                repository.findByCode(code)
                        .orElseThrow(() -> new EntityNotFoundException("Item not found: code=" + code)));
    }

    public Page<ItemResponse> search(Specification<Item> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest req) {
        Item item = getOrThrow(id);
        item.update(req.name(), req.category(), req.unit(), req.standardCost(), req.standardPrice());
        return mapper.toResponse(item);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Item getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + id));
    }
}

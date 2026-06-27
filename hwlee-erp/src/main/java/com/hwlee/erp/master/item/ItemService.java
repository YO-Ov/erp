package com.hwlee.erp.master.item;

import com.hwlee.erp.common.code.CodeGenerator;
import com.hwlee.erp.common.entity.MasterStatus;
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
    private final ItemCategoryRepository categoryRepository;
    private final ItemMapper mapper;
    private final CodeGenerator codeGenerator;

    @Transactional
    public ItemResponse create(ItemCreateRequest req) {
        requireActiveCategory(req.category());
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
        requireActiveCategory(req.category());
        Item item = getOrThrow(id);
        item.update(req.name(), req.category(), req.unit(), req.standardCost(), req.standardPrice());
        return mapper.toResponse(item);
    }

    /** 품목에 부여할 카테고리 코드가 카테고리 마스터에 존재하고 활성인지 검증한다. */
    private void requireActiveCategory(String categoryCode) {
        if (!categoryRepository.existsByCodeAndStatus(categoryCode, MasterStatus.ACTIVE)) {
            throw new IllegalArgumentException("존재하지 않거나 비활성인 카테고리입니다: " + categoryCode);
        }
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

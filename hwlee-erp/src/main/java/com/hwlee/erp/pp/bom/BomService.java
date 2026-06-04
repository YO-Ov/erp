package com.hwlee.erp.pp.bom;

import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.master.item.ItemType;
import com.hwlee.erp.pp.bom.dto.BomCreateRequest;
import com.hwlee.erp.pp.bom.dto.BomResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BOM(자재 명세서) 서비스 — 완제품의 부품 구성(단일 레벨)을 관리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BomService {

    private final BomRepository bomRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public BomResponse create(BomCreateRequest req) {
        Item product = item(req.productItemId());
        Item component = item(req.componentItemId());

        if (product.getItemType() != ItemType.FINISHED)
            throw new IllegalArgumentException("완제품(FINISHED) 품목만 BOM 의 대상이 될 수 있습니다: " + product.getCode());
        if (component.getItemType() != ItemType.COMPONENT)
            throw new IllegalArgumentException("부품(COMPONENT) 품목만 BOM 자식이 될 수 있습니다: " + component.getCode());
        if (bomRepository.existsByProductIdAndComponentId(product.getId(), component.getId()))
            throw new IllegalArgumentException("이미 등록된 부품입니다. 수량을 바꾸려면 삭제 후 재등록하세요.");

        Bom saved = bomRepository.save(Bom.of(product, component, req.quantity()));
        return toResponse(saved);
    }

    public List<BomResponse> findByProduct(Long productId) {
        return bomRepository.findByProductId(productId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        Bom bom = bomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bom not found: id=" + id));
        bomRepository.delete(bom);
    }

    private Item item(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + id));
    }

    private BomResponse toResponse(Bom b) {
        return new BomResponse(
                b.getId(),
                b.getProduct().getId(), b.getProduct().getCode(), b.getProduct().getName(),
                b.getComponent().getId(), b.getComponent().getCode(), b.getComponent().getName(),
                b.getQuantity());
    }
}

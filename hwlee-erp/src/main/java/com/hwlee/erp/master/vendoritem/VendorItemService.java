package com.hwlee.erp.master.vendoritem;

import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.master.vendor.Vendor;
import com.hwlee.erp.master.vendor.VendorRepository;
import com.hwlee.erp.master.vendoritem.dto.VendorItemCreateRequest;
import com.hwlee.erp.master.vendoritem.dto.VendorItemResponse;
import com.hwlee.erp.master.vendoritem.dto.VendorItemUpdateRequest;
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
public class VendorItemService {

    private final VendorItemRepository repository;
    private final VendorItemMapper mapper;
    private final VendorRepository vendorRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public VendorItemResponse create(VendorItemCreateRequest req) {
        if (repository.existsByVendorIdAndItemId(req.vendorId(), req.itemId())) {
            throw new IllegalStateException("이미 등록된 거래처-품목 조합입니다.");
        }
        Vendor vendor = vendorRepository.findById(req.vendorId())
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: id=" + req.vendorId()));
        Item item = itemRepository.findById(req.itemId())
                .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + req.itemId()));
        VendorItem vi = VendorItem.create(vendor, item, req.supplyPrice(), req.leadTimeDays());
        return mapper.toResponse(repository.save(vi));
    }

    public VendorItemResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public Page<VendorItemResponse> search(Specification<VendorItem> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public VendorItemResponse update(Long id, VendorItemUpdateRequest req) {
        VendorItem vi = getOrThrow(id);
        vi.update(req.supplyPrice(), req.leadTimeDays());
        return mapper.toResponse(vi);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private VendorItem getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("VendorItem not found: id=" + id));
    }
}

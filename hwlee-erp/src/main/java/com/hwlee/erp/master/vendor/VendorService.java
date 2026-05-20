package com.hwlee.erp.master.vendor;

import com.hwlee.erp.common.code.CodeGenerator;
import com.hwlee.erp.master.vendor.dto.VendorCreateRequest;
import com.hwlee.erp.master.vendor.dto.VendorResponse;
import com.hwlee.erp.master.vendor.dto.VendorUpdateRequest;
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
public class VendorService {

    static final String CODE_PREFIX = "VEND";

    private final VendorRepository repository;
    private final VendorMapper mapper;
    private final CodeGenerator codeGenerator;

    @Transactional
    public VendorResponse create(VendorCreateRequest req) {
        if (repository.existsByBusinessNo(req.businessNo())) {
            throw new IllegalStateException("이미 등록된 사업자번호입니다: " + req.businessNo());
        }
        String code = codeGenerator.nextCode(CODE_PREFIX);
        Vendor vendor = Vendor.create(code, req.name(), req.businessNo(), req.address(), req.paymentTerms());
        return mapper.toResponse(repository.save(vendor));
    }

    public VendorResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public VendorResponse findByCode(String code) {
        return mapper.toResponse(repository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: code=" + code)));
    }

    public Page<VendorResponse> search(Specification<Vendor> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public VendorResponse update(Long id, VendorUpdateRequest req) {
        Vendor vendor = getOrThrow(id);
        vendor.update(req.name(), req.address(), req.paymentTerms());
        return mapper.toResponse(vendor);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Vendor getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: id=" + id));
    }
}

package com.hwlee.erp.master.department;

import com.hwlee.erp.master.department.dto.DepartmentCreateRequest;
import com.hwlee.erp.master.department.dto.DepartmentResponse;
import com.hwlee.erp.master.department.dto.DepartmentUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository repository;

    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest req) {
        if (repository.existsByCode(req.code())) {
            throw new IllegalStateException("이미 등록된 부서 코드입니다: " + req.code());
        }
        Department parent = resolveParent(req.parentCode());
        Department department = Department.create(req.code(), req.name(), parent);
        return toResponse(repository.save(department));
    }

    public DepartmentResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public DepartmentResponse findByCode(String code) {
        return toResponse(repository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Department not found: code=" + code)));
    }

    public List<DepartmentResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentUpdateRequest req) {
        Department department = getOrThrow(id);
        Department parent = resolveParent(req.parentCode());
        department.update(req.name(), parent);
        return toResponse(department);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Department getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found: id=" + id));
    }

    private Department resolveParent(String parentCode) {
        if (parentCode == null || parentCode.isBlank()) {
            return null;
        }
        return repository.findByCode(parentCode)
                .orElseThrow(() -> new EntityNotFoundException("Parent department not found: code=" + parentCode));
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(
                d.getId(),
                d.getCode(),
                d.getName(),
                d.getParent() != null ? d.getParent().getCode() : null,
                d.getStatus(),
                d.getCreatedAt(),
                d.getCreatedBy(),
                d.getUpdatedAt(),
                d.getUpdatedBy()
        );
    }
}

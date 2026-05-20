package com.hwlee.erp.master.vendor;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VendorRepository
        extends JpaRepository<Vendor, Long>, JpaSpecificationExecutor<Vendor> {

    Optional<Vendor> findByCode(String code);

    boolean existsByBusinessNo(String businessNo);
}

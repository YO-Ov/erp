package com.hwlee.erp.master.factory;

import com.hwlee.erp.common.entity.MasterStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactoryRepository extends JpaRepository<Factory, Long> {

    Optional<Factory> findByCode(String code);

    List<Factory> findAllByStatusOrderByCodeAsc(MasterStatus status);
}

package com.hwlee.erp.pp.bom;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BomRepository extends JpaRepository<Bom, Long> {

    /** 완제품의 BOM 전체(부품 목록). */
    List<Bom> findByProductId(Long productId);

    boolean existsByProductIdAndComponentId(Long productId, Long componentId);
}

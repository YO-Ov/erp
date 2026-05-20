package com.hwlee.erp.master.item;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ItemRepository
        extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    Optional<Item> findByCode(String code);
}

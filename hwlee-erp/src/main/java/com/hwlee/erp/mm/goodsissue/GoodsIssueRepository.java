package com.hwlee.erp.mm.goodsissue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GoodsIssueRepository
        extends JpaRepository<GoodsIssue, Long>, JpaSpecificationExecutor<GoodsIssue> {
}

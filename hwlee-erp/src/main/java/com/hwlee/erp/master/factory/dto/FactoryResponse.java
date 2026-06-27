package com.hwlee.erp.master.factory.dto;

import com.hwlee.erp.common.entity.MasterStatus;

public record FactoryResponse(
        Long id,
        String code,
        String name,
        String address,
        MasterStatus status
) {}

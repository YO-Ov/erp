package com.hwlee.erp.fi.account.dto;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.fi.account.AccountType;
import com.hwlee.erp.fi.account.NormalSide;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String code,
        String name,
        AccountType type,
        NormalSide normalSide,
        String parentCode,
        boolean postable,
        MasterStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {}

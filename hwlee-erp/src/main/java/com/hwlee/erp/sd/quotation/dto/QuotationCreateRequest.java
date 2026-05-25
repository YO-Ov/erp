package com.hwlee.erp.sd.quotation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record QuotationCreateRequest(
        @NotNull Long customerId,
        @NotNull LocalDate issuedDate,
        LocalDate validUntil,
        @NotEmpty @Valid List<QuotationLineRequest> lines
) {}

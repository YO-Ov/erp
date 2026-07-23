package com.hwlee.erp.master.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 고객 담당자 등록/수정 요청. 등록·수정 공용.
 * primary=true 면 그 고객의 대표 담당자로 지정되고, 기존 대표는 자동 해제된다(서버가 보장).
 */
public record CustomerContactRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String position,
        @Size(max = 30) String phone,
        @Email @Size(max = 200) String email,
        boolean primary
) {}

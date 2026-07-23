package com.hwlee.erp.master.customer.dto;

public record CustomerContactResponse(
        Long id,
        String name,
        String position,
        String phone,
        String email,
        boolean primary
) {}

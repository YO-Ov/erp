package com.hwlee.erp.master.vendoritem;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 거래처 취급품목 마스터 화면 — 구매가 "어느 거래처가 무슨 품목을 공급하는지"를 관리한다.
 */
@Controller
@PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
public class VendorItemViewController {

    @GetMapping("/master/vendor-items")
    public String list() {
        return "master/vendoritem/list";
    }
}

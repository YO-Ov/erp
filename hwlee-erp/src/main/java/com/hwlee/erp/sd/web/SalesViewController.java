package com.hwlee.erp.sd.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * SD(영업) 화면 진입점 (Thymeleaf).
 *
 * <p>화면은 껍데기 HTML 만 내려주고, 실제 데이터는 각 페이지의 JS 가
 * REST API(/api/quotations 등)를 호출해 채운다. 여기서는 라우팅과 권한만 담당한다.
 * 인가는 REST 컨트롤러와 동일하게 SALES/ADMIN.
 */
@Controller
@PreAuthorize("hasAnyRole('SALES','ADMIN')")
public class SalesViewController {

    // ── 견적(Quotation) ─────────────────────────────────────────
    @GetMapping("/sd/quotations")
    public String quotationList() {
        return "sd/quotation/list";
    }

    @GetMapping("/sd/quotations/new")
    public String quotationNew() {
        // id 속성을 넣지 않으면 템플릿의 ${id} 는 null → 신규 모드
        return "sd/quotation/form";
    }

    @GetMapping("/sd/quotations/{id}/edit")
    public String quotationEdit(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "sd/quotation/form";
    }

    @GetMapping("/sd/quotations/{id}")
    public String quotationDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "sd/quotation/detail";
    }

    // ── 수주(SalesOrder) ────────────────────────────────────────
    @GetMapping("/sd/sales-orders")
    public String orderList() {
        return "sd/order/list";
    }

    @GetMapping("/sd/sales-orders/new")
    public String orderNew() {
        return "sd/order/form";
    }

    @GetMapping("/sd/sales-orders/{id}/edit")
    public String orderEdit(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "sd/order/form";
    }

    @GetMapping("/sd/sales-orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "sd/order/detail";
    }

    // ── 출하(Delivery) ──────────────────────────────────────────
    @GetMapping("/sd/deliveries")
    public String deliveryList() {
        return "sd/delivery/list";
    }

    @GetMapping("/sd/deliveries/new")
    public String deliveryNew() {
        return "sd/delivery/form";
    }

    @GetMapping("/sd/deliveries/{id}")
    public String deliveryDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "sd/delivery/detail";
    }

    // ── 인보이스(Invoice) ───────────────────────────────────────
    @GetMapping("/sd/invoices")
    public String invoiceList() {
        return "sd/invoice/list";
    }

    @GetMapping("/sd/invoices/new")
    public String invoiceNew() {
        return "sd/invoice/form";
    }

    @GetMapping("/sd/invoices/{id}")
    public String invoiceDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "sd/invoice/detail";
    }
}

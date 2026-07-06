package com.hwlee.erp.mm.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * MM(자재) 화면 진입점 (Thymeleaf).
 *
 * <p>SD 와 동일하게 화면은 껍데기 HTML 만 내려주고, 실제 데이터는 각 페이지의 JS 가
 * REST API(/api/stocks, /api/goods-receipts 등)를 호출해 채운다. 여기서는 라우팅과 권한만 담당한다.
 * 인가는 REST 컨트롤러와 동일하게 PURCHASING/ADMIN.
 */
@Controller
@PreAuthorize("hasAnyRole('PURCHASING','ADMIN')")
public class MaterialsViewController {

    // ── 재고(Stock) — 조회 전용 ─────────────────────────────────
    @GetMapping("/mm/stocks")
    public String stockList() {
        return "mm/stock/list";
    }

    @GetMapping("/mm/stocks/{id}")
    public String stockDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "mm/stock/detail";
    }

    // ── 입고(GoodsReceipt) ──────────────────────────────────────
    @GetMapping("/mm/goods-receipts")
    public String receiptList() {
        return "mm/receipt/list";
    }

    @GetMapping("/mm/goods-receipts/new")
    public String receiptNew() {
        return "mm/receipt/form";
    }

    @GetMapping("/mm/goods-receipts/{id}/edit")
    public String receiptEdit(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "mm/receipt/form";
    }

    @GetMapping("/mm/goods-receipts/{id}")
    public String receiptDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "mm/receipt/detail";
    }

    // ── 구매발주(PurchaseOrder) ─────────────────────────────────
    @GetMapping("/mm/purchase-orders")
    public String purchaseOrderList() {
        return "mm/purchaseorder/list";
    }

    @GetMapping("/mm/purchase-orders/new")
    public String purchaseOrderNew() {
        return "mm/purchaseorder/form";
    }

    @GetMapping("/mm/purchase-orders/{id}/edit")
    public String purchaseOrderEdit(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "mm/purchaseorder/form";
    }

    @GetMapping("/mm/purchase-orders/{id}")
    public String purchaseOrderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "mm/purchaseorder/detail";
    }

    // ── 출고(GoodsIssue) ────────────────────────────────────────
    @GetMapping("/mm/goods-issues")
    public String issueList() {
        return "mm/issue/list";
    }

    @GetMapping("/mm/goods-issues/new")
    public String issueNew() {
        return "mm/issue/form";
    }

    @GetMapping("/mm/goods-issues/{id}/edit")
    public String issueEdit(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "mm/issue/form";
    }

    @GetMapping("/mm/goods-issues/{id}")
    public String issueDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "mm/issue/detail";
    }

    // ── 창고(Warehouse) ─────────────────────────────────────────
    @GetMapping("/mm/warehouses")
    public String warehouseList() {
        return "mm/warehouse/list";
    }

    @GetMapping("/mm/warehouses/new")
    public String warehouseNew() {
        return "mm/warehouse/form";
    }

    @GetMapping("/mm/warehouses/{id}/edit")
    public String warehouseEdit(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "mm/warehouse/form";
    }

    @GetMapping("/mm/warehouses/{id}")
    public String warehouseDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "mm/warehouse/detail";
    }
}

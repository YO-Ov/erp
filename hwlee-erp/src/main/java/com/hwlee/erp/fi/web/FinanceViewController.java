package com.hwlee.erp.fi.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * FI(회계) 화면 진입점 (Thymeleaf).
 *
 * <p>SD/MM 과 동일하게 화면은 껍데기 HTML 만 내려주고, 실제 데이터는 각 페이지의 JS 가
 * REST API(/api/accounts, /api/journal-entries, /api/payments)를 호출해 채운다.
 * 여기서는 라우팅과 권한만 담당한다. 인가는 REST 컨트롤러와 동일하게 FINANCE/ADMIN.
 */
@Controller
@PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
public class FinanceViewController {

    // ── 계정과목(Account) — 마스터 CRUD ─────────────────────────
    @GetMapping("/fi/accounts")
    public String accountList() {
        return "fi/account/list";
    }

    @GetMapping("/fi/accounts/new")
    public String accountNew() {
        return "fi/account/form";
    }

    @GetMapping("/fi/accounts/{id}/edit")
    public String accountEdit(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "fi/account/form";
    }

    @GetMapping("/fi/accounts/{id}")
    public String accountDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "fi/account/detail";
    }

    // ── 전표(JournalEntry) ──────────────────────────────────────
    //   수동 전표는 생성 즉시 전기(POSTED)된다. 수정 없음, POSTED→취소만.
    @GetMapping("/fi/journal-entries")
    public String journalList() {
        return "fi/journal/list";
    }

    @GetMapping("/fi/journal-entries/new")
    public String journalNew() {
        return "fi/journal/form";
    }

    @GetMapping("/fi/journal-entries/{id}")
    public String journalDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "fi/journal/detail";
    }

    // ── 입출금(Payment) ─────────────────────────────────────────
    //   생성 즉시 전기(POSTED). 수정/취소 없음 — 상세는 읽기 전용.
    @GetMapping("/fi/payments")
    public String paymentList() {
        return "fi/payment/list";
    }

    @GetMapping("/fi/payments/new")
    public String paymentNew() {
        return "fi/payment/form";
    }

    @GetMapping("/fi/payments/{id}")
    public String paymentDetail(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        return "fi/payment/detail";
    }
}

package com.hwlee.erp.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hwlee.erp.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 인가(RBAC) — 역할에 따라 모듈 접근이 허용/거부되는지.
 * "영업팀은 SD만, 재무팀은 FI만" 규칙을 코드로 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "SALES")
    void sales_can_read_sales_orders() throws Exception {
        mockMvc.perform(get("/api/sales-orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES")
    void sales_can_read_sales_order_summary() throws Exception {
        // 리터럴 경로 /summary 가 /{id} 로 잘못 매칭되면 id 변환 실패로 400 이 난다 → 200 이어야 정상.
        mockMvc.perform(get("/api/sales-orders/summary")
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES")
    void sales_can_read_quotation_summary() throws Exception {
        // 리터럴 경로 /summary 가 /{id} 로 잘못 매칭되면 id 변환 실패로 400 이 난다 → 200 이어야 정상.
        mockMvc.perform(get("/api/quotations/summary")
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "someone@hyunwoo.com", roles = "SALES")
    void outbox_accepts_date_range() throws Exception {
        // 상신함 기간 파라미터가 바인딩되고 쿼리가 실행되는지 (파생 쿼리명 오타는 여기서 드러난다).
        mockMvc.perform(get("/api/approvals/outbox")
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES")
    void customers_accept_created_date_range() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .param("createdFrom", "2026-07-01")
                        .param("createdTo", "2026-07-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void finance_cannot_read_sales_order_summary_returns_403() throws Exception {
        mockMvc.perform(get("/api/sales-orders/summary")
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALES")
    void sales_cannot_access_finance_module_returns_403() throws Exception {
        mockMvc.perform(get("/api/journal-entries"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void finance_can_access_finance_module() throws Exception {
        mockMvc.perform(get("/api/journal-entries"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES")
    void sales_can_read_master_but_cannot_write_returns_403() throws Exception {
        // master 조회는 전 업무 역할 허용
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk());
        // 변경은 ADMIN 만 — SALES 는 403
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/customers/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_can_access_everything() throws Exception {
        mockMvc.perform(get("/api/journal-entries")).andExpect(status().isOk());
        mockMvc.perform(get("/api/sales-orders")).andExpect(status().isOk());
        mockMvc.perform(get("/api/stocks")).andExpect(status().isOk());
    }
}

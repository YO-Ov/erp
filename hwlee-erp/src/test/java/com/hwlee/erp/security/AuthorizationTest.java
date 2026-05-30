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

package com.hwlee.erp.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.master.customer.PaymentTerms;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 감사 층위① — auditorProvider 교체(복선 회수)로 created_by/updated_by 가
 * "system" 대신 인증 사용자로 박히는지. 기존 SD/MM/FI 코드는 한 줄도 안 고친 채 동작.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuditorAwareTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @WithMockUser(username = "kim@hwlee-erp.example", roles = "ADMIN")
    void created_by_is_authenticated_user() {
        Customer c = Customer.create("CUST-AUD-2", "감사자테스트", "111-00-22200",
                "서울", new BigDecimal("100000"), PaymentTerms.NET30);
        Customer saved = customerRepository.saveAndFlush(c);

        assertThat(saved.getCreatedBy()).isEqualTo("kim@hwlee-erp.example");
        assertThat(saved.getUpdatedBy()).isEqualTo("kim@hwlee-erp.example");
    }
}

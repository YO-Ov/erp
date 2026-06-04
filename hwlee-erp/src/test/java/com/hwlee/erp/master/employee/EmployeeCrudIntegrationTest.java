package com.hwlee.erp.master.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.employee.dto.EmployeeCreateRequest;
import com.hwlee.erp.master.employee.dto.EmployeeResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EmployeeCrudIntegrationTest {

    @Autowired
    EmployeeService service;

    @Test
    @DisplayName("직원_생성시_EMP_코드가_발급되고_부서가_연결된다")
    void 직원_생성시_EMP_코드_발급_및_부서_연결() {
        EmployeeResponse e = service.create(new EmployeeCreateRequest(
                "신입사원-" + System.nanoTime(),
                "new" + System.nanoTime() + "@hyunwoo.com",
                "DEPT-SALES",
                LocalDate.of(2026, 5, 20)
        ));

        assertThat(e.code()).matches("EMP-\\d{4}-\\d{4}");
        assertThat(e.departmentCode()).isEqualTo("DEPT-SALES");
        assertThat(e.departmentName()).isEqualTo("영업팀");
    }

    @Test
    @DisplayName("이메일_중복_등록은_거부된다")
    void 이메일_중복_등록은_거부된다() {
        String email = "dup" + System.nanoTime() + "@hyunwoo.com";
        service.create(new EmployeeCreateRequest("A", email, "DEPT-SALES", LocalDate.of(2026, 1, 1)));
        assertThatThrownBy(() ->
                service.create(new EmployeeCreateRequest("B", email, "DEPT-FINANCE", LocalDate.of(2026, 1, 1)))
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("존재하지_않는_부서_코드는_거부된다")
    void 존재하지_않는_부서_코드는_거부() {
        assertThatThrownBy(() ->
                service.create(new EmployeeCreateRequest(
                        "X", "x" + System.nanoTime() + "@hyunwoo.com",
                        "DEPT-NOWHERE", LocalDate.of(2026, 1, 1)))
        ).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
}

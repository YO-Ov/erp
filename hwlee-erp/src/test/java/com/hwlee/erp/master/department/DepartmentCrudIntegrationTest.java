package com.hwlee.erp.master.department;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.department.dto.DepartmentCreateRequest;
import com.hwlee.erp.master.department.dto.DepartmentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DepartmentCrudIntegrationTest {

    @Autowired
    DepartmentService service;

    @Test
    @DisplayName("자식_부서는_부모_부서_코드로_연결된다")
    void 자식_부서는_부모_부서_코드로_연결된다() {
        // V8 시드로 이미 DEPT-HQ 와 5개 하위 부서가 존재한다.
        DepartmentResponse sales = service.findByCode("DEPT-SALES");

        assertThat(sales.parentCode()).isEqualTo("DEPT-HQ");
        assertThat(sales.name()).isEqualTo("영업팀");
    }

    @Test
    @DisplayName("부서_코드_중복_등록은_거부된다")
    void 부서_코드_중복_등록은_거부된다() {
        // V8 에서 DEPT-SALES 가 이미 등록됨
        assertThatThrownBy(() ->
                service.create(new DepartmentCreateRequest("DEPT-SALES", "영업2팀", "DEPT-HQ"))
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("존재하지_않는_부모_코드로_생성하면_예외가_발생한다")
    void 존재하지_않는_부모_코드로_생성하면_예외() {
        assertThatThrownBy(() ->
                service.create(new DepartmentCreateRequest("DEPT-RND", "연구소", "DEPT-NOWHERE"))
        ).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
}

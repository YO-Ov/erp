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
        // V8 시드 후 STEP4(V52)에서 조직이 본부-팀 트리로 재편됨:
        // DEPT-SALES 는 '국내영업1팀'으로 개명되고 부모가 영업본부(DEPT-SALESHQ)로 이동.
        DepartmentResponse sales = service.findByCode("DEPT-SALES");

        assertThat(sales.parentCode()).isEqualTo("DEPT-SALESHQ");
        assertThat(sales.name()).isEqualTo("국내영업1팀");
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
        // (DEPT-RND 는 STEP4 에서 실제 부서로 생성됐으므로, 코드 충돌을 피해 미사용 코드를 쓴다.)
        assertThatThrownBy(() ->
                service.create(new DepartmentCreateRequest("DEPT-NEW-TEST", "연구소", "DEPT-NOWHERE"))
        ).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
}

package com.hwlee.erp.master.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.customer.dto.CustomerCreateRequest;
import com.hwlee.erp.master.customer.dto.CustomerResponse;
import com.hwlee.erp.master.customer.dto.CustomerUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/**
 * Customer 도메인의 핵심 업무 규칙을 통합 테스트로 검증한다.
 * 테스트 이름은 "이 시스템이 보장하는 업무 규칙" 의 살아있는 문서 역할.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CustomerCrudIntegrationTest {

    @Autowired
    CustomerService service;

    @Autowired
    CustomerRepository repository;

    @Test
    @DisplayName("신규 고객을 생성하면 CUST-YYYY-NNNN 형식의 코드가 자동 발급된다")
    void 신규_고객을_생성하면_코드가_자동_발급된다() {
        // given
        CustomerCreateRequest req = new CustomerCreateRequest(
                "신원전자",
                uniqueBusinessNo(),
                "서울시 강남구",
                PaymentTerms.NET30
        );

        // when
        CustomerResponse created = service.create(req);

        // then
        assertThat(created.code()).matches("CUST-\\d{4}-\\d{4}");
        assertThat(created.name()).isEqualTo("신원전자");
        assertThat(created.status().name()).isEqualTo("ACTIVE");
        assertThat(created.createdBy()).isEqualTo("system");
        assertThat(created.updatedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("사업자번호가_중복되면_생성이_거부된다")
    void 사업자번호가_중복되면_생성이_거부된다() {
        // given — 첫 등록
        String dup = uniqueBusinessNo();
        service.create(new CustomerCreateRequest("회사A", dup, null,
                PaymentTerms.NET30));

        // when / then — 같은 사업자번호로 재등록 시도
        assertThatThrownBy(() ->
                service.create(new CustomerCreateRequest("회사B", dup, null,
                        PaymentTerms.NET30))
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("이미 등록된 사업자번호");
    }

    @Test
    @DisplayName("삭제된_고객은_일반_조회에서_보이지_않는다")
    void 삭제된_고객은_일반_조회에서_보이지_않는다() {
        // given
        CustomerResponse created = service.create(new CustomerCreateRequest(
                "삭제대상", uniqueBusinessNo(), null,
                PaymentTerms.COD));
        Long id = created.id();

        // when — Soft Delete
        service.delete(id);

        // then — findById 는 EntityNotFoundException (조회 시점에 deleted_at IS NULL 필터 자동 적용)
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(EntityNotFoundException.class);

        // 페이징 목록에도 안 잡힌다
        boolean stillVisible = service.search(null, PageRequest.of(0, 1000))
                .stream()
                .anyMatch(c -> c.id().equals(id));
        assertThat(stillVisible).isFalse();
    }

    @Test
    @DisplayName("고객을_수정하면_updated_at_과_업데이트된_값이_반영된다")
    void 고객을_수정하면_업데이트된_값이_반영된다() {
        // given
        CustomerResponse created = service.create(new CustomerCreateRequest(
                "수정대상", uniqueBusinessNo(), "옛 주소",
                PaymentTerms.NET30));

        // when — 영업 수정(이름/주소/결제조건). 한도는 수정 DTO에 없으므로(여신=재무 권한 분리)
        CustomerResponse updated = service.update(created.id(),
                new CustomerUpdateRequest("수정대상-개명", "새 주소", PaymentTerms.NET60));

        // then
        assertThat(updated.name()).isEqualTo("수정대상-개명");
        assertThat(updated.address()).isEqualTo("새 주소");
        assertThat(updated.paymentTerms()).isEqualTo(PaymentTerms.NET60);
        // business_no 는 수정 불가 정책 — Request DTO 에 필드 자체가 없음
        assertThat(updated.businessNo()).isEqualTo(created.businessNo());

        // 한도 변경은 별도 도메인 메서드(changeCreditLimit)로만 — 변경이 조회에 반영된다
        repository.findById(created.id()).orElseThrow().changeCreditLimit(new BigDecimal("5000000.00"));
        repository.flush();
        assertThat(service.findById(created.id()).creditLimit())
                .isEqualByComparingTo(new BigDecimal("5000000.00"));
    }

    @Test
    @DisplayName("코드로_조회할_수_있다")
    void 코드로_조회할_수_있다() {
        CustomerResponse created = service.create(new CustomerCreateRequest(
                "조회대상", uniqueBusinessNo(), null,
                PaymentTerms.COD));

        CustomerResponse found = service.findByCode(created.code());

        assertThat(found.id()).isEqualTo(created.id());
    }

    private static int counter = 1;

    private static synchronized String uniqueBusinessNo() {
        // 사업자번호 형식: 3-2-5
        return String.format("%03d-%02d-%05d", 100 + counter, counter % 100, counter++ + 10000);
    }
}

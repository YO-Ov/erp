package com.hwlee.erp.master.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.vendor.dto.VendorCreateRequest;
import com.hwlee.erp.master.vendor.dto.VendorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class VendorCrudIntegrationTest {

    @Autowired
    VendorService service;

    @Test
    @DisplayName("거래처_생성시_VEND_코드가_발급된다")
    void 거래처_생성시_VEND_코드가_발급된다() {
        VendorResponse v = service.create(new VendorCreateRequest(
                "삼우전자부품", uniqueBusinessNo(), "수원시", PaymentTerms.NET30));
        assertThat(v.code()).matches("VEND-\\d{4}-\\d{4}");
    }

    @Test
    @DisplayName("거래처_사업자번호_중복_등록은_거부된다")
    void 거래처_사업자번호_중복_등록은_거부된다() {
        String dup = uniqueBusinessNo();
        service.create(new VendorCreateRequest("A부품", dup, null, PaymentTerms.NET30));
        assertThatThrownBy(() ->
                service.create(new VendorCreateRequest("B부품", dup, null, PaymentTerms.NET30))
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("삭제된_거래처는_조회되지_않는다")
    void 삭제된_거래처는_조회되지_않는다() {
        VendorResponse v = service.create(new VendorCreateRequest(
                "단종부품", uniqueBusinessNo(), null, PaymentTerms.NET30));
        service.delete(v.id());
        assertThatThrownBy(() -> service.findById(v.id()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private static int counter = 1;

    private static synchronized String uniqueBusinessNo() {
        return String.format("%03d-%02d-%05d", 200 + counter, counter % 100, counter++ + 20000);
    }
}

package com.hwlee.erp.master.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.master.item.dto.ItemCreateRequest;
import com.hwlee.erp.master.item.dto.ItemResponse;
import com.hwlee.erp.master.item.dto.ItemUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ItemCrudIntegrationTest {

    @Autowired
    ItemService service;

    @Test
    @DisplayName("상품_생성시_ITEM_YYYY_NNNN_코드가_발급된다")
    void 상품_생성시_ITEM_코드가_발급된다() {
        ItemResponse created = service.create(new ItemCreateRequest(
                "노트북 15인치",
                ItemCategory.NOTEBOOK,
                ItemUnit.EA,
                new BigDecimal("800000.00"),
                new BigDecimal("1200000.00")
        ));

        assertThat(created.code()).matches("ITEM-\\d{4}-\\d{4}");
        assertThat(created.category()).isEqualTo(ItemCategory.NOTEBOOK);
        assertThat(created.unit()).isEqualTo(ItemUnit.EA);
    }

    @Test
    @DisplayName("표준_판매가가_표준_원가보다_낮아도_등록을_막지_않는다")
    void 표준_판매가가_원가보다_낮아도_등록을_막지_않는다() {
        // Phase 1 정책: 손해 판매 가능성을 인정하고 검증하지 않는다.
        ItemResponse created = service.create(new ItemCreateRequest(
                "행사용 노트북",
                ItemCategory.NOTEBOOK,
                ItemUnit.EA,
                new BigDecimal("1000000.00"),
                new BigDecimal("900000.00")
        ));

        assertThat(created.standardPrice()).isEqualByComparingTo(new BigDecimal("900000.00"));
        assertThat(created.standardCost()).isEqualByComparingTo(new BigDecimal("1000000.00"));
    }

    @Test
    @DisplayName("삭제된_상품은_일반_조회에서_안_보인다")
    void 삭제된_상품은_조회되지_않는다() {
        ItemResponse created = service.create(new ItemCreateRequest(
                "단종 예정",
                ItemCategory.MONITOR,
                ItemUnit.EA,
                new BigDecimal("100000.00"),
                new BigDecimal("150000.00")
        ));

        service.delete(created.id());

        assertThatThrownBy(() -> service.findById(created.id()))
                .isInstanceOf(EntityNotFoundException.class);
    }
}

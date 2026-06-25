package com.hwlee.erp.sd.quotation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 견적 목록에서 여러 건을 한 번에 처리하는 일괄 작업 요청.
 *
 * <p>상태 머신상 부적합한 건은 서버가 건너뛰고 {@link QuotationBulkResponse#failed()} 로 사유를
 * 돌려준다(부분 성공 허용).
 */
public record QuotationBulkRequest(
        @NotEmpty(message = "대상을 1건 이상 선택하세요.") List<Long> ids,
        @NotNull(message = "action 은 필수입니다.") Action action
) {
    public enum Action { SEND, CANCEL }
}

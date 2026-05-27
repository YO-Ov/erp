package com.hwlee.erp.mm.stock;

import java.math.BigDecimal;

/**
 * 가용 재고가 요청 수량보다 적을 때 발생.
 *
 * <p>{@code GlobalExceptionHandler} 가 별도로 처리해 409 + {@code code=INSUFFICIENT_STOCK}
 * + {@code available, requested} 속성을 응답으로 내보낸다.
 */
public class InsufficientStockException extends RuntimeException {

    private final Long itemId;
    private final Long warehouseId;
    private final BigDecimal available;
    private final BigDecimal requested;

    public InsufficientStockException(Long itemId, Long warehouseId,
                                      BigDecimal available, BigDecimal requested) {
        super("재고 부족: itemId=" + itemId + ", warehouseId=" + warehouseId
                + ", available=" + available + ", requested=" + requested);
        this.itemId = itemId;
        this.warehouseId = warehouseId;
        this.available = available;
        this.requested = requested;
    }

    public Long getItemId() { return itemId; }
    public Long getWarehouseId() { return warehouseId; }
    public BigDecimal getAvailable() { return available; }
    public BigDecimal getRequested() { return requested; }
}

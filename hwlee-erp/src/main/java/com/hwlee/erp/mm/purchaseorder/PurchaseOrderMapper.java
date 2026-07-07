package com.hwlee.erp.mm.purchaseorder;

import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderLineResponse;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderResponse;
import java.math.BigDecimal;
import java.util.Map;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PurchaseOrderMapper {

    /**
     * 발주 응답 매핑. {@code receivedByItem} 는 품목별 입고 누계(POSTED 기준) — 라인의
     * 입고수량/미납수량 계산에 쓰인다. 라인 집계가 필요 없는 목록에서는 빈 맵을 넘긴다.
     */
    @Mapping(source = "vendor.id",      target = "vendorId")
    @Mapping(source = "vendor.code",    target = "vendorCode")
    @Mapping(source = "vendor.name",    target = "vendorName")
    @Mapping(source = "warehouse.id",   target = "warehouseId")
    @Mapping(source = "warehouse.code", target = "warehouseCode")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    @Mapping(expression = "java(entity.totalAmount())", target = "totalAmount")
    PurchaseOrderResponse toResponse(PurchaseOrder entity, @Context Map<Long, BigDecimal> receivedByItem);

    @Mapping(source = "item.id",   target = "itemId")
    @Mapping(source = "item.code", target = "itemCode")
    @Mapping(source = "item.name", target = "itemName")
    @Mapping(expression = "java(receivedOf(line, receivedByItem))",     target = "receivedQuantity")
    @Mapping(expression = "java(openOf(line, receivedByItem))",         target = "openQuantity")
    PurchaseOrderLineResponse toResponse(PurchaseOrderLine line, @Context Map<Long, BigDecimal> receivedByItem);

    /** 이 라인 품목의 입고 누계(집계에 없으면 0). */
    default BigDecimal receivedOf(PurchaseOrderLine line, Map<Long, BigDecimal> receivedByItem) {
        return receivedByItem.getOrDefault(line.getItem().getId(), BigDecimal.ZERO);
    }

    /** 미납 수량 = 발주수량 − 입고누계(하한 0). */
    default BigDecimal openOf(PurchaseOrderLine line, Map<Long, BigDecimal> receivedByItem) {
        return line.getQuantity().subtract(receivedOf(line, receivedByItem)).max(BigDecimal.ZERO);
    }
}

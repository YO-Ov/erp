package com.hwlee.erp.mm.goodsreceipt;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsReceiptRepository
        extends JpaRepository<GoodsReceipt, Long>, JpaSpecificationExecutor<GoodsReceipt> {

    /**
     * 발주(PO) 대비 입고 누계 — 그 발주를 참조하는 <b>POSTED</b> 입고들의 품목별 수량 합.
     * DRAFT/CANCELLED 입고는 제외한다. 발주 라인의 입고 진행·전량 입고 판정에 쓰인다.
     */
    @Query("""
            select l.item.id as itemId, sum(l.quantity) as quantity
            from GoodsReceiptLine l
            where l.goodsReceipt.purchaseOrder.id = :purchaseOrderId
              and l.goodsReceipt.status = com.hwlee.erp.mm.goodsreceipt.GoodsReceiptStatus.POSTED
            group by l.item.id
            """)
    List<ReceivedQtyRow> sumReceivedQuantityByPurchaseOrder(@Param("purchaseOrderId") Long purchaseOrderId);

    /** 품목별 입고 누계 프로젝션. */
    interface ReceivedQtyRow {
        Long getItemId();
        BigDecimal getQuantity();
    }
}

package com.hwlee.erp.mm.purchaseorder;

import com.hwlee.erp.approval.ApprovalDocType;
import com.hwlee.erp.approval.ApprovalService;
import com.hwlee.erp.approval.dto.ApprovalResponse;
import com.hwlee.erp.approval.dto.ApprovalSubmitCommand;
import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.master.vendor.Vendor;
import com.hwlee.erp.master.vendor.VendorRepository;
import com.hwlee.erp.master.vendoritem.VendorItemRepository;
import com.hwlee.erp.mm.goodsreceipt.GoodsReceiptRepository;
import com.hwlee.erp.mm.goodsreceipt.GoodsReceiptRepository.ReceivedQtyRow;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderCreateRequest;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderLineRequest;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderResponse;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderUpdateRequest;
import com.hwlee.erp.mm.warehouse.Warehouse;
import com.hwlee.erp.mm.warehouse.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구매발주(PO) 서비스 — 헤더/라인 관리 + 전자결재 상신 + 승인 콜백(확정).
 *
 * <p>결재 없이는 발주가 확정되지 않는다(지출 통제). 흐름: {@link #create}(DRAFT) →
 * {@link #submitForApproval}(전자결재 상신) → 최종 승인 시 {@code PurchaseOrderApprovalListener}
 * 가 {@link #confirmByApproval} 을 호출해 CONFIRMED 로 전이. 라인은 거래처 취급품목(VendorItem)
 * ACTIVE 인 품목만 허용(입고와 동일 게이트).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository repository;
    private final PurchaseOrderMapper mapper;
    private final VendorRepository vendorRepository;
    private final VendorItemRepository vendorItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final TransactionNumberGenerator numberGenerator;
    private final ApprovalService approvalService;

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderCreateRequest req) {
        Vendor vendor = vendorRepository.findById(req.vendorId())
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: id=" + req.vendorId()));
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: id=" + req.warehouseId()));
        String number = numberGenerator.nextPurchaseOrderNumber(req.orderDate());
        PurchaseOrder po = PurchaseOrder.draft(
                number, vendor, warehouse, req.orderDate(), req.expectedDate(), req.remark());
        addLines(po, req.lines());
        // 신규 발주는 입고 이력이 없으므로 집계 불필요.
        return mapper.toResponse(repository.save(po), Map.of());
    }

    public PurchaseOrderResponse findById(Long id) {
        PurchaseOrder po = getOrThrow(id);
        return mapper.toResponse(po, receivedByItem(po.getId()));
    }

    public Page<PurchaseOrderResponse> search(Specification<PurchaseOrder> spec, Pageable pageable) {
        // 목록은 라인 입고 집계를 쓰지 않으므로 PO 마다 집계 쿼리를 돌리지 않는다(N+1 방지).
        return repository.findAll(spec, pageable).map(po -> mapper.toResponse(po, Map.of()));
    }

    @Transactional
    public PurchaseOrderResponse update(Long id, PurchaseOrderUpdateRequest req) {
        PurchaseOrder po = getOrThrow(id);
        Vendor vendor = vendorRepository.findById(req.vendorId())
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: id=" + req.vendorId()));
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: id=" + req.warehouseId()));
        po.updateHeader(vendor, warehouse, req.orderDate(), req.expectedDate(), req.remark());
        po.clearLines();
        addLines(po, req.lines());
        // 수정은 DRAFT 상태에서만 가능(입고 이전)하므로 집계 불필요.
        return mapper.toResponse(po, Map.of());
    }

    /**
     * 발주 결재 상신 — DRAFT 발주를 전자결재에 올린다.
     * 최종 승인되면 {@code PurchaseOrderApprovalListener} 가 {@link #confirmByApproval} 로 CONFIRMED 전이.
     */
    @Transactional
    public ApprovalResponse submitForApproval(Long id, String requester) {
        PurchaseOrder po = getOrThrow(id);
        if (po.getStatus() != PurchaseOrderStatus.DRAFT)
            throw new IllegalStateException("작성 중(DRAFT) 발주만 결재 상신할 수 있습니다. 현재: " + po.getStatus());
        if (po.getLines().isEmpty())
            throw new IllegalStateException("품목이 없는 발주는 상신할 수 없습니다.");
        return approvalService.submit(new ApprovalSubmitCommand(
                ApprovalDocType.PURCHASE_ORDER, po.getId(), po.getNumber(),
                "구매발주 · " + po.getVendor().getName() + " (" + po.getNumber() + ")",
                po.totalAmount(), requester));
    }

    /** 결재 최종 승인 콜백 — DRAFT → CONFIRMED(발주 확정). */
    @Transactional
    public void confirmByApproval(Long id) {
        PurchaseOrder po = getOrThrow(id);
        po.confirm();
    }

    /**
     * 발주 대비 입고 진행을 재집계해 상태를 동기화한다(입고 확정/취소 콜백에서 호출).
     * 전량 입고면 CONFIRMED→RECEIVED, 입고 취소로 미달되면 RECEIVED→CONFIRMED.
     */
    @Transactional
    public void syncReceiptStatus(Long id) {
        PurchaseOrder po = getOrThrow(id);
        Map<Long, BigDecimal> received = receivedByItem(po.getId());
        po.syncReceiptStatus(isFullyReceived(po, received));
    }

    @Transactional
    public PurchaseOrderResponse close(Long id) {
        PurchaseOrder po = getOrThrow(id);
        po.close();
        return mapper.toResponse(po, receivedByItem(po.getId()));
    }

    @Transactional
    public PurchaseOrderResponse cancel(Long id) {
        PurchaseOrder po = getOrThrow(id);
        po.cancel();
        return mapper.toResponse(po, receivedByItem(po.getId()));
    }

    /** 발주 참조로 역집계한 품목별 입고 누계(POSTED 입고 기준). */
    private Map<Long, BigDecimal> receivedByItem(Long purchaseOrderId) {
        return goodsReceiptRepository.sumReceivedQuantityByPurchaseOrder(purchaseOrderId).stream()
                .collect(Collectors.toMap(ReceivedQtyRow::getItemId, ReceivedQtyRow::getQuantity));
    }

    /** 발주 전 라인이 전량 입고되었는지(라인별 입고누계 ≥ 발주수량). */
    private boolean isFullyReceived(PurchaseOrder po, Map<Long, BigDecimal> received) {
        return po.getLines().stream().allMatch(l ->
                received.getOrDefault(l.getItem().getId(), BigDecimal.ZERO)
                        .compareTo(l.getQuantity()) >= 0);
    }

    private void addLines(PurchaseOrder po, List<PurchaseOrderLineRequest> lineReqs) {
        Vendor vendor = po.getVendor();
        for (PurchaseOrderLineRequest lineReq : lineReqs) {
            Item item = itemRepository.findById(lineReq.itemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + lineReq.itemId()));
            // 구매정보레코드 검증 — 이 거래처가 현재 취급(ACTIVE)하는 품목만 발주 가능(입고와 동일 게이트).
            if (!vendorItemRepository.existsByVendorIdAndItemIdAndStatus(
                    vendor.getId(), item.getId(), MasterStatus.ACTIVE)) {
                throw new IllegalStateException(
                        "거래처 '" + vendor.getName() + "' 의 취급품목이 아닙니다: " + item.getName()
                                + " — 거래처 취급품목(구매정보레코드)에 먼저 등록하세요.");
            }
            po.addLine(item, lineReq.quantity(), lineReq.unitPrice());
        }
    }

    private PurchaseOrder getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PurchaseOrder not found: id=" + id));
    }
}

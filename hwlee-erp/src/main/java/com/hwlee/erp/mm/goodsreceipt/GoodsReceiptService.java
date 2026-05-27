package com.hwlee.erp.mm.goodsreceipt;

import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.master.vendor.Vendor;
import com.hwlee.erp.master.vendor.VendorRepository;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptLineRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptResponse;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptUpdateRequest;
import com.hwlee.erp.mm.stock.MovementReason;
import com.hwlee.erp.mm.stock.Stock;
import com.hwlee.erp.mm.stock.StockMovement;
import com.hwlee.erp.mm.stock.StockMovementRepository;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.mm.warehouse.Warehouse;
import com.hwlee.erp.mm.warehouse.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 입고 서비스 — 헤더 관리 + {@link #post} 시 Stock 가중평균 갱신 + StockMovement 적재.
 *
 * <p>동시성: Stock 갱신은 {@link Stock @Version} (낙관적 락) 으로 보호. 동시 입고 충돌은 드물고,
 * 충돌 시 {@code OptimisticLockingFailureException} 으로 전체 트랜잭션 롤백 → 클라이언트 재시도.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoodsReceiptService {

    private final GoodsReceiptRepository repository;
    private final GoodsReceiptMapper mapper;
    private final VendorRepository vendorRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final TransactionNumberGenerator numberGenerator;
    private final Clock clock;

    @Transactional
    public GoodsReceiptResponse create(GoodsReceiptCreateRequest req) {
        Vendor vendor = vendorRepository.findById(req.vendorId())
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: id=" + req.vendorId()));
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: id=" + req.warehouseId()));
        String number = numberGenerator.nextGoodsReceiptNumber(req.receiptDate());
        GoodsReceipt gr = GoodsReceipt.draft(number, vendor, warehouse, req.receiptDate());
        addLines(gr, req.lines());
        return mapper.toResponse(repository.save(gr));
    }

    public GoodsReceiptResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public Page<GoodsReceiptResponse> search(Specification<GoodsReceipt> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public GoodsReceiptResponse update(Long id, GoodsReceiptUpdateRequest req) {
        GoodsReceipt gr = getOrThrow(id);
        Vendor vendor = vendorRepository.findById(req.vendorId())
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: id=" + req.vendorId()));
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: id=" + req.warehouseId()));
        gr.updateHeader(vendor, warehouse, req.receiptDate());
        gr.clearLines();
        addLines(gr, req.lines());
        return mapper.toResponse(gr);
    }

    /**
     * DRAFT → POSTED. 라인별로:
     * <ol>
     *   <li>{@link Stock} 행을 찾거나(없으면) 생성</li>
     *   <li>{@code stock.receive(qty, unitCost)} 로 가중평균 갱신 + 수량 누적</li>
     *   <li>{@link StockMovement} (+) 행 적재</li>
     * </ol>
     */
    @Transactional
    public GoodsReceiptResponse post(Long id) {
        GoodsReceipt gr = getOrThrow(id);
        gr.post(LocalDateTime.now(clock));
        Warehouse warehouse = gr.getWarehouse();
        LocalDateTime now = LocalDateTime.now(clock);

        for (GoodsReceiptLine line : gr.getLines()) {
            Item item = line.getItem();
            Stock stock = stockRepository
                    .findByItemIdAndWarehouseId(item.getId(), warehouse.getId())
                    .orElseGet(() -> stockRepository.save(Stock.empty(item, warehouse)));
            stock.receive(line.getQuantity(), line.getUnitCost());

            stockMovementRepository.save(StockMovement.of(
                    item, warehouse, line.getQuantity(), line.getUnitCost(),
                    MovementReason.GOODS_RECEIPT, "GR", gr.getId(), now));
        }
        return mapper.toResponse(gr);
    }

    /**
     * POSTED → CANCELLED. 수량만 차감, 평균은 건드리지 않는다 (학습 노트 §10).
     * 그 사이 출고로 빠져 가용 재고가 부족하면 {@code InsufficientStockException}.
     */
    @Transactional
    public GoodsReceiptResponse cancel(Long id) {
        GoodsReceipt gr = getOrThrow(id);
        gr.cancel();
        Warehouse warehouse = gr.getWarehouse();
        LocalDateTime now = LocalDateTime.now(clock);

        for (GoodsReceiptLine line : gr.getLines()) {
            Item item = line.getItem();
            // 취소도 비관 락으로 잡는다 — 그 사이 출고와의 race 가 가능하므로.
            Stock stock = stockRepository
                    .findForUpdate(item.getId(), warehouse.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Stock not found: item=" + item.getId() + ", warehouse=" + warehouse.getId()));
            BigDecimal appliedCost = stock.cancelReceipt(line.getQuantity());

            stockMovementRepository.save(StockMovement.of(
                    item, warehouse, line.getQuantity().negate(), appliedCost,
                    MovementReason.ADJUSTMENT_MINUS, "GR", gr.getId(), now));
        }
        return mapper.toResponse(gr);
    }

    private void addLines(GoodsReceipt gr, List<GoodsReceiptLineRequest> lineReqs) {
        for (GoodsReceiptLineRequest lineReq : lineReqs) {
            Item item = itemRepository.findById(lineReq.itemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: id=" + lineReq.itemId()));
            gr.addLine(item, lineReq.quantity(), lineReq.unitCost());
        }
    }

    private GoodsReceipt getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("GoodsReceipt not found: id=" + id));
    }
}

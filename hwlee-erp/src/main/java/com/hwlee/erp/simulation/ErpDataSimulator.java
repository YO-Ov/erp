package com.hwlee.erp.simulation;

import com.hwlee.erp.common.entity.MasterStatus;
import com.hwlee.erp.fi.payment.PaymentService;
import com.hwlee.erp.fi.payment.PaymentType;
import com.hwlee.erp.fi.payment.dto.PaymentCreateRequest;
import com.hwlee.erp.hr.attendance.AttendanceService;
import com.hwlee.erp.hr.attendance.dto.AttendanceCreateRequest;
import com.hwlee.erp.master.customer.Customer;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.master.employee.Employee;
import com.hwlee.erp.master.employee.EmployeeRepository;
import com.hwlee.erp.master.item.Item;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.master.item.ItemType;
import com.hwlee.erp.master.vendor.Vendor;
import com.hwlee.erp.master.vendor.VendorRepository;
import com.hwlee.erp.master.vendoritem.VendorItem;
import com.hwlee.erp.master.vendoritem.VendorItemRepository;
import com.hwlee.erp.mm.goodsreceipt.GoodsReceiptService;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptLineRequest;
import com.hwlee.erp.mm.purchaseorder.PurchaseOrderService;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderCreateRequest;
import com.hwlee.erp.mm.purchaseorder.dto.PurchaseOrderLineRequest;
import com.hwlee.erp.mm.warehouse.Warehouse;
import com.hwlee.erp.mm.warehouse.WarehouseRepository;
import com.hwlee.erp.pp.order.ProductionService;
import com.hwlee.erp.pp.order.dto.ProductionOrderCreateRequest;
import com.hwlee.erp.sd.delivery.DeliveryService;
import com.hwlee.erp.sd.delivery.dto.DeliveryCreateRequest;
import com.hwlee.erp.sd.delivery.dto.DeliveryLineRequest;
import com.hwlee.erp.sd.invoice.InvoiceService;
import com.hwlee.erp.sd.invoice.dto.InvoiceCreateRequest;
import com.hwlee.erp.sd.invoice.dto.InvoiceLineRequest;
import com.hwlee.erp.sd.order.SalesOrderService;
import com.hwlee.erp.sd.order.dto.SalesOrderCreateRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderLineRequest;
import com.hwlee.erp.sd.quotation.QuotationService;
import com.hwlee.erp.sd.quotation.dto.QuotationCreateRequest;
import com.hwlee.erp.sd.quotation.dto.QuotationLineRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ERP 데이터 시뮬레이터 — "쓰지 않아도 매일 업무 데이터가 쌓이는 것처럼" 보이게 하는 학습/데모용 생성기.
 *
 * <p>MES 의 {@code ProductionSimulator} 와 같은 결로, 실제 사용자 조작 없이도 화면·대시보드·리포트가
 * 비어 보이지 않도록 주기적으로 소량의 데이터를 생성한다. 각 활동은 <b>실제 REST 와 동일한 서비스 메서드</b>를
 * 그대로 호출하므로 상태전이·자동분개·재고반영·이벤트 발행까지 정상 경로로 흐른다.
 *
 * <p>한 틱마다 아래 활동을 무작위로 조합해 목표 건수(per-tick)만큼 생성한다. 하나의 활동 실패가 다른 활동을
 * 막지 않도록 각각 격리한다(예: 여신 초과로 수주 확정 실패, 재고 부족으로 출하 실패 등은 조용히 건너뜀).
 *
 * <ul>
 *   <li>영업(SD): 견적 · 수주(+확정) · 출하 · 청구</li>
 *   <li>구매(MM): 발주 · 무발주 입고(+전기 → 재고·자동분개)</li>
 *   <li>생산(PP): 작업지시 초안(BOM 전개)</li>
 *   <li>회계(FI): 입금/출금(+즉시 전기·자동분개)</li>
 *   <li>인사(HR): 당일 근태</li>
 * </ul>
 *
 * <p>설정(application.yml, 기본 비활성 — 운영 프로파일에서만 켠다):
 * <pre>
 * erp.simulator.enabled       on/off (기본 false, prod 에서 true)
 * erp.simulator.cron          실행 주기 (기본 매일 08~20시 2시간마다)
 * erp.simulator.per-tick-min  틱당 최소 생성 건수 (기본 3)
 * erp.simulator.per-tick-max  틱당 최대 생성 건수 (기본 5)
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "erp.simulator.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ErpDataSimulator {

    private static final Logger log = LoggerFactory.getLogger(ErpDataSimulator.class);

    private final QuotationService quotationService;
    private final SalesOrderService salesOrderService;
    private final DeliveryService deliveryService;
    private final InvoiceService invoiceService;
    private final PurchaseOrderService purchaseOrderService;
    private final GoodsReceiptService goodsReceiptService;
    private final ProductionService productionService;
    private final PaymentService paymentService;
    private final AttendanceService attendanceService;

    private final CustomerRepository customerRepository;
    private final VendorRepository vendorRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final EmployeeRepository employeeRepository;
    private final VendorItemRepository vendorItemRepository;

    @Value("${erp.simulator.per-tick-min:3}")
    private int perTickMin;

    @Value("${erp.simulator.per-tick-max:5}")
    private int perTickMax;

    @Scheduled(cron = "${erp.simulator.cron:0 0 8-20/2 * * *}")
    public void tick() {
        List<Customer> customers = customerRepository.findAll();
        List<Vendor> vendors = vendorRepository.findAll();
        List<Item> items = itemRepository.findAll();
        List<Warehouse> warehouses = warehouseRepository.findAll();
        List<Employee> employees = employeeRepository.findAll();

        List<Item> finished = items.stream()
                .filter(i -> i.getItemType() == ItemType.FINISHED)
                .toList();
        Map<Long, List<VendorItem>> activeByVendor = vendorItemRepository.findAll().stream()
                .filter(vi -> vi.getStatus() == MasterStatus.ACTIVE)
                .collect(Collectors.groupingBy(vi -> vi.getVendor().getId()));

        if (customers.isEmpty() || warehouses.isEmpty() || finished.isEmpty()) {
            log.warn("[시뮬] 마스터 데이터 부족(고객·창고·완제품) — 이번 틱 생성 건너뜀");
            return;
        }

        int target = rand(perTickMin, perTickMax);
        int created = 0;
        int attempts = 0;
        int maxAttempts = target * 6 + 10; // 무한루프 방지 안전장치

        while (created < target && attempts < maxAttempts) {
            attempts++;
            int pick = rand(0, 6);
            try {
                created += switch (pick) {
                    case 0 -> newQuotation(customers, finished);
                    case 1 -> salesFlow(customers, finished, warehouses);
                    case 2 -> newPurchaseOrder(warehouses, activeByVendor);
                    case 3 -> goodsReceiptDirect(warehouses, activeByVendor);
                    case 4 -> newProductionOrder(finished, warehouses);
                    case 5 -> newPayment(customers, vendors);
                    default -> attendance(employees);
                };
            } catch (Exception e) {
                // 한 활동의 실패가 이번 틱 전체를 막지 않도록 격리.
                log.debug("[시뮬] 활동 {} 실패: {}", pick, e.getMessage());
            }
        }
        log.info("[시뮬] 이번 틱 데이터 {}건 생성 (목표 {})", created, target);
    }

    // ── SD: 견적 ────────────────────────────────────────────────
    private int newQuotation(List<Customer> customers, List<Item> finished) {
        var req = new QuotationCreateRequest(
                pick(customers).getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                subset(finished, 1, 3).stream()
                        .map(it -> new QuotationLineRequest(it.getId(), qty(), price(it)))
                        .toList());
        var res = quotationService.create(req);
        if (bool()) { // 절반은 발송까지
            try {
                quotationService.send(res.id());
            } catch (Exception ignore) {
                // DRAFT 로 남겨둠
            }
        }
        return 1;
    }

    // ── SD: 수주 → (확정) → 출하 → 청구 ─────────────────────────
    private int salesFlow(List<Customer> customers, List<Item> finished, List<Warehouse> warehouses) {
        var lines = subset(finished, 1, 3).stream()
                .map(it -> new SalesOrderLineRequest(it.getId(), qty(), price(it)))
                .toList();
        var so = salesOrderService.create(
                new SalesOrderCreateRequest(pick(customers).getId(), null, null, LocalDate.now(), lines));
        int count = 1;

        var confirmed = trySafe(() -> salesOrderService.confirm(so.id()));
        if (confirmed == null) {
            return count; // 여신 초과 등 → DRAFT 로 남김
        }
        count++;

        // 출하(재고 필요 — 소량 1개씩 시도, 재고 부족이면 건너뜀)
        if (bool()) {
            var delLines = confirmed.lines().stream()
                    .map(l -> new DeliveryLineRequest(l.id(), BigDecimal.ONE))
                    .toList();
            if (trySafe(() -> deliveryService.create(new DeliveryCreateRequest(
                    confirmed.id(), pick(warehouses).getId(), LocalDate.now(), delLines))) != null) {
                count++;
            }
        }

        // 청구(발행 + 자동분개까지 자동) — 소량 1개씩
        if (bool()) {
            var invLines = confirmed.lines().stream()
                    .map(l -> new InvoiceLineRequest(l.id(), BigDecimal.ONE))
                    .toList();
            if (trySafe(() -> invoiceService.create(
                    new InvoiceCreateRequest(confirmed.id(), LocalDate.now(), invLines))) != null) {
                count++;
            }
        }
        return count;
    }

    // ── MM: 발주(초안) ──────────────────────────────────────────
    private int newPurchaseOrder(List<Warehouse> warehouses, Map<Long, List<VendorItem>> activeByVendor) {
        Long vendorId = pickVendorWithItems(activeByVendor);
        if (vendorId == null) {
            return 0;
        }
        var lines = subset(activeByVendor.get(vendorId), 1, 3).stream()
                .map(vi -> new PurchaseOrderLineRequest(vi.getItem().getId(), qty(), vi.getSupplyPrice()))
                .toList();
        purchaseOrderService.create(new PurchaseOrderCreateRequest(
                vendorId, pick(warehouses).getId(), LocalDate.now(),
                LocalDate.now().plusDays(7), "자동 생성(데이터 시뮬레이터)", lines));
        return 1;
    }

    // ── MM: 무발주 입고 → 전기(재고 반영·자동분개) ──────────────
    private int goodsReceiptDirect(List<Warehouse> warehouses, Map<Long, List<VendorItem>> activeByVendor) {
        Long vendorId = pickVendorWithItems(activeByVendor);
        if (vendorId == null) {
            return 0;
        }
        var lines = subset(activeByVendor.get(vendorId), 1, 3).stream()
                .map(vi -> new GoodsReceiptLineRequest(vi.getItem().getId(), qty(), vi.getSupplyPrice()))
                .toList();
        var gr = goodsReceiptService.create(
                new GoodsReceiptCreateRequest(vendorId, pick(warehouses).getId(), LocalDate.now(), lines));
        goodsReceiptService.post(gr.id());
        return 1;
    }

    // ── PP: 작업지시 초안(BOM 전개) ─────────────────────────────
    private int newProductionOrder(List<Item> finished, List<Warehouse> warehouses) {
        // BOM 미등록 완제품은 예외 → 몇 번 다른 품목으로 재시도.
        for (int i = 0; i < 3; i++) {
            var it = pick(finished);
            if (trySafe(() -> productionService.createDraft(new ProductionOrderCreateRequest(
                    it.getId(), pick(warehouses).getId(), qty(), LocalDate.now(), LocalDate.now().plusDays(3)))) != null) {
                return 1;
            }
        }
        return 0;
    }

    // ── FI: 입금/출금(즉시 전기·자동분개) ───────────────────────
    private int newPayment(List<Customer> customers, List<Vendor> vendors) {
        BigDecimal amount = BigDecimal.valueOf(rand(1, 50) * 100_000L); // 10만~500만
        PaymentCreateRequest req;
        if (bool() || vendors.isEmpty()) {
            req = new PaymentCreateRequest(PaymentType.RECEIPT, pick(customers).getId(), null,
                    amount, LocalDate.now(), "자동 수금(데이터 시뮬레이터)");
        } else {
            req = new PaymentCreateRequest(PaymentType.DISBURSEMENT, null, pick(vendors).getId(),
                    amount, LocalDate.now(), "자동 지급(데이터 시뮬레이터)");
        }
        paymentService.createAndPost(req);
        return 1;
    }

    // ── HR: 당일 근태 ───────────────────────────────────────────
    private int attendance(List<Employee> employees) {
        if (employees.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (var emp : subset(employees, 1, 3)) {
            LocalTime clockIn = LocalTime.of(8, rand(30, 59));       // 08:30~08:59
            LocalTime clockOut = LocalTime.of(rand(18, 20), rand(0, 59)); // 18:00~20:59
            if (trySafe(() -> attendanceService.create(
                    new AttendanceCreateRequest(emp.getId(), LocalDate.now(), clockIn, clockOut))) != null) {
                count++; // (사원, 당일) 중복이면 예외 → 건너뜀
            }
        }
        return count;
    }

    // ── 헬퍼 ────────────────────────────────────────────────────

    /** 실패해도 예외를 삼키고 null 을 돌려주는 얇은 래퍼(성공 여부 판정용). */
    private <T> T trySafe(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (Exception e) {
            log.debug("[시뮬] 하위 액션 실패: {}", e.getMessage());
            return null;
        }
    }

    private <T> T pick(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /** 리스트에서 서로 다른 원소를 min~max 개 무작위로 뽑는다(라인 중복 방지). */
    private <T> List<T> subset(List<T> list, int min, int max) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        int n = Math.min(copy.size(), rand(min, max));
        return copy.subList(0, Math.max(1, n));
    }

    private Long pickVendorWithItems(Map<Long, List<VendorItem>> activeByVendor) {
        if (activeByVendor.isEmpty()) {
            return null;
        }
        List<Long> vendorIds = new ArrayList<>(activeByVendor.keySet());
        return pick(vendorIds);
    }

    private BigDecimal price(Item item) {
        BigDecimal p = item.getStandardPrice();
        return (p != null && p.signum() > 0) ? p : BigDecimal.valueOf(rand(1, 50) * 10_000L);
    }

    private BigDecimal qty() {
        return BigDecimal.valueOf(rand(1, 20));
    }

    private boolean bool() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /** min~max 사이의 정수(양끝 포함). */
    private int rand(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}

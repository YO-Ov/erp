package com.hwlee.erp.fi;

import static org.assertj.core.api.Assertions.assertThat;

import com.hwlee.erp.TestcontainersConfiguration;
import com.hwlee.erp.fi.account.AccountService;
import com.hwlee.erp.fi.journal.JournalEntry;
import com.hwlee.erp.fi.journal.JournalEntryRepository;
import com.hwlee.erp.fi.journal.JournalEntryStatus;
import com.hwlee.erp.fi.journal.JournalSource;
import com.hwlee.erp.fi.journal.SystemAccounts;
import com.hwlee.erp.fi.payment.PaymentService;
import com.hwlee.erp.fi.payment.PaymentType;
import com.hwlee.erp.fi.payment.dto.PaymentCreateRequest;
import com.hwlee.erp.fi.payment.dto.PaymentResponse;
import com.hwlee.erp.master.customer.CustomerRepository;
import com.hwlee.erp.master.customer.CustomerService;
import com.hwlee.erp.master.customer.PaymentTerms;
import com.hwlee.erp.master.customer.dto.CustomerCreateRequest;
import com.hwlee.erp.master.item.ItemCategory;
import com.hwlee.erp.master.item.ItemService;
import com.hwlee.erp.master.item.ItemUnit;
import com.hwlee.erp.master.item.dto.ItemCreateRequest;
import com.hwlee.erp.master.vendor.VendorService;
import com.hwlee.erp.master.vendor.dto.VendorCreateRequest;
import com.hwlee.erp.mm.goodsissue.GoodsIssueRepository;
import com.hwlee.erp.mm.goodsreceipt.GoodsReceiptService;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptCreateRequest;
import com.hwlee.erp.mm.goodsreceipt.dto.GoodsReceiptLineRequest;
import com.hwlee.erp.mm.warehouse.WarehouseService;
import com.hwlee.erp.mm.warehouse.dto.WarehouseCreateRequest;
import com.hwlee.erp.sd.delivery.DeliveryService;
import com.hwlee.erp.sd.delivery.dto.DeliveryCreateRequest;
import com.hwlee.erp.sd.delivery.dto.DeliveryLineRequest;
import com.hwlee.erp.sd.delivery.dto.DeliveryResponse;
import com.hwlee.erp.sd.invoice.InvoiceService;
import com.hwlee.erp.sd.invoice.dto.InvoiceCreateRequest;
import com.hwlee.erp.sd.invoice.dto.InvoiceLineRequest;
import com.hwlee.erp.sd.invoice.dto.InvoiceResponse;
import com.hwlee.erp.sd.order.SalesOrderService;
import com.hwlee.erp.sd.order.dto.SalesOrderCreateRequest;
import com.hwlee.erp.sd.order.dto.SalesOrderLineRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Phase 5 의 하이라이트 — 거래 사건이 자동 회계 전표가 되는 흐름.
 *
 * <p>다섯 가지 핵심 검증:
 * <ol>
 *   <li>매입(입고) → {@code 차)재고자산 / 대)매입채무}</li>
 *   <li>매출(인보이스 발행) → {@code 차)매출채권 / 대)매출+부가세예수금}</li>
 *   <li>매출원가(출하 확정) → {@code 차)매출원가 / 대)재고자산} — {@code @Order} 순서 보장 검증</li>
 *   <li>입금(Payment RECEIPT) → {@code 차)현금 / 대)매출채권} → 채권 잔액 0</li>
 *   <li>출금(Payment DISBURSEMENT) → {@code 차)매입채무 / 대)현금}</li>
 * </ol>
 *
 * <p>한 트랜잭션 안에서 발행자(SD/MM/Payment)와 자동 분개(FI)가 같이 commit 또는 같이 rollback —
 * Phase 4 의 BEFORE_COMMIT 패턴이 회계로 확장된 형태다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FiAccountingIntegrationTest {

    @Autowired CustomerService customerService;
    @Autowired CustomerRepository customerRepository;
    @Autowired ItemService itemService;
    @Autowired VendorService vendorService;
    @Autowired WarehouseService warehouseService;
    @Autowired GoodsReceiptService goodsReceiptService;
    @Autowired SalesOrderService salesOrderService;
    @Autowired DeliveryService deliveryService;
    @Autowired InvoiceService invoiceService;
    @Autowired PaymentService paymentService;
    @Autowired JournalEntryRepository journalRepository;
    @Autowired AccountService accountService;
    @Autowired GoodsIssueRepository goodsIssueRepository;

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. 매입 — 입고 확정 → 차)재고자산 / 대)매입채무
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("입고 확정 시 매입 분개(차)재고자산 / 대)매입채무) 가 자동 생성된다")
    void 입고_확정_시_매입_분개_자동_생성() {
        var ctx = setupMasters();
        // 10대 × 800,000원 입고 — 총 8,000,000원 매입
        var gr = goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(ctx.itemId, bd(10), bd(800000)))));
        goodsReceiptService.post(gr.id());

        // 매입 전표 1건 — source=GR, sourceId=grId
        List<JournalEntry> entries = journalRepository.findBySourceTypeAndSourceIdWithLines(JournalSource.GR, gr.id());
        assertThat(entries).hasSize(1);
        JournalEntry je = entries.get(0);

        assertThat(je.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(je.getDescription()).contains(gr.number());
        assertThat(je.getTotalDebit()).isEqualByComparingTo("8000000");
        assertThat(je.getTotalCredit()).isEqualByComparingTo("8000000");

        // 차변: 재고자산 8,000,000
        assertThat(debitOf(je, SystemAccounts.INVENTORY)).isEqualByComparingTo("8000000");
        // 대변: 매입채무 8,000,000
        assertThat(creditOf(je, SystemAccounts.AP)).isEqualByComparingTo("8000000");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. 매출 — 인보이스 발행 → 차)매출채권 / 대)매출 + 부가세예수금
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("인보이스 발행 시 매출 분개(차)매출채권 / 대)매출+부가세예수금) 가 자동 생성된다")
    void 인보이스_발행_시_매출_분개_자동_생성() {
        var ctx = setupMasters();
        stockUp(ctx, 10, bd(800000));   // 매입 분개도 함께 생성됨
        var so = createConfirmedOrder(ctx, 10, bd(1200000));

        // 인보이스는 출하된 만큼만 가능(invoiced_qty <= shipped_qty). 먼저 6대 출하 후 6대 인보이스.
        deliveryService.create(new DeliveryCreateRequest(
                so.salesOrderId, ctx.warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(so.salesOrderLineId, bd(6)))));

        // 6대 인보이스 — 공급가 7,200,000 + 부가세 720,000 = 총 7,920,000
        InvoiceResponse inv = invoiceService.create(new InvoiceCreateRequest(
                so.salesOrderId, LocalDate.now(),
                List.of(new InvoiceLineRequest(so.salesOrderLineId, bd(6)))));

        List<JournalEntry> entries = journalRepository.findBySourceTypeAndSourceIdWithLines(JournalSource.INV, inv.id());
        assertThat(entries).hasSize(1);
        JournalEntry je = entries.get(0);

        assertThat(je.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(je.getTotalDebit()).isEqualByComparingTo("7920000");
        assertThat(je.getTotalCredit()).isEqualByComparingTo("7920000");

        assertThat(debitOf(je, SystemAccounts.AR)).isEqualByComparingTo("7920000");
        assertThat(creditOf(je, SystemAccounts.SALES)).isEqualByComparingTo("7200000");
        assertThat(creditOf(je, SystemAccounts.VAT_PAYABLE)).isEqualByComparingTo("720000");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. 매출원가 — 출하 확정 → 차)매출원가 / 대)재고자산
    //    + @Order 검증 (재고 리스너가 먼저 돌아 unit_cost 가 박힌 뒤 회계가 읽음)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("출하 확정 시 매출원가 분개가 자동 생성된다 — @Order 로 재고 리스너가 먼저 돌아 단가가 정확히 합산된다")
    void 출하_확정_시_매출원가_분개_자동_생성() {
        var ctx = setupMasters();
        stockUp(ctx, 10, bd(800000));   // 평균단가 800,000
        var so = createConfirmedOrder(ctx, 10, bd(1200000));

        // 6대 출하 — 매출원가 = 6 × 800,000 = 4,800,000
        DeliveryResponse dlv = deliveryService.create(new DeliveryCreateRequest(
                so.salesOrderId, ctx.warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(so.salesOrderLineId, bd(6)))));

        // 출하 1건 → GoodsIssue 1건 → 매출원가 전표 1건. sourceType=GI, sourceId=giId.
        // 검색은 dlv.id() 가 아니라 GoodsIssue.id 로 해야 함.
        Long giId = goodsIssueIdOfDelivery(dlv.id());
        List<JournalEntry> entries = journalRepository.findBySourceTypeAndSourceIdWithLines(JournalSource.GI, giId);
        assertThat(entries)
                .as("출하 1건 → 매출원가 전표 1건")
                .hasSize(1);

        JournalEntry je = entries.get(0);
        assertThat(je.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(je.getTotalDebit())
                .as("재고 리스너(@Order=10) 가 unit_cost 를 박은 뒤 회계 리스너(@Order=20) 가 합산")
                .isEqualByComparingTo("4800000");
        assertThat(debitOf(je, SystemAccounts.COGS)).isEqualByComparingTo("4800000");
        assertThat(creditOf(je, SystemAccounts.INVENTORY)).isEqualByComparingTo("4800000");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. 입금 — Payment RECEIPT → 차)현금 / 대)매출채권
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("입금 등록 시 차)현금 / 대)매출채권 자동 분개")
    void 입금_등록_시_채권_감소_분개() {
        var ctx = setupMasters();
        PaymentResponse pay = paymentService.createAndPost(new PaymentCreateRequest(
                PaymentType.RECEIPT, ctx.customerId, null,
                new BigDecimal("1000000"), LocalDate.now(), "외상값 회수"));

        List<JournalEntry> entries = journalRepository.findBySourceTypeAndSourceIdWithLines(JournalSource.PAY, pay.id());
        assertThat(entries).hasSize(1);
        JournalEntry je = entries.get(0);

        assertThat(je.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(debitOf(je, SystemAccounts.CASH)).isEqualByComparingTo("1000000");
        assertThat(creditOf(je, SystemAccounts.AR)).isEqualByComparingTo("1000000");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 5. 출금 — Payment DISBURSEMENT → 차)매입채무 / 대)현금
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("출금 등록 시 차)매입채무 / 대)현금 자동 분개")
    void 출금_등록_시_채무_감소_분개() {
        var ctx = setupMasters();
        PaymentResponse pay = paymentService.createAndPost(new PaymentCreateRequest(
                PaymentType.DISBURSEMENT, null, ctx.vendorId,
                new BigDecimal("500000"), LocalDate.now(), "거래처 대금 지급"));

        List<JournalEntry> entries = journalRepository.findBySourceTypeAndSourceIdWithLines(JournalSource.PAY, pay.id());
        assertThat(entries).hasSize(1);
        JournalEntry je = entries.get(0);

        assertThat(je.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(debitOf(je, SystemAccounts.AP)).isEqualByComparingTo("500000");
        assertThat(creditOf(je, SystemAccounts.CASH)).isEqualByComparingTo("500000");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 6. 전체 시나리오 — 매입 → 수주 → 출하 → 인보이스 → 입금
    //    채권 잔액(매출채권 SUM) 이 0 으로 닫혀야 한다.
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("매입→출하→인보이스→입금 전체 시나리오 — 채권 잔액 0 / 손익 = 매출 - 매출원가")
    void 전체_OTC_시나리오로_채권이_닫히고_손익이_맞는다() {
        var ctx = setupMasters();
        // 입고 10대 × 800,000 → 매입 전표 8,000,000
        stockUp(ctx, 10, bd(800000));
        var so = createConfirmedOrder(ctx, 6, bd(1200000));

        // 출하 6대 → 매출원가 전표 4,800,000 (= 6 × 800,000)
        deliveryService.create(new DeliveryCreateRequest(
                so.salesOrderId, ctx.warehouseId, LocalDate.now(),
                List.of(new DeliveryLineRequest(so.salesOrderLineId, bd(6)))));

        // 인보이스 6대 → 매출 7,200,000 + 부가세 720,000, 매출채권 +7,920,000
        invoiceService.create(new InvoiceCreateRequest(
                so.salesOrderId, LocalDate.now(),
                List.of(new InvoiceLineRequest(so.salesOrderLineId, bd(6)))));

        // 입금 7,920,000 → 매출채권 -7,920,000, 현금 +7,920,000
        paymentService.createAndPost(new PaymentCreateRequest(
                PaymentType.RECEIPT, ctx.customerId, null,
                new BigDecimal("7920000"), LocalDate.now(), "수금"));

        // 매출채권 잔액 = +7,920,000 (인보이스) - 7,920,000 (입금) = 0
        assertThat(accountBalance(SystemAccounts.AR))
                .as("외상값을 모두 회수했으므로 매출채권 잔액 0")
                .isEqualByComparingTo(BigDecimal.ZERO);

        // 손익 = 매출 7,200,000 - 매출원가 4,800,000 = 2,400,000
        BigDecimal sales = accountBalance(SystemAccounts.SALES).abs();   // 대변 정상이라 음수
        BigDecimal cogs = accountBalance(SystemAccounts.COGS);
        assertThat(sales.subtract(cogs))
                .as("총이익 = 매출(7.2M) - 매출원가(4.8M) = 2.4M")
                .isEqualByComparingTo("2400000");

        // 부가세예수금 잔액 720,000 (대변, 즉 -720,000 in 차변-대변 차)
        assertThat(accountBalance(SystemAccounts.VAT_PAYABLE).abs())
                .as("부가세예수금 잔액 — 다음 부가세 신고 시 납부 대상")
                .isEqualByComparingTo("720000");
    }

    // === 헬퍼 ===

    /**
     * 계정 잔액 — POSTED 전표의 그 계정 라인 (debit - credit) 합.
     * 차변 정상 계정(자산/비용)은 양수, 대변 정상 계정(부채/자본/수익)은 음수가 정상 방향.
     */
    private BigDecimal accountBalance(String accountCode) {
        return journalRepository.findPostedLinesByAccountCode(accountCode).stream()
                .map(line -> line.getDebit().subtract(line.getCredit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Long goodsIssueIdOfDelivery(Long deliveryId) {
        return goodsIssueRepository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new AssertionError("GoodsIssue not found for delivery=" + deliveryId))
                .getId();
    }

    private static BigDecimal debitOf(JournalEntry je, String accountCode) {
        return je.getLines().stream()
                .filter(l -> l.getAccount().getCode().equals(accountCode))
                .map(l -> l.getDebit())
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal creditOf(JournalEntry je, String accountCode) {
        return je.getLines().stream()
                .filter(l -> l.getAccount().getCode().equals(accountCode))
                .map(l -> l.getCredit())
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private record TestContext(Long customerId, Long itemId, Long warehouseId, Long vendorId) {}
    private record OrderContext(Long salesOrderId, Long salesOrderLineId) {}

    private TestContext setupMasters() {
        long nano = System.nanoTime();
        var customer = customerService.create(new CustomerCreateRequest(
                "현우테크-" + nano, uniqueBusinessNo(), "서울시", PaymentTerms.NET30));
        // 생성 시 한도는 항상 0이므로, 수주 확정이 신용한도 검증을 통과하도록 충분히 올려준다.
        customerRepository.findById(customer.id()).orElseThrow().changeCreditLimit(new BigDecimal("100000000"));
        customerRepository.flush();
        var item = itemService.create(new ItemCreateRequest(
                "노트북-" + nano, ItemCategory.NOTEBOOK, ItemUnit.EA, bd(800000), bd(1200000)));
        var warehouse = warehouseService.create(new WarehouseCreateRequest(
                "WH-" + whSuffix(), "본사창고", "서울시"));
        var vendor = vendorService.create(new VendorCreateRequest(
                "거래처-" + nano, uniqueBusinessNo(), "인천시", PaymentTerms.NET30));
        return new TestContext(customer.id(), item.id(), warehouse.id(), vendor.id());
    }

    private void stockUp(TestContext ctx, int qty, BigDecimal unitCost) {
        var gr = goodsReceiptService.create(new GoodsReceiptCreateRequest(
                ctx.vendorId, ctx.warehouseId, LocalDate.now(),
                List.of(new GoodsReceiptLineRequest(ctx.itemId, new BigDecimal(qty), unitCost))));
        goodsReceiptService.post(gr.id());
    }

    private OrderContext createConfirmedOrder(TestContext ctx, int qty, BigDecimal unitPrice) {
        var order = salesOrderService.create(new SalesOrderCreateRequest(
                ctx.customerId, null, null, LocalDate.now(),
                List.of(new SalesOrderLineRequest(ctx.itemId, bd(qty), unitPrice))));
        salesOrderService.confirm(order.id());
        Long solId = salesOrderService.findById(order.id()).lines().get(0).id();
        return new OrderContext(order.id(), solId);
    }

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime());

    private static String uniqueBusinessNo() {
        long n = SEQ.incrementAndGet();
        return String.format("%03d-%02d-%05d",
                (int) ((n / 10_000_000L) % 900) + 100,
                (int) ((n / 100_000L) % 100),
                (int) (n % 100_000L));
    }

    private static String whSuffix() {
        long n = SEQ.incrementAndGet();
        return "T" + Long.toString(n, 36).toUpperCase().replace("-", "");
    }

    private static BigDecimal bd(long n) { return new BigDecimal(n); }
}

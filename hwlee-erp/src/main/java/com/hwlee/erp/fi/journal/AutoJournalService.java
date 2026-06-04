package com.hwlee.erp.fi.journal;

import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.fi.account.Account;
import com.hwlee.erp.fi.account.AccountService;
import com.hwlee.erp.mm.goodsissue.GoodsIssue;
import com.hwlee.erp.mm.goodsissue.GoodsIssueRepository;
import com.hwlee.erp.mm.goodsreceipt.event.GoodsReceiptPostedEvent;
import com.hwlee.erp.mm.stock.MovementReason;
import com.hwlee.erp.mm.stock.StockMovement;
import com.hwlee.erp.mm.stock.StockMovementRepository;
import com.hwlee.erp.hr.payroll.event.PayrollConfirmedEvent;
import com.hwlee.erp.hr.payroll.event.PayrollPaidEvent;
import com.hwlee.erp.master.item.ItemRepository;
import com.hwlee.erp.master.item.ItemType;
import com.hwlee.erp.sd.delivery.event.DeliveryShippedEvent;
import com.hwlee.erp.sd.invoice.event.InvoiceIssuedEvent;
import java.time.LocalDate;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 자동 분개 — 사건(인보이스/출하/입고/입금/출금) 을 회계 전표로 변환한다.
 *
 * <p>설계 #7 — 범용 규칙 엔진 대신 사건별 메서드. 분개 규칙이 자바 코드로 한눈에 읽힌다.
 *
 * <p>이 서비스는 회계 리스너({@code fi/integration/sd|mm/}) 가 직접 호출한다.
 * 리스너는 {@code @TransactionalEventListener(BEFORE_COMMIT)} 이라 발행자 트랜잭션에 참여 —
 * 분개 실패는 발행자(인보이스 발행/출하 확정)까지 모두 롤백시킨다 (정합성 우선).
 *
 * <p>모든 메서드는 {@code @Transactional(REQUIRED)} — 이미 열린 트랜잭션에 합류.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoJournalService {

    private final JournalEntryRepository journalRepository;
    private final AccountService accountService;
    private final GoodsIssueRepository goodsIssueRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ItemRepository itemRepository;
    private final TransactionNumberGenerator numberGenerator;
    private final Clock clock;

    /**
     * 매출 분개 — 인보이스 발행 사건으로부터.
     * <pre>
     *   차) 매출채권 totalAmount
     *   대) 매출         subtotal
     *   대) 부가세예수금 taxAmount
     * </pre>
     */
    @Transactional
    public JournalEntry createSalesEntry(InvoiceIssuedEvent event) {
        String number = numberGenerator.nextJournalEntryNumber(event.invoiceDate());
        JournalEntry entry = JournalEntry.draft(
                number, event.invoiceDate(),
                "매출 " + event.number(),
                JournalSource.INV, event.invoiceId());

        entry.addDebit(account(SystemAccounts.AR),           event.totalAmount());
        entry.addCredit(account(SystemAccounts.SALES),       event.subtotal());
        entry.addCredit(account(SystemAccounts.VAT_PAYABLE), event.taxAmount());

        entry.post(LocalDateTime.now(clock));
        log.info("매출 자동 전표 생성: {} (invoiceId={}, total={})",
                number, event.invoiceId(), event.totalAmount());
        return journalRepository.save(entry);
    }

    /**
     * 매출원가 분개 — 출하 확정 사건으로부터.
     * <pre>
     *   차) 매출원가 totalCost
     *   대) 재고자산 totalCost
     * </pre>
     *
     * <p>핵심: 출고 단가는 {@link DeliveryShippedEvent} 본문에 없다. 같은 트랜잭션 안에서
     * 재고 리스너({@code @Order(10)}) 가 먼저 GoodsIssue.post 를 끝내 StockMovement 의
     * {@code unit_cost} 를 박아두고, 회계 리스너({@code @Order(20)}) 가 나중에 그것을 읽어 합산한다.
     * 순서가 정합성을 좌우 — Phase 5 학습의 새 포인트.
     */
    @Transactional
    public JournalEntry createCogsEntry(DeliveryShippedEvent event) {
        // 1) deliveryId → GoodsIssue 역추적 (Phase 4 가 만들어 둔 매핑).
        GoodsIssue gi = goodsIssueRepository.findByDeliveryId(event.deliveryId())
                .orElseThrow(() -> new IllegalStateException(
                        "GoodsIssue 가 없다 — 재고 리스너(@Order=10) 가 먼저 돌지 않았다. "
                                + "deliveryId=" + event.deliveryId()));

        // 2) StockMovement(GOODS_ISSUE) 합산 → 매출원가 총액.
        BigDecimal totalCost = stockMovementRepository
                .findByRefTypeAndRefIdAndReason("GI", gi.getId(), MovementReason.GOODS_ISSUE)
                .stream()
                .map(m -> m.getUnitCost().multiply(m.getQtyDelta().abs()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalCost.signum() == 0) {
            // 단가가 0인 매출원가 전표는 의미가 없고 차변/대변 양쪽 0 → CHECK 제약에도 걸린다.
            log.warn("매출원가 0 — 전표 생략. deliveryId={}, giId={}", event.deliveryId(), gi.getId());
            return null;
        }

        String number = numberGenerator.nextJournalEntryNumber(event.shippedDate());
        JournalEntry entry = JournalEntry.draft(
                number, event.shippedDate(),
                "매출원가 " + gi.getNumber(),
                JournalSource.GI, gi.getId());

        entry.addDebit(account(SystemAccounts.COGS),       totalCost);
        entry.addCredit(account(SystemAccounts.INVENTORY), totalCost);

        entry.post(LocalDateTime.now(clock));
        log.info("매출원가 자동 전표 생성: {} (deliveryId={}, giId={}, totalCost={})",
                number, event.deliveryId(), gi.getId(), totalCost);
        return journalRepository.save(entry);
    }

    /**
     * 매입 분개 — 입고 확정 사건으로부터. (Phase 8 — 품목 유형별 재고 계정 분기)
     * <pre>
     *   차) 제품(재고자산)  Σ(FINISHED 라인)
     *   차) 원재료          Σ(COMPONENT 라인)
     *           대) 매입채무 totalCost
     * </pre>
     * 매입 부가세는 학습 1차 범위에서 분리하지 않는다 (단가에 포함 가정).
     */
    @Transactional
    public JournalEntry createPurchaseEntry(GoodsReceiptPostedEvent event) {
        BigDecimal finishedTotal = BigDecimal.ZERO; // FINISHED → 제품(1400)
        BigDecimal rawTotal = BigDecimal.ZERO;      // COMPONENT → 원재료(1410)
        for (GoodsReceiptPostedEvent.Line l : event.lines()) {
            BigDecimal lineCost = l.unitCost().multiply(l.quantity());
            ItemType type = itemRepository.findById(l.itemId())
                    .map(com.hwlee.erp.master.item.Item::getItemType)
                    .orElse(ItemType.FINISHED);
            if (type == ItemType.COMPONENT) {
                rawTotal = rawTotal.add(lineCost);
            } else {
                finishedTotal = finishedTotal.add(lineCost);
            }
        }
        BigDecimal totalCost = finishedTotal.add(rawTotal);

        if (totalCost.signum() == 0) {
            log.warn("매입 0 — 전표 생략. grId={}", event.goodsReceiptId());
            return null;
        }

        String number = numberGenerator.nextJournalEntryNumber(event.receiptDate());
        JournalEntry entry = JournalEntry.draft(
                number, event.receiptDate(),
                "매입 " + event.number(),
                JournalSource.GR, event.goodsReceiptId());

        if (finishedTotal.signum() > 0) {
            entry.addDebit(account(SystemAccounts.INVENTORY), finishedTotal);
        }
        if (rawTotal.signum() > 0) {
            entry.addDebit(account(SystemAccounts.RAW_MATERIAL), rawTotal);
        }
        entry.addCredit(account(SystemAccounts.AP), totalCost);

        entry.post(LocalDateTime.now(clock));
        log.info("매입 자동 전표 생성: {} (grId={}, 제품={}, 원재료={})",
                number, event.goodsReceiptId(), finishedTotal, rawTotal);
        return journalRepository.save(entry);
    }

    /**
     * 생산 완료 분개 (Phase 8) — 직접재료비만(재공품 WIP·노무비·제조간접비 생략).
     * <pre>
     *   차) 제품(재고자산) materialCost
     *           대) 원재료  materialCost
     * </pre>
     * 투입 부품의 실제(이동평균) 원가 합이 완제품 원가로 이동 — 원가 보존. 가공원가는 0(간이).
     * {@code ProductionService} 가 완료 시 직접 호출(이벤트 아님 — 완제품 원가가 부품 출고원가에 순차 의존).
     */
    @Transactional
    public JournalEntry createProductionEntry(Long productionOrderId, String orderNumber,
                                              LocalDate completedDate, BigDecimal materialCost) {
        if (materialCost == null || materialCost.signum() == 0) {
            log.warn("생산 직접재료비 0 — 전표 생략. poId={}", productionOrderId);
            return null;
        }
        String number = numberGenerator.nextJournalEntryNumber(completedDate);
        JournalEntry entry = JournalEntry.draft(
                number, completedDate,
                "생산완료 " + orderNumber,
                JournalSource.PROD, productionOrderId);

        entry.addDebit(account(SystemAccounts.INVENTORY),     materialCost);  // 제품 ↑
        entry.addCredit(account(SystemAccounts.RAW_MATERIAL), materialCost);  // 원재료 ↓

        entry.post(LocalDateTime.now(clock));
        log.info("생산 완료 자동 전표 생성: {} (poId={}, materialCost={})",
                number, productionOrderId, materialCost);
        return journalRepository.save(entry);
    }

    /**
     * 입금 분개 — 고객이 외상값을 갚을 때.
     * <pre>
     *   차) 현금     amount
     *   대) 매출채권 amount
     * </pre>
     */
    @Transactional
    public JournalEntry createReceiptEntry(Long paymentId, java.time.LocalDate paymentDate,
                                           String paymentNumber, BigDecimal amount) {
        String number = numberGenerator.nextJournalEntryNumber(paymentDate);
        JournalEntry entry = JournalEntry.draft(
                number, paymentDate,
                "입금 " + paymentNumber,
                JournalSource.PAY, paymentId);

        entry.addDebit(account(SystemAccounts.CASH), amount);
        entry.addCredit(account(SystemAccounts.AR),  amount);

        entry.post(LocalDateTime.now(clock));
        log.info("입금 자동 전표 생성: {} (paymentId={}, amount={})", number, paymentId, amount);
        return journalRepository.save(entry);
    }

    /**
     * 출금 분개 — 거래처에 외상값을 지급할 때.
     * <pre>
     *   차) 매입채무 amount
     *   대) 현금     amount
     * </pre>
     */
    @Transactional
    public JournalEntry createDisbursementEntry(Long paymentId, java.time.LocalDate paymentDate,
                                                String paymentNumber, BigDecimal amount) {
        String number = numberGenerator.nextJournalEntryNumber(paymentDate);
        JournalEntry entry = JournalEntry.draft(
                number, paymentDate,
                "출금 " + paymentNumber,
                JournalSource.PAY, paymentId);

        entry.addDebit(account(SystemAccounts.AP),    amount);
        entry.addCredit(account(SystemAccounts.CASH), amount);

        entry.post(LocalDateTime.now(clock));
        log.info("출금 자동 전표 생성: {} (paymentId={}, amount={})", number, paymentId, amount);
        return journalRepository.save(entry);
    }

    /**
     * 급여 확정 분개 — 급여대장 확정 사건으로부터 (Phase 7).
     * <pre>
     *   차) 급여비용   Σgross
     *   차) 법정복리비 Σinsurance_company           (4대보험 회사부담분 = 회사 추가비용)
     *           대) 예수금-소득세   Σincome_tax
     *           대) 예수금-사회보험 Σ(insurance_employee + insurance_company)
     *           대) 미지급급여      Σnet
     * </pre>
     *
     * <p>차변 합 = Σgross + Σins_company, 대변 합 = Σtax + Σ(ins_emp+ins_company) + Σnet.
     * net = gross − tax − ins_emp 이므로 양변이 항상 일치 → 복식부기 불변식 통과(라운딩과 무관).
     */
    @Transactional
    public JournalEntry createPayrollEntry(PayrollConfirmedEvent event) {
        String number = numberGenerator.nextJournalEntryNumber(event.entryDate());
        JournalEntry entry = JournalEntry.draft(
                number, event.entryDate(),
                "급여 " + event.number(),
                JournalSource.PAYROLL, event.payrollRunId());

        entry.addDebit(account(SystemAccounts.SALARY_EXPENSE), event.totalGross());
        entry.addDebit(account(SystemAccounts.LEGAL_WELFARE),  event.totalInsuranceCompany());
        entry.addCredit(account(SystemAccounts.WITHHOLDING_TAX),       event.totalIncomeTax());
        entry.addCredit(account(SystemAccounts.WITHHOLDING_INSURANCE),
                event.totalInsuranceEmployee().add(event.totalInsuranceCompany()));
        entry.addCredit(account(SystemAccounts.SALARY_PAYABLE),        event.totalNet());

        entry.post(LocalDateTime.now(clock));
        log.info("급여 확정 자동 전표 생성: {} (payrollRunId={}, gross={}, net={})",
                number, event.payrollRunId(), event.totalGross(), event.totalNet());
        return journalRepository.save(entry);
    }

    /**
     * 급여 지급 분개 — 급여대장 지급 사건으로부터 (Phase 7).
     * <pre>
     *   차) 미지급급여 Σnet
     *   대) 현금       Σnet
     * </pre>
     * 확정 시 인식한 미지급급여(부채)를 현금 유출로 상계 — 발생주의의 2단계 중 지급 단계.
     */
    @Transactional
    public JournalEntry createSalaryPaymentEntry(PayrollPaidEvent event) {
        String number = numberGenerator.nextJournalEntryNumber(event.paymentDate());
        JournalEntry entry = JournalEntry.draft(
                number, event.paymentDate(),
                "급여지급 " + event.number(),
                JournalSource.PAYROLL, event.payrollRunId());

        entry.addDebit(account(SystemAccounts.SALARY_PAYABLE), event.totalNet());
        entry.addCredit(account(SystemAccounts.CASH),          event.totalNet());

        entry.post(LocalDateTime.now(clock));
        log.info("급여 지급 자동 전표 생성: {} (payrollRunId={}, net={})",
                number, event.payrollRunId(), event.totalNet());
        return journalRepository.save(entry);
    }

    private Account account(String code) {
        try {
            return accountService.getEntityByCode(code);
        } catch (EntityNotFoundException e) {
            throw new IllegalStateException(
                    "시스템 계정 " + code + " 가 마스터에 없다. V23 시드 점검 필요.", e);
        }
    }
}

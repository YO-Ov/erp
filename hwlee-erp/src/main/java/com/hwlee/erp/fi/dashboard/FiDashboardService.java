package com.hwlee.erp.fi.dashboard;

import com.hwlee.erp.fi.credit.CreditLimitRequestRepository;
import com.hwlee.erp.fi.credit.CreditLimitRequestStatus;
import com.hwlee.erp.fi.dashboard.dto.FiDashboardResponse;
import com.hwlee.erp.fi.journal.JournalEntryRepository;
import com.hwlee.erp.fi.journal.JournalEntryStatus;
import com.hwlee.erp.fi.payment.PaymentRepository;
import com.hwlee.erp.sd.invoice.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 재무(FI) 대시보드 집계 — 청구/입금/전표/여신을 서버에서 정확히 합산한다. */
@Service
@RequiredArgsConstructor
public class FiDashboardService {

    private static final String AR_DEFINITION =
            "미수금 = 발행(ISSUED) 인보이스 총액 합(부가세 포함) − 확정(POSTED) 입금(RECEIPT) 합";

    private final InvoiceRepository invoiceRepo;
    private final PaymentRepository paymentRepo;
    private final JournalEntryRepository journalRepo;
    private final CreditLimitRequestRepository creditRepo;

    @Transactional(readOnly = true)
    public FiDashboardResponse summary() {
        YearMonth ym = YearMonth.now();
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        BigDecimal thisMonthSales = invoiceRepo.sumIssuedTotalBetween(from, to);
        BigDecimal thisMonthReceipt = paymentRepo.sumPostedReceiptAmountBetween(from, to);

        // 미수금 = 발행 인보이스 합 − 확정 입금 합. 음수면 0 으로 절사(선수금 상황 방어).
        BigDecimal issuedTotal = invoiceRepo.sumAllIssuedInvoiceTotal();
        BigDecimal receiptTotal = paymentRepo.sumAllPostedReceiptAmount();
        BigDecimal receivable = issuedTotal.subtract(receiptTotal).max(BigDecimal.ZERO);

        long pendingJournal = journalRepo.countByStatus(JournalEntryStatus.DRAFT);
        long pendingCredit = creditRepo.countByStatus(CreditLimitRequestStatus.PENDING);

        return new FiDashboardResponse(
                thisMonthSales, thisMonthReceipt, receivable,
                pendingJournal, pendingCredit, AR_DEFINITION);
    }
}

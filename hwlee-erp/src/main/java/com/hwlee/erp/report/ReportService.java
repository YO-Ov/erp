package com.hwlee.erp.report;

import com.hwlee.erp.fi.account.AccountType;
import com.hwlee.erp.fi.journal.JournalEntryRepository;
import com.hwlee.erp.fi.journal.SystemAccounts;
import com.hwlee.erp.mm.stock.StockRepository;
import com.hwlee.erp.report.dto.AccountAmount;
import com.hwlee.erp.report.dto.IncomeStatementLine;
import com.hwlee.erp.report.dto.IncomeStatementResponse;
import com.hwlee.erp.report.dto.InventoryReportResponse;
import com.hwlee.erp.report.dto.InventoryReportRow;
import com.hwlee.erp.report.dto.SalesReportResponse;
import com.hwlee.erp.report.dto.SalesReportRow;
import com.hwlee.erp.sd.invoice.Invoice;
import com.hwlee.erp.sd.invoice.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리포트(정보계 맛보기) read 전용 서비스 — 기간계 트랜잭션 데이터를 집계해 보여준다.
 *
 * <p>OLTP(기간계)는 건별 정합성, 여기서는 그 데이터를 GROUP BY/SUM 으로 요약(OLAP 성격).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final StockRepository stockRepository;
    private final JournalEntryRepository journalEntryRepository;

    /** 매출 리포트 — 일별/월별 발행 인보이스 집계 + 합계행. */
    public SalesReportResponse salesReport(LocalDate from, LocalDate to, String unit) {
        boolean monthly = "MONTH".equalsIgnoreCase(unit);

        // 기간 인보이스를 구간 키(일별 yyyy-MM-dd / 월별 yyyy-MM)로 그룹핑.
        Map<String, List<Invoice>> grouped = invoiceRepository.findIssuedBetween(from, to).stream()
                .collect(Collectors.groupingBy(i -> periodKey(i.getInvoiceDate(), monthly),
                        TreeMap::new, Collectors.toList()));

        List<SalesReportRow> rows = grouped.entrySet().stream()
                .map(e -> toRow(e.getKey(), e.getValue()))
                .toList();

        long count = rows.stream().mapToLong(SalesReportRow::invoiceCount).sum();
        SalesReportRow totalRow = new SalesReportRow("합계", count,
                sum(rows, SalesReportRow::subtotal),
                sum(rows, SalesReportRow::taxAmount),
                sum(rows, SalesReportRow::totalAmount));

        return new SalesReportResponse(monthly ? "MONTH" : "DAY", from, to, rows, totalRow);
    }

    private static String periodKey(LocalDate date, boolean monthly) {
        return monthly ? java.time.YearMonth.from(date).toString() : date.toString();
    }

    private static SalesReportRow toRow(String period, List<Invoice> invoices) {
        return new SalesReportRow(period, (long) invoices.size(),
                invoices.stream().map(Invoice::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                invoices.stream().map(Invoice::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                invoices.stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** 재고 현황 리포트 — 보유>0 (품목, 창고)별 평가액 + 총계. */
    public InventoryReportResponse inventoryReport(Long itemId, Long warehouseId) {
        List<InventoryReportRow> rows = stockRepository.inventoryReport(itemId, warehouseId);
        BigDecimal totalValuation = rows.stream()
                .map(InventoryReportRow::valuationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InventoryReportResponse(rows, totalValuation);
    }

    /** 손익계산서(미니) — 기간 POSTED 전표의 수익/비용을 집계해 단계별 이익을 산출. */
    public IncomeStatementResponse incomeStatement(LocalDate from, LocalDate to) {
        List<AccountAmount> sums = journalEntryRepository.incomeStatementSums(from, to);

        List<IncomeStatementLine> revenueLines = sums.stream()
                .filter(a -> a.type() == AccountType.REVENUE)
                .map(a -> new IncomeStatementLine(a.code(), a.name(), a.normalAmount()))
                .toList();
        List<IncomeStatementLine> expenseLines = sums.stream()
                .filter(a -> a.type() == AccountType.EXPENSE)
                .map(a -> new IncomeStatementLine(a.code(), a.name(), a.normalAmount()))
                .toList();

        BigDecimal revenue = sumLines(revenueLines);
        BigDecimal totalExpense = sumLines(expenseLines);
        BigDecimal cogs = sums.stream()
                .filter(a -> a.code().equals(SystemAccounts.COGS))
                .map(AccountAmount::normalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sga = totalExpense.subtract(cogs);
        BigDecimal grossProfit = revenue.subtract(cogs);
        BigDecimal operatingProfit = grossProfit.subtract(sga);
        BigDecimal netIncome = revenue.subtract(totalExpense);

        return new IncomeStatementResponse(from, to, revenue, cogs, grossProfit, sga,
                operatingProfit, netIncome, revenueLines, expenseLines);
    }

    private static BigDecimal sum(List<SalesReportRow> rows,
                                  java.util.function.Function<SalesReportRow, BigDecimal> field) {
        return rows.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumLines(List<IncomeStatementLine> lines) {
        return lines.stream().map(IncomeStatementLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

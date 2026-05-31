package com.hwlee.erp.hr.payroll;

import com.hwlee.erp.common.code.TransactionNumberGenerator;
import com.hwlee.erp.hr.attendance.AttendanceRepository;
import com.hwlee.erp.hr.contract.EmploymentContract;
import com.hwlee.erp.hr.contract.EmploymentContractRepository;
import com.hwlee.erp.hr.payroll.dto.PayrollRunCreateRequest;
import com.hwlee.erp.hr.payroll.dto.PayrollRunResponse;
import com.hwlee.erp.hr.payroll.event.PayrollConfirmedEvent;
import com.hwlee.erp.hr.payroll.event.PayrollPaidEvent;
import com.hwlee.erp.master.employee.Employee;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 급여 계산·확정·지급 — Phase 7 의 중심.
 *
 * <p>핵심 흐름: 대상 월의 유효 급여계약을 찾아, 그 달 근태(연장근로)를 집계하고,
 * 정책({@link PayrollPolicy})으로 공제까지 계산한 명세서를 만든다(DRAFT).
 * 확정/지급 시 회계 이벤트를 발행 — FI 가 인건비/지급 전표를 자동 생성한다(HR 은 FI 를 직접 모른다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollService {

    private final PayrollRunRepository repository;
    private final EmploymentContractRepository contractRepository;
    private final AttendanceRepository attendanceRepository;
    private final TransactionNumberGenerator numberGenerator;
    private final PayrollMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * 대상 월의 급여대장을 계산해 DRAFT 로 만든다.
     * 그 달 말일에 유효한 ACTIVE 계약을 가진 직원 전원에 대해 명세서를 생성.
     */
    @Transactional
    public PayrollRunResponse createDraft(PayrollRunCreateRequest req) {
        YearMonth ym = YearMonth.parse(req.period());
        if (repository.existsByPeriod(req.period())) {
            throw new IllegalArgumentException("이미 해당 월의 급여대장이 있습니다. period=" + req.period());
        }
        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        String number = numberGenerator.nextPayrollNumber(ym);
        PayrollRun run = PayrollRun.draft(number, req.period(), LocalDate.now(clock));

        List<EmploymentContract> contracts = contractRepository.findEffectiveOn(lastDay);
        if (contracts.isEmpty()) {
            throw new IllegalStateException(
                    "해당 월에 유효한 급여계약이 없습니다. period=" + req.period());
        }
        for (EmploymentContract contract : contracts) {
            Employee employee = contract.getEmployee();
            int overtimeMinutes = attendanceRepository.sumOvertimeMinutes(
                    employee.getId(), firstDay, lastDay);

            BigDecimal basePay = contract.getBaseSalary();
            BigDecimal overtimePay = PayrollPolicy.overtimePay(contract.hourlyWage(), overtimeMinutes);
            BigDecimal grossPay = basePay.add(overtimePay);
            BigDecimal incomeTax = PayrollPolicy.incomeTax(grossPay);
            BigDecimal insuranceEmployee = PayrollPolicy.insuranceEmployee(grossPay);
            BigDecimal insuranceCompany = PayrollPolicy.insuranceCompany(grossPay);

            run.addPayslip(employee, basePay, overtimePay, incomeTax, insuranceEmployee, insuranceCompany);
        }
        return mapper.toResponse(repository.save(run));
    }

    public PayrollRunResponse findById(Long id) {
        return mapper.toResponse(repository.findByIdWithPayslips(id)
                .orElseThrow(() -> new EntityNotFoundException("PayrollRun not found: id=" + id)));
    }

    /**
     * 급여 확정 — 비용 인식 단계. 확정 사건을 발행해 FI 가 인건비 전표를 만든다(같은 트랜잭션).
     * 분개 실패(차/대 불일치 등)면 확정 자체가 롤백.
     */
    @Transactional
    public PayrollRunResponse confirm(Long id) {
        PayrollRun run = getOrThrow(id);
        run.confirm(LocalDateTime.now(clock));
        events.publishEvent(new PayrollConfirmedEvent(
                run.getId(), run.getNumber(), run.getRunDate(),
                run.getTotalGross(), run.totalIncomeTax(),
                run.totalInsuranceEmployee(), run.totalInsuranceCompany(), run.getTotalNet()));
        return mapper.toResponse(run);
    }

    /**
     * 급여 지급 — 현금 유출 단계. 지급 사건을 발행해 FI 가 지급 전표(미지급급여→현금)를 만든다.
     */
    @Transactional
    public PayrollRunResponse markPaid(Long id) {
        PayrollRun run = getOrThrow(id);
        run.markPaid(LocalDateTime.now(clock));
        events.publishEvent(new PayrollPaidEvent(
                run.getId(), run.getNumber(), LocalDate.now(clock), run.getTotalNet()));
        return mapper.toResponse(run);
    }

    PayrollRun getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PayrollRun not found: id=" + id));
    }
}

package com.hwlee.mes.simulation;

import com.hwlee.mes.performance.PerformanceService;
import com.hwlee.mes.performance.dto.ReportRequest;
import com.hwlee.mes.quality.DefectReason;
import com.hwlee.mes.quality.DefectReasonRepository;
import com.hwlee.mes.workorder.WorkOrder;
import com.hwlee.mes.workorder.WorkOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 생산 시뮬레이터 — "설비 가동을 켜면 제품이 만들어지는 것처럼" 보이게 하는 학습용 데모.
 *
 * <p>주기적으로(기본 2초) <b>진행 중(IN_PROGRESS)이면서 배정 설비가 가동(RUNNING) 중</b>인
 * 작업지시를 찾아, 매 틱마다 일정량을 자동 생산한다. 생산은 실제 현장 실행과 동일하게
 * {@link PerformanceService#report}·{@link PerformanceService#complete} 를 그대로 호출하므로
 * 자재 투입·Outbox 이벤트까지 정상 발생한다. 지시 수량에 도달하면 자동 완료한다.
 *
 * <p>설정(application.yml):
 * <pre>
 * mes.simulator.enabled            시뮬레이터 on/off (기본 true)
 * mes.simulator.tick-ms            틱 주기(ms, 기본 2000)
 * mes.simulator.rate               틱당 생산량 = 지시수량 × rate (기본 0.12)
 * mes.simulator.defect-probability 틱당 불량 1개 발생 확률 (기본 0.15)
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "mes.simulator.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ProductionSimulator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSimulator.class);

    private final WorkOrderRepository workOrderRepository;
    private final DefectReasonRepository defectReasonRepository;
    private final PerformanceService performanceService;

    @Value("${mes.simulator.rate:0.12}")
    private double rate;

    @Value("${mes.simulator.defect-probability:0.15}")
    private double defectProbability;

    @Scheduled(fixedDelayString = "${mes.simulator.tick-ms:2000}")
    public void tick() {
        List<WorkOrder> targets = workOrderRepository.findRunningInProgress();
        for (WorkOrder wo : targets) {
            try {
                produceOneTick(wo);
            } catch (Exception e) {
                // 한 작업지시의 실패가 다른 작업지시 시뮬레이션을 막지 않도록 격리.
                log.warn("생산 시뮬레이션 틱 실패 (workOrderId={}): {}", wo.getId(), e.getMessage());
            }
        }
    }

    private void produceOneTick(WorkOrder wo) {
        BigDecimal quantity = wo.getQuantity();
        BigDecimal produced = wo.getProducedQty();
        BigDecimal remaining = quantity.subtract(produced);

        // 이미 목표 도달 → 완료 처리하고 종료.
        if (remaining.signum() <= 0) {
            performanceService.complete(wo.getId());
            log.info("생산 시뮬레이션: {} 목표 도달 → 완료", wo.getWorkOrderNo());
            return;
        }

        // 이번 틱 양품 = ceil(지시수량 × rate), 남은 수량으로 상한. 최소 1개.
        BigDecimal perTick = quantity.multiply(BigDecimal.valueOf(rate))
                .setScale(0, RoundingMode.CEILING)
                .max(BigDecimal.ONE);
        BigDecimal good = perTick.min(remaining);

        // 확률적으로 불량 1개 발생(불량사유 랜덤 지정).
        BigDecimal defect = BigDecimal.ZERO;
        Long defectReasonId = null;
        if (ThreadLocalRandom.current().nextDouble() < defectProbability) {
            defect = BigDecimal.ONE;
            defectReasonId = randomDefectReasonId();
        }

        // 실제 현장 실적과 동일 경로로 보고 → 자재 투입·Outbox 이벤트 자동 처리.
        performanceService.report(wo.getId(), new ReportRequest(good, defect, defectReasonId));

        // 이번 틱으로 목표 도달 시 자동 완료.
        if (produced.add(good).compareTo(quantity) >= 0) {
            performanceService.complete(wo.getId());
            log.info("생산 시뮬레이션: {} 생산 완료 (지시 {}개)", wo.getWorkOrderNo(), quantity.stripTrailingZeros().toPlainString());
        }
    }

    private Long randomDefectReasonId() {
        List<DefectReason> reasons = defectReasonRepository.findAll();
        if (reasons.isEmpty()) {
            return null;
        }
        return reasons.get(ThreadLocalRandom.current().nextInt(reasons.size())).getId();
    }
}

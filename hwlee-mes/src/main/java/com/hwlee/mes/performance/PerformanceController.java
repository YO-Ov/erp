package com.hwlee.mes.performance;

import com.hwlee.mes.performance.dto.ProductionResultResponse;
import com.hwlee.mes.performance.dto.ReportRequest;
import com.hwlee.mes.performance.dto.StartRequest;
import com.hwlee.mes.workorder.dto.WorkOrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현장 실행 API — 작업 시작/일시정지/재개/완료 + 생산 실적 보고.
 */
@RestController
@RequestMapping("/api/work-orders/{id}")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService service;

    @PostMapping("/start")
    public WorkOrderResponse start(@PathVariable Long id, @Valid @RequestBody StartRequest req) {
        return service.start(id, req.equipmentId(), req.operatorId());
    }

    @PostMapping("/pause")
    public WorkOrderResponse pause(@PathVariable Long id) {
        return service.pause(id);
    }

    @PostMapping("/resume")
    public WorkOrderResponse resume(@PathVariable Long id) {
        return service.resume(id);
    }

    @PostMapping("/complete")
    public WorkOrderResponse complete(@PathVariable Long id) {
        return service.complete(id);
    }

    @PostMapping("/results")
    public ResponseEntity<ProductionResultResponse> report(@PathVariable Long id,
                                                           @Valid @RequestBody ReportRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.report(id, req));
    }

    @GetMapping("/results")
    public List<ProductionResultResponse> results(@PathVariable Long id) {
        return service.results(id);
    }
}

package com.hwlee.mes.workorder;

import com.hwlee.mes.workorder.dto.WorkOrderReceiveRequest;
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
 * 작업지시 수신 API — ERP 가 호출한다.
 *
 * <p>응답 코드로 멱등 결과를 구분: <b>201</b>=신규 수신, <b>200</b>=이미 등록(중복 무시).
 */
@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService service;

    @PostMapping
    public ResponseEntity<WorkOrderResponse> receive(@Valid @RequestBody WorkOrderReceiveRequest req) {
        WorkOrderService.ReceiveOutcome outcome = service.receive(req);
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(outcome.response());
    }

    @GetMapping
    public List<WorkOrderResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public WorkOrderResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}

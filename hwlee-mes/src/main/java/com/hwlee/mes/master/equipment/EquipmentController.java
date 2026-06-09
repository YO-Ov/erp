package com.hwlee.mes.master.equipment;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 설비 마스터 조회 + 상태 변경 + 가동률. */
@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentRepository repository;
    private final EquipmentStatusService statusService;

    public record EquipmentResponse(Long id, String code, String name, String lineName, EquipmentStatus status) {
        static EquipmentResponse from(Equipment e) {
            return new EquipmentResponse(e.getId(), e.getCode(), e.getName(), e.getLineName(), e.getStatus());
        }
    }

    public record ChangeStatusRequest(@NotNull EquipmentStatus status) {
    }

    @GetMapping
    public List<EquipmentResponse> list() {
        return repository.findAll().stream().map(EquipmentResponse::from).toList();
    }

    @PostMapping("/{id}/status")
    public EquipmentStatus changeStatus(@PathVariable Long id, @RequestBody ChangeStatusRequest req) {
        return statusService.changeStatus(id, req.status());
    }

    @GetMapping("/{id}/utilization")
    public EquipmentStatusService.UtilizationResponse utilization(@PathVariable Long id) {
        return statusService.utilization(id);
    }
}

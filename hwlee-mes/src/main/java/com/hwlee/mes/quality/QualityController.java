package com.hwlee.mes.quality;

import com.hwlee.mes.quality.dto.InspectRequest;
import com.hwlee.mes.quality.dto.QualityInspectionResponse;
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

/** 품질 검사 API + 불량 사유 마스터 조회. */
@RestController
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;
    private final DefectReasonRepository defectReasonRepository;

    public record DefectReasonResponse(Long id, String code, String name) {
    }

    @GetMapping("/api/defect-reasons")
    public List<DefectReasonResponse> defectReasons() {
        return defectReasonRepository.findAll().stream()
                .map(d -> new DefectReasonResponse(d.getId(), d.getCode(), d.getName()))
                .toList();
    }

    @PostMapping("/api/work-orders/{id}/inspections")
    public ResponseEntity<QualityInspectionResponse> inspect(@PathVariable Long id,
                                                             @Valid @RequestBody InspectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(qualityService.inspect(id, req));
    }

    @GetMapping("/api/work-orders/{id}/inspections")
    public List<QualityInspectionResponse> list(@PathVariable Long id) {
        return qualityService.list(id);
    }
}

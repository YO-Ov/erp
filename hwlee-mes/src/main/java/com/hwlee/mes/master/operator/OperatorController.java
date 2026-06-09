package com.hwlee.mes.master.operator;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 작업자 마스터 조회. */
@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorRepository repository;

    public record OperatorResponse(Long id, String code, String name) {
        static OperatorResponse from(Operator o) {
            return new OperatorResponse(o.getId(), o.getCode(), o.getName());
        }
    }

    @GetMapping
    public List<OperatorResponse> list() {
        return repository.findAll().stream().map(OperatorResponse::from).toList();
    }
}

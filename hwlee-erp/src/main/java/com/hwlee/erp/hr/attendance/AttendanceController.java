package com.hwlee.erp.hr.attendance;

import com.hwlee.erp.hr.attendance.dto.AttendanceCreateRequest;
import com.hwlee.erp.hr.attendance.dto.AttendanceResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 근태 API — 등록/조회는 HR/ADMIN 전용.
 */
@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class AttendanceController {

    private final AttendanceService service;

    @PostMapping
    public ResponseEntity<AttendanceResponse> create(@Valid @RequestBody AttendanceCreateRequest req) {
        AttendanceResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/attendances/" + created.id())).body(created);
    }

    @GetMapping
    public List<AttendanceResponse> findByEmployee(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.findByEmployee(employeeId, from, to);
    }
}

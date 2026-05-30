package com.hwlee.erp.fi.journal;

import static com.hwlee.erp.fi.journal.JournalEntrySpecifications.entryDateFrom;
import static com.hwlee.erp.fi.journal.JournalEntrySpecifications.entryDateTo;
import static com.hwlee.erp.fi.journal.JournalEntrySpecifications.sourceIdEquals;
import static com.hwlee.erp.fi.journal.JournalEntrySpecifications.sourceTypeEquals;
import static com.hwlee.erp.fi.journal.JournalEntrySpecifications.statusEquals;
import static org.springframework.data.jpa.domain.Specification.where;

import com.hwlee.erp.fi.journal.dto.JournalEntryCreateRequest;
import com.hwlee.erp.fi.journal.dto.JournalEntryResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journal-entries")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
public class JournalEntryController {

    private final JournalEntryService service;

    @PostMapping
    public ResponseEntity<JournalEntryResponse> createManual(@Valid @RequestBody JournalEntryCreateRequest req) {
        JournalEntryResponse created = service.createManual(req);
        return ResponseEntity.created(URI.create("/api/journal-entries/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public JournalEntryResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/by-number/{number}")
    public JournalEntryResponse findByNumber(@PathVariable String number) {
        return service.findByNumber(number);
    }

    @GetMapping
    public Page<JournalEntryResponse> search(
            @RequestParam(required = false) JournalSource sourceType,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) JournalEntryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return service.search(
                where(sourceTypeEquals(sourceType))
                        .and(sourceIdEquals(sourceId))
                        .and(statusEquals(status))
                        .and(entryDateFrom(dateFrom))
                        .and(entryDateTo(dateTo)),
                pageable
        );
    }

    @PostMapping("/{id}/cancel")
    public JournalEntryResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}

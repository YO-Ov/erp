package com.hwlee.erp.fi.journal;

import com.hwlee.erp.fi.journal.dto.JournalEntryResponse;
import com.hwlee.erp.fi.journal.dto.JournalLineResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JournalEntryMapper {

    JournalEntryResponse toResponse(JournalEntry entity);

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.code", target = "accountCode")
    @Mapping(source = "account.name", target = "accountName")
    JournalLineResponse toResponse(JournalLine line);
}

package com.zenius.one.modules.finance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinJournalLineRepository extends JpaRepository<FinJournalLineEntity, UUID> {
    List<FinJournalLineEntity> findByOrganizationIdAndJournalEntryId(UUID organizationId, UUID journalEntryId);
    List<FinJournalLineEntity> findByOrganizationId(UUID organizationId);
}


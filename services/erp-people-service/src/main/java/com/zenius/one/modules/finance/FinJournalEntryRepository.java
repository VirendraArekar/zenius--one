package com.zenius.one.modules.finance;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinJournalEntryRepository extends JpaRepository<FinJournalEntryEntity, UUID> {
    Page<FinJournalEntryEntity> findByOrganizationIdOrderByEntryDateDescCreatedAtDesc(UUID organizationId, Pageable pageable);
    long countByOrganizationId(UUID organizationId);
}


package com.zenius.one.modules.crm;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmQuoteLineRepository extends JpaRepository<CrmQuoteLineEntity, UUID> {

    List<CrmQuoteLineEntity> findByOrganizationIdAndQuoteIdOrderByCreatedAtAsc(UUID organizationId, UUID quoteId);
}


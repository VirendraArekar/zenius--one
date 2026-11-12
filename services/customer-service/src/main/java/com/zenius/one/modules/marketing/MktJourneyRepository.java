package com.zenius.one.modules.marketing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MktJourneyRepository extends JpaRepository<MktJourneyEntity, UUID> {
    List<MktJourneyEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}


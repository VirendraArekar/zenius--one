package com.zenius.one.modules.marketing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MktCampaignRepository extends JpaRepository<MktCampaignEntity, UUID> {
    Page<MktCampaignEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
    List<MktCampaignEntity> findByOrganizationId(UUID organizationId);
    Optional<MktCampaignEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}


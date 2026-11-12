package com.zenius.one.modules.itsecurity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecItAssetRepository extends JpaRepository<SecItAssetEntity, UUID> {
    List<SecItAssetEntity> findByOrganizationIdOrderByAssetTagAsc(UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}


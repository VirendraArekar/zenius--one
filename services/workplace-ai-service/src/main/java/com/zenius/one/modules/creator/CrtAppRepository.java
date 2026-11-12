package com.zenius.one.modules.creator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrtAppRepository extends JpaRepository<CrtAppEntity, UUID> {
    List<CrtAppEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<CrtAppEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}


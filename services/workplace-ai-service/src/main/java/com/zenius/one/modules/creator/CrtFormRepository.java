package com.zenius.one.modules.creator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrtFormRepository extends JpaRepository<CrtFormEntity, UUID> {
    List<CrtFormEntity> findByOrganizationIdAndAppIdOrderByCreatedAtAsc(UUID organizationId, UUID appId);

    List<CrtFormEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<CrtFormEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}


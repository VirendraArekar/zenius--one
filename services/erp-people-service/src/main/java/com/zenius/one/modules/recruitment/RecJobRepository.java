package com.zenius.one.modules.recruitment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecJobRepository extends JpaRepository<RecJobEntity, UUID> {
    List<RecJobEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<RecJobEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}


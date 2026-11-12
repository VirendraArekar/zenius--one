package com.zenius.one.modules.projects;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjTaskRepository extends JpaRepository<ProjTaskEntity, UUID> {
    List<ProjTaskEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<ProjTaskEntity> findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(UUID organizationId, UUID projectId);
    Optional<ProjTaskEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationIdAndProjectId(UUID organizationId, UUID projectId);
}


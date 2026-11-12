package com.zenius.one.modules.projects;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjSprintRepository extends JpaRepository<ProjSprintEntity, UUID> {
    List<ProjSprintEntity> findByOrganizationIdAndProjectIdOrderByStartDateAsc(UUID organizationId, UUID projectId);
    Optional<ProjSprintEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}


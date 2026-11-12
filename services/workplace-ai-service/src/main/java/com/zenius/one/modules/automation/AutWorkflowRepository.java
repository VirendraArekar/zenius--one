package com.zenius.one.modules.automation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutWorkflowRepository extends JpaRepository<AutWorkflowEntity, UUID> {
    List<AutWorkflowEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<AutWorkflowEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}


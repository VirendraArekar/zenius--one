package com.zenius.one.modules.automation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutExecutionRepository extends JpaRepository<AutExecutionEntity, UUID> {
    List<AutExecutionEntity> findByOrganizationIdOrderByExecutedAtDesc(UUID organizationId);

    List<AutExecutionEntity> findByOrganizationIdAndWorkflowIdOrderByExecutedAtDesc(
            UUID organizationId, UUID workflowId);
}


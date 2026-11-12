package com.zenius.one.modules.crm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmStageRepository extends JpaRepository<CrmStageEntity, UUID> {

    List<CrmStageEntity> findByOrganizationIdAndPipelineIdOrderByStageOrderAsc(UUID organizationId, UUID pipelineId);

    Optional<CrmStageEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}


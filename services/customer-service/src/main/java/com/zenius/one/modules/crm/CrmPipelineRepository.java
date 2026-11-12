package com.zenius.one.modules.crm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmPipelineRepository extends JpaRepository<CrmPipelineEntity, UUID> {

    List<CrmPipelineEntity> findByOrganizationIdOrderByDefaultPipelineDescNameAsc(UUID organizationId);

    Optional<CrmPipelineEntity> findFirstByOrganizationIdAndDefaultPipelineTrue(UUID organizationId);

    Optional<CrmPipelineEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}


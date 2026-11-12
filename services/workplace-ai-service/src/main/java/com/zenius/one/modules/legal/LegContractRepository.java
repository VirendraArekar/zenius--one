package com.zenius.one.modules.legal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegContractRepository extends JpaRepository<LegContractEntity, UUID> {
    List<LegContractEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<LegContractEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}


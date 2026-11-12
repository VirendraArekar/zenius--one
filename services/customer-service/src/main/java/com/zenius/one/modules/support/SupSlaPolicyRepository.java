package com.zenius.one.modules.support;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupSlaPolicyRepository extends JpaRepository<SupSlaPolicyEntity, UUID> {
    List<SupSlaPolicyEntity> findByOrganizationIdOrderByResolutionMinutesAsc(UUID organizationId);
    Optional<SupSlaPolicyEntity> findByOrganizationIdAndPriorityIgnoreCase(UUID organizationId, String priority);
}


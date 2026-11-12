package com.zenius.one.modules.analytics;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiDashboardRepository extends JpaRepository<BiDashboardEntity, UUID> {
    List<BiDashboardEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<BiDashboardEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}


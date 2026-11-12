package com.zenius.one.modules.analytics;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiReportRepository extends JpaRepository<BiReportEntity, UUID> {
    List<BiReportEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<BiReportEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}


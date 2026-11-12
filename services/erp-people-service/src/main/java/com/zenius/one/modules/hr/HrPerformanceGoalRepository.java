package com.zenius.one.modules.hr;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrPerformanceGoalRepository extends JpaRepository<HrPerformanceGoalEntity, UUID> {
    List<HrPerformanceGoalEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}


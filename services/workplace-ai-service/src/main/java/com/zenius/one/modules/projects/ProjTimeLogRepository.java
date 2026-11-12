package com.zenius.one.modules.projects;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjTimeLogRepository extends JpaRepository<ProjTimeLogEntity, UUID> {
    List<ProjTimeLogEntity> findByOrganizationIdOrderByWorkDateDescCreatedAtDesc(UUID organizationId);
}


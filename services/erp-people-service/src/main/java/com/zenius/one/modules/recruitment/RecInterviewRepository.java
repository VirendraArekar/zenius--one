package com.zenius.one.modules.recruitment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecInterviewRepository extends JpaRepository<RecInterviewEntity, UUID> {
    List<RecInterviewEntity> findByOrganizationIdOrderByScheduledAtAsc(UUID organizationId);
}


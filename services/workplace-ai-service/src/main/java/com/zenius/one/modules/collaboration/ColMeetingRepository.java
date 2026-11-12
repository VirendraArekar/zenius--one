package com.zenius.one.modules.collaboration;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColMeetingRepository extends JpaRepository<ColMeetingEntity, UUID> {
    List<ColMeetingEntity> findByOrganizationIdOrderByScheduledAtAsc(UUID organizationId);
}


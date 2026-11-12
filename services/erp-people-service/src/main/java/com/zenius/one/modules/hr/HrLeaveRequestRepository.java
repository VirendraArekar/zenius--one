package com.zenius.one.modules.hr;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrLeaveRequestRepository extends JpaRepository<HrLeaveRequestEntity, UUID> {
    List<HrLeaveRequestEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<HrLeaveRequestEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}


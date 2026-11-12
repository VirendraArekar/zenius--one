package com.zenius.one.modules.support;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupServiceAppointmentRepository extends JpaRepository<SupServiceAppointmentEntity, UUID> {
    List<SupServiceAppointmentEntity> findByOrganizationIdOrderByScheduledStartAsc(UUID organizationId);
    Optional<SupServiceAppointmentEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}


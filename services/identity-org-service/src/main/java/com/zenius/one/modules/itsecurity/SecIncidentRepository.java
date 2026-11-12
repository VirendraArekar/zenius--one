package com.zenius.one.modules.itsecurity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecIncidentRepository extends JpaRepository<SecIncidentEntity, UUID> {
    List<SecIncidentEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<SecIncidentEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}


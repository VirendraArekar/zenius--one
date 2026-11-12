package com.zenius.one.modules.marketing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MktFormRepository extends JpaRepository<MktFormEntity, UUID> {
    List<MktFormEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<MktFormEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}


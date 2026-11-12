package com.zenius.one.modules.marketing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MktSegmentRepository extends JpaRepository<MktSegmentEntity, UUID> {
    List<MktSegmentEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<MktSegmentEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}


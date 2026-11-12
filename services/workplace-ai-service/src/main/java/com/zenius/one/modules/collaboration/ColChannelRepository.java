package com.zenius.one.modules.collaboration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColChannelRepository extends JpaRepository<ColChannelEntity, UUID> {
    List<ColChannelEntity> findByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<ColChannelEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}


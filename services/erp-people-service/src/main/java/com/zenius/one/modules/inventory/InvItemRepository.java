package com.zenius.one.modules.inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvItemRepository extends JpaRepository<InvItemEntity, UUID> {
    List<InvItemEntity> findByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<InvItemEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndSkuIgnoreCase(UUID organizationId, String sku);
}


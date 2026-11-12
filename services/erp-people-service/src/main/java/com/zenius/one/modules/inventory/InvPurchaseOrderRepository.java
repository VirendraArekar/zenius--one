package com.zenius.one.modules.inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvPurchaseOrderRepository extends JpaRepository<InvPurchaseOrderEntity, UUID> {
    List<InvPurchaseOrderEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<InvPurchaseOrderEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}


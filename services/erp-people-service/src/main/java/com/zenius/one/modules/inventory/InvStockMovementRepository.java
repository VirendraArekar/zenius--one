package com.zenius.one.modules.inventory;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvStockMovementRepository extends JpaRepository<InvStockMovementEntity, UUID> {
    List<InvStockMovementEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}


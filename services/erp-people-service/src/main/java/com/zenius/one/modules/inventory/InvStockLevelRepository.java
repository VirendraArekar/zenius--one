package com.zenius.one.modules.inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvStockLevelRepository extends JpaRepository<InvStockLevelEntity, UUID> {
    List<InvStockLevelEntity> findByOrganizationId(UUID organizationId);
    Optional<InvStockLevelEntity> findByOrganizationIdAndItemIdAndWarehouseId(UUID organizationId, UUID itemId, UUID warehouseId);
}


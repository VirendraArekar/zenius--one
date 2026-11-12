package com.zenius.one.modules.inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvWarehouseRepository extends JpaRepository<InvWarehouseEntity, UUID> {
    List<InvWarehouseEntity> findByOrganizationIdOrderByDefaultWarehouseDescNameAsc(UUID organizationId);
    Optional<InvWarehouseEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}


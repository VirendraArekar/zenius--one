package com.zenius.one.modules.inventory;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvStoreOrderRepository extends JpaRepository<InvStoreOrderEntity, UUID> {
    List<InvStoreOrderEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}


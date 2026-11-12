package com.zenius.one.modules.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillSubscriptionRepository extends JpaRepository<BillSubscriptionEntity, UUID> {
    List<BillSubscriptionEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<BillSubscriptionEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}


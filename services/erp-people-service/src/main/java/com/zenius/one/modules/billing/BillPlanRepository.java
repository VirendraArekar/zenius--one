package com.zenius.one.modules.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillPlanRepository extends JpaRepository<BillPlanEntity, UUID> {
    List<BillPlanEntity> findByOrganizationIdOrderByPriceAsc(UUID organizationId);
    Optional<BillPlanEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}


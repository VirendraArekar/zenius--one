package com.zenius.one.modules.marketing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MktCouponRepository extends JpaRepository<MktCouponEntity, UUID> {
    List<MktCouponEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<MktCouponEntity> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}


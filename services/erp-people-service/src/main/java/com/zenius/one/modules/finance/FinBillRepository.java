package com.zenius.one.modules.finance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinBillRepository extends JpaRepository<FinBillEntity, UUID> {
    Page<FinBillEntity> findByOrganizationIdOrderByBillDateDescCreatedAtDesc(UUID organizationId, Pageable pageable);
    List<FinBillEntity> findByOrganizationId(UUID organizationId);
    Optional<FinBillEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}


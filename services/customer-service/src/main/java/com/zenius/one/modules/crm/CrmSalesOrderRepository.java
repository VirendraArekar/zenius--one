package com.zenius.one.modules.crm;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmSalesOrderRepository extends JpaRepository<CrmSalesOrderEntity, UUID> {

    Page<CrmSalesOrderEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Optional<CrmSalesOrderEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}


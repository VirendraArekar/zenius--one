package com.zenius.one.modules.finance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinInvoiceRepository extends JpaRepository<FinInvoiceEntity, UUID> {
    Page<FinInvoiceEntity> findByOrganizationIdOrderByIssueDateDescCreatedAtDesc(UUID organizationId, Pageable pageable);
    List<FinInvoiceEntity> findByOrganizationId(UUID organizationId);
    Optional<FinInvoiceEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}


package com.zenius.one.modules.creator;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrtRecordRepository extends JpaRepository<CrtRecordEntity, UUID> {
    List<CrtRecordEntity> findByOrganizationIdAndFormIdOrderByCreatedAtDesc(UUID organizationId, UUID formId);

    long countByOrganizationIdAndFormId(UUID organizationId, UUID formId);
}


package com.zenius.one.modules.finance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinPaymentRepository extends JpaRepository<FinPaymentEntity, UUID> {
    List<FinPaymentEntity> findByOrganizationIdOrderByPaymentDateDescCreatedAtDesc(UUID organizationId);
}


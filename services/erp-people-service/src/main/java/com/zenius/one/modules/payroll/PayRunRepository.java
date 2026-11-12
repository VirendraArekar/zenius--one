package com.zenius.one.modules.payroll;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayRunRepository extends JpaRepository<PayRunEntity, UUID> {
    List<PayRunEntity> findByOrganizationIdOrderByPayMonthDesc(UUID organizationId);
    boolean existsByOrganizationIdAndPayMonth(UUID organizationId, String payMonth);
}


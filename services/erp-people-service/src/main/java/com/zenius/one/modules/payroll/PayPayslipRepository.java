package com.zenius.one.modules.payroll;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayPayslipRepository extends JpaRepository<PayPayslipEntity, UUID> {
    List<PayPayslipEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}


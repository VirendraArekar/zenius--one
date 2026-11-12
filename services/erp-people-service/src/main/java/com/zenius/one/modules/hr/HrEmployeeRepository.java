package com.zenius.one.modules.hr;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrEmployeeRepository extends JpaRepository<HrEmployeeEntity, UUID> {
    List<HrEmployeeEntity> findByOrganizationIdOrderByEmployeeCodeAsc(UUID organizationId);
    List<HrEmployeeEntity> findByOrganizationIdAndStatus(UUID organizationId, String status);
    Optional<HrEmployeeEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndWorkEmailIgnoreCase(UUID organizationId, String workEmail);
    long countByOrganizationId(UUID organizationId);
}


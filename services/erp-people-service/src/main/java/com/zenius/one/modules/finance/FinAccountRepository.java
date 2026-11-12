package com.zenius.one.modules.finance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinAccountRepository extends JpaRepository<FinAccountEntity, UUID> {
    List<FinAccountEntity> findByOrganizationIdOrderByCodeAsc(UUID organizationId);
    Optional<FinAccountEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<FinAccountEntity> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}


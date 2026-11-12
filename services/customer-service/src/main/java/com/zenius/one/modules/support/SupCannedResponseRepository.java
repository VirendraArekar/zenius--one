package com.zenius.one.modules.support;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupCannedResponseRepository extends JpaRepository<SupCannedResponseEntity, UUID> {
    List<SupCannedResponseEntity> findByOrganizationIdOrderByTitleAsc(UUID organizationId);
}


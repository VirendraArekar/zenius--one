package com.zenius.one.modules.itsecurity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecPolicyRepository extends JpaRepository<SecPolicyEntity, UUID> {
    Optional<SecPolicyEntity> findByOrganizationId(UUID organizationId);
}


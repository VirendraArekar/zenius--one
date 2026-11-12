package com.zenius.one.modules.integration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    List<ApiKeyEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<ApiKeyEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ApiKeyEntity> findByKeyHash(String keyHash);
}


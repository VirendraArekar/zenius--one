package com.zenius.one.modules.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    List<RoleEntity> findByOrganizationIdOrderBySystemRoleDescNameAsc(UUID organizationId);

    Optional<RoleEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<RoleEntity> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    List<RoleEntity> findByOrganizationIdAndIdIn(UUID organizationId, Collection<UUID> ids);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}


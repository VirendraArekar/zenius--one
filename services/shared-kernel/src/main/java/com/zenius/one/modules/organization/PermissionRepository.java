package com.zenius.one.modules.organization;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    List<PermissionEntity> findAllByOrderByModuleCodeAscPermissionKeyAsc();

    List<PermissionEntity> findByPermissionKeyIn(Collection<String> keys);
}


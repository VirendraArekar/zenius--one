package com.zenius.one.modules.crm;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmActivityRepository extends JpaRepository<CrmActivityEntity, UUID> {

    java.util.Optional<CrmActivityEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
            SELECT a FROM CrmActivityEntity a
            WHERE a.organizationId = :orgId
              AND (:entityType IS NULL OR UPPER(a.relatedEntityType) = UPPER(:entityType))
              AND (:entityId IS NULL OR a.relatedEntityId = :entityId)
            ORDER BY a.createdAt DESC
            """)
    Page<CrmActivityEntity> search(
            @Param("orgId") UUID organizationId,
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            Pageable pageable
    );
}


package com.zenius.one.modules.itsecurity;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    @Query("""
            SELECT a FROM AuditLogEntity a
            WHERE a.organizationId = :orgId
              AND (:action IS NULL OR UPPER(a.action) = UPPER(:action))
              AND (:query IS NULL OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(a.actorEmail, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(a.resourceType) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<AuditLogEntity> searchByOrganization(
            @Param("orgId") UUID organizationId,
            @Param("action") String action,
            @Param("query") String query,
            Pageable pageable
    );
}


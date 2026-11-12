package com.zenius.one.modules.crm;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmAccountRepository extends JpaRepository<CrmAccountEntity, UUID> {

    Optional<CrmAccountEntity> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    Optional<CrmAccountEntity> findFirstByOrganizationIdAndNameIgnoreCaseAndDeletedAtIsNull(
            UUID organizationId,
            String name
    );

    long countByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    @Query("""
            SELECT a FROM CrmAccountEntity a
            WHERE a.organizationId = :orgId
              AND a.deletedAt IS NULL
              AND (:stage IS NULL OR UPPER(a.lifecycleStage) = UPPER(:stage))
              AND (:query IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(a.industry, '')) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<CrmAccountEntity> search(
            @Param("orgId") UUID organizationId,
            @Param("stage") String stage,
            @Param("query") String query,
            Pageable pageable
    );
}


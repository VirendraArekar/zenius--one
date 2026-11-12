package com.zenius.one.modules.crm;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmContactRepository extends JpaRepository<CrmContactEntity, UUID> {

    Optional<CrmContactEntity> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    long countByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    @Query("""
            SELECT c FROM CrmContactEntity c
            WHERE c.organizationId = :orgId
              AND c.deletedAt IS NULL
              AND (:accountId IS NULL OR c.accountId = :accountId)
              AND (:query IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<CrmContactEntity> search(
            @Param("orgId") UUID organizationId,
            @Param("accountId") UUID accountId,
            @Param("query") String query,
            Pageable pageable
    );
}


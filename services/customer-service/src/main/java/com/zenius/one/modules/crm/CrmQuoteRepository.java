package com.zenius.one.modules.crm;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmQuoteRepository extends JpaRepository<CrmQuoteEntity, UUID> {

    Optional<CrmQuoteEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationId(UUID organizationId);

    @Query("""
            SELECT q FROM CrmQuoteEntity q
            WHERE q.organizationId = :orgId
              AND (:status IS NULL OR UPPER(q.status) = UPPER(:status))
              AND (:query IS NULL OR LOWER(q.subject) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(q.quoteNumber) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<CrmQuoteEntity> search(
            @Param("orgId") UUID organizationId,
            @Param("status") String status,
            @Param("query") String query,
            Pageable pageable
    );
}


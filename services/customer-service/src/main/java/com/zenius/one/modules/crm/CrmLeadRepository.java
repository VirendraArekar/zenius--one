package com.zenius.one.modules.crm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmLeadRepository extends JpaRepository<CrmLeadEntity, UUID> {

    Optional<CrmLeadEntity> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndEmailIgnoreCaseAndDeletedAtIsNull(UUID organizationId, String email);

    long countByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    long countByOrganizationIdAndStatusAndDeletedAtIsNull(UUID organizationId, String status);

    List<CrmLeadEntity> findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

    @Query("""
            SELECT l FROM CrmLeadEntity l
            WHERE l.organizationId = :orgId
              AND l.deletedAt IS NULL
              AND (:status IS NULL OR UPPER(l.status) = UPPER(:status))
              AND (:source IS NULL OR UPPER(l.source) = UPPER(:source))
              AND (:query IS NULL OR LOWER(l.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(l.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(l.companyName, '')) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<CrmLeadEntity> search(
            @Param("orgId") UUID organizationId,
            @Param("status") String status,
            @Param("source") String source,
            @Param("query") String query,
            Pageable pageable
    );
}


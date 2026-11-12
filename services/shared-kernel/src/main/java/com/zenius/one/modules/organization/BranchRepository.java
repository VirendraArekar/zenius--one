package com.zenius.one.modules.organization;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {

    Optional<BranchEntity> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID organizationId, String code);

    @Query("""
            SELECT b FROM BranchEntity b
            WHERE b.organizationId = :orgId
              AND b.deletedAt IS NULL
              AND (:query IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(b.code) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(b.city, '')) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<BranchEntity> searchByOrganization(
            @Param("orgId") UUID organizationId,
            @Param("query") String query,
            Pageable pageable
    );
}


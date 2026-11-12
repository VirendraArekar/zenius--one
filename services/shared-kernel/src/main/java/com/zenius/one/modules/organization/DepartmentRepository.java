package com.zenius.one.modules.organization;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    Optional<DepartmentEntity> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID organizationId, String code);

    @Query("""
            SELECT d FROM DepartmentEntity d
            WHERE d.organizationId = :orgId
              AND d.deletedAt IS NULL
              AND (:query IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(d.code) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<DepartmentEntity> searchByOrganization(
            @Param("orgId") UUID organizationId,
            @Param("query") String query,
            Pageable pageable
    );
}


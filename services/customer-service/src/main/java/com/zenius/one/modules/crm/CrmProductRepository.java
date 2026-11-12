package com.zenius.one.modules.crm;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmProductRepository extends JpaRepository<CrmProductEntity, UUID> {

    Optional<CrmProductEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndSkuIgnoreCase(UUID organizationId, String sku);

    @Query("""
            SELECT p FROM CrmProductEntity p
            WHERE p.organizationId = :orgId
              AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<CrmProductEntity> search(
            @Param("orgId") UUID organizationId,
            @Param("query") String query,
            Pageable pageable
    );
}


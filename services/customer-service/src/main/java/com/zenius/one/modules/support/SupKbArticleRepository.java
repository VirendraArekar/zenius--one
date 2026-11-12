package com.zenius.one.modules.support;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupKbArticleRepository extends JpaRepository<SupKbArticleEntity, UUID> {

    Optional<SupKbArticleEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationIdAndPublishedTrue(UUID organizationId);

    @Query("""
            SELECT a FROM SupKbArticleEntity a
            WHERE a.organizationId = :orgId
              AND (:category IS NULL OR UPPER(a.category) = UPPER(:category))
              AND (:query IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<SupKbArticleEntity> search(
            @Param("orgId") UUID organizationId,
            @Param("category") String category,
            @Param("query") String query,
            Pageable pageable
    );
}


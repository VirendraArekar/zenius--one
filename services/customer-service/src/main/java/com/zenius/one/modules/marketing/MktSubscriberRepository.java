package com.zenius.one.modules.marketing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MktSubscriberRepository extends JpaRepository<MktSubscriberEntity, UUID> {

    Optional<MktSubscriberEntity> findByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);

    long countByOrganizationIdAndStatus(UUID organizationId, String status);

    List<MktSubscriberEntity> findByOrganizationIdAndStatus(UUID organizationId, String status);

    @Query("""
            SELECT s FROM MktSubscriberEntity s
            WHERE s.organizationId = :orgId
              AND (:status IS NULL OR UPPER(s.status) = UPPER(:status))
              AND (:query IS NULL OR LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(s.fullName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(s.tags, '')) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<MktSubscriberEntity> search(
            @Param("orgId") UUID organizationId,
            @Param("status") String status,
            @Param("query") String query,
            Pageable pageable
    );
}


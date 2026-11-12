package com.zenius.one.modules.crm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmDealRepository extends JpaRepository<CrmDealEntity, UUID> {

    Optional<CrmDealEntity> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    List<CrmDealEntity> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    @Query("""
            SELECT d FROM CrmDealEntity d
            WHERE d.organizationId = :orgId
              AND d.deletedAt IS NULL
              AND (:pipelineId IS NULL OR d.pipelineId = :pipelineId)
              AND (:stageId IS NULL OR d.stageId = :stageId)
              AND (:query IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<CrmDealEntity> search(
            @Param("orgId") UUID organizationId,
            @Param("pipelineId") UUID pipelineId,
            @Param("stageId") UUID stageId,
            @Param("query") String query,
            Pageable pageable
    );
}


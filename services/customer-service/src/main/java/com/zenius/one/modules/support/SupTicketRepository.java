package com.zenius.one.modules.support;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupTicketRepository extends JpaRepository<SupTicketEntity, UUID> {

    Optional<SupTicketEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationId(UUID organizationId);

    List<SupTicketEntity> findByOrganizationId(UUID organizationId);

    @Query("""
            SELECT t FROM SupTicketEntity t
            WHERE t.organizationId = :orgId
              AND (:status IS NULL OR UPPER(t.status) = UPPER(:status))
              AND (:priority IS NULL OR UPPER(t.priority) = UPPER(:priority))
              AND (:query IS NULL OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(t.requesterEmail) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<SupTicketEntity> search(
            @Param("orgId") UUID organizationId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("query") String query,
            Pageable pageable
    );
}


package com.zenius.one.modules.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrgMembershipRepository extends JpaRepository<OrgMembershipEntity, UUID> {

    Optional<OrgMembershipEntity> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    Optional<OrgMembershipEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<OrgMembershipEntity> findByUserIdAndStatusOrderByJoinedAtAsc(UUID userId, String status);

    @Query("""
            SELECT m FROM OrgMembershipEntity m
            JOIN UserEntity u ON u.id = m.userId
            WHERE m.organizationId = :orgId
              AND (:query IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(m.employeeCode, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(m.designation, '')) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<OrgMembershipEntity> searchMembers(
            @Param("orgId") UUID organizationId,
            @Param("query") String query,
            Pageable pageable
    );
}


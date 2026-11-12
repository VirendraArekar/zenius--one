package com.zenius.one.modules.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    Optional<OrganizationEntity> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    @Query("""
            SELECT o FROM OrganizationEntity o
            JOIN OrgMembershipEntity m ON m.organizationId = o.id
            WHERE m.userId = :userId AND m.status = 'ACTIVE' AND o.status = 'ACTIVE'
            ORDER BY o.name ASC
            """)
    List<OrganizationEntity> findActiveOrganizationsForUser(@Param("userId") UUID userId);
}


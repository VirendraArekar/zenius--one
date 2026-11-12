package com.zenius.one.platform.system;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformModuleRepository extends JpaRepository<PlatformModuleEntity, UUID> {

    Optional<PlatformModuleEntity> findByCode(String code);

    long countByStatus(String status);

    @Query("""
            SELECT m FROM PlatformModuleEntity m
            WHERE (:category IS NULL OR UPPER(m.category) = UPPER(:category))
              AND (:phase IS NULL OR m.phaseNumber = :phase)
              AND (:query IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(m.tagline) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(m.code) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<PlatformModuleEntity> searchModules(
            @Param("category") String category,
            @Param("phase") Integer phase,
            @Param("query") String query,
            Pageable pageable
    );
}


package com.zenius.one.modules.recruitment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecCandidateRepository extends JpaRepository<RecCandidateEntity, UUID> {
    List<RecCandidateEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<RecCandidateEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

